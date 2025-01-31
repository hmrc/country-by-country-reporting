/*
 * Copyright 2024 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.submission

import cats.data.EitherT
import config.AppConfig
import connectors.SubmissionConnector
import controllers.auth.IdentifierRequest
import controllers.dateTimeNow
import models.agentSubscription.AgentContactDetails
import models.audit.AuditType
import models.error.{BackendError, ReadSubscriptionError, RepositoryError, SubmissionServiceError}
import models.submission._
import models.subscription.ResponseDetail
import models.xml.XmlHandler
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import repositories.submission.FileDetailsRepository
import services.audit.AuditService
import services.validation.XMLValidationService
import services.{AgentSubscriptionService, DataExtraction, SubscriptionService, TransformService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpErrorFunctions.is2xx

import java.time.{Clock, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml.{Elem, NodeSeq}

class SubmissionService @Inject() (
  appConfig: AppConfig,
  dataExtraction: DataExtraction,
  transformService: TransformService,
  xmlHandler: XmlHandler,
  xmlValidationService: XMLValidationService,
  agentSubscriptionService: AgentSubscriptionService,
  subscriptionService: SubscriptionService,
  sdesService: SDESService,
  submissionConnector: SubmissionConnector,
  fileDetailsRepository: FileDetailsRepository,
  auditService: AuditService
)(implicit ec: ExecutionContext, clock: Clock)
    extends Logging {

  def submitLargeFile(
    submissionDetails: SubmissionDetails
  )(implicit request: IdentifierRequest[JsValue], hc: HeaderCarrier): Future[Either[BackendError, ConversationId]] = {
    val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)
    logger.info(s"Submitting large file with conversation Id: [${conversationId.value}]")
    (for {
      orgContactDetails        <- EitherT(subscriptionService.getContactInformation(submissionDetails.enrolmentId))
      maybeAgentContactDetails <- EitherT(getAgentContactDetails())
      _                        <- EitherT(sdesService.sendFileNotification(submissionDetails, orgContactDetails, conversationId))
      reportType = submissionDetails.messageSpecData.reportType
      fileDetails = createFilePendingDetails(conversationId,
                                             submissionDetails,
                                             dateTimeNow,
                                             reportType,
                                             maybeAgentContactDetails,
                                             request.affinityGroup,
                                             Some(LargeFile)
      )
      _ = auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(fileDetails))
      _ <- EitherT(persistFileDetails(fileDetails))
    } yield conversationId).value
  }

  def submitNormalFile(
    submissionDetails: SubmissionDetails
  )(implicit request: IdentifierRequest[JsValue], hc: HeaderCarrier): Future[Either[BackendError, ConversationId]] = {
    val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)
    logger.info(s"Submitting normal sized file with conversation Id: [${conversationId.value}]")
    val submissionTime = dateTimeNow
    val documentUrl    = submissionDetails.documentUrl
    val result = xmlHandler.load(documentUrl) match {
      case Success(xml) =>
        xml.headOption match {
          case Some(rootNode) =>
            val xmlElement = Elem.apply(rootNode.prefix, rootNode.label, rootNode.attributes, rootNode.scope, true, rootNode.child: _*)
            val reportType = dataExtraction.getReportType(submissionDetails.messageSpecData.messageTypeIndic, xmlElement)
            for {
              orgContactDetails        <- EitherT(subscriptionService.getContactInformation(submissionDetails.enrolmentId))
              maybeAgentContactDetails <- EitherT(getAgentContactDetails())
              submissionMetaData = SubmissionMetaData.build(submissionTime, conversationId, submissionDetails.fileName)
              fileDetails = createFilePendingDetails(conversationId,
                                                     submissionDetails,
                                                     submissionTime,
                                                     reportType,
                                                     maybeAgentContactDetails,
                                                     request.affinityGroup,
                                                     Some(NormalFile)
              )
              _ = auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(fileDetails))
              _ <- EitherT(addSubscriptionDetailsToXml(xml, submissionMetaData, orgContactDetails, fileDetails))
              _ <- EitherT(persistFileDetails(fileDetails))
            } yield conversationId
          case None =>
            val error = SubmissionServiceError(s"Xml file with conversation Id [${conversationId.value}] is empty", Some(request.affinityGroup))
            auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(error))
            EitherT.left(Future.successful(error))
        }
      case Failure(_) =>
        val error =
          SubmissionServiceError(s"Failed to load xml file [$documentUrl] with conversation Id [${conversationId.value}]", Some(request.affinityGroup))
        auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(error))
        EitherT.left(Future.successful(error))
    }

    result.value
  }

  private def addSubscriptionDetailsToXml(
    uploadedXml: NodeSeq,
    submissionMetaData: SubmissionMetaData,
    organisationDetails: ResponseDetail,
    fileDetails: FileDetails
  )(implicit hc: HeaderCarrier): Future[Either[BackendError, ConversationId]] = {
    val conversationId = fileDetails._id
    val submissionXml = transformService
      .addSubscriptionDetailsToSubmission(uploadedXml, organisationDetails, submissionMetaData, fileDetails.agentDetails)
    val validatedResponse = xmlValidationService.validate(xml = submissionXml, filePath = appConfig.submissionXSDFilePath)

    validatedResponse match {
      case Left(parseErrors) =>
        Future.successful(
          Left(SubmissionServiceError(s"Xml parse error file with conversation Id [${conversationId.value}]: ${parseErrors.mkString(", ")}"))
        )
      case Right(_) =>
        submissionConnector.submitDisclosure(submissionXml, submissionMetaData.conversationId).flatMap { httpResponse =>
          val statusCode = httpResponse.status
          if (is2xx(statusCode)) {
            Future.successful(Right(submissionMetaData.conversationId))
          } else {
            Future.successful(Left(SubmissionServiceError(s"Failed to submit file with conversation Id [${conversationId.value}]. Got status: $statusCode")))
          }
        }
    }
  }

  private def getAgentContactDetails()(implicit
    request: IdentifierRequest[JsValue],
    hc: HeaderCarrier
  ): Future[Either[ReadSubscriptionError, Option[AgentContactDetails]]] =
    request.arn
      .map(agentRefNo =>
        agentSubscriptionService.getContactInformation(agentRefNo).map {
          case Right(value) =>
            Right(Option(AgentContactDetails(agentRefNo, value)))
          case Left(error) =>
            logger.warn(error.detail)
            Left(error)
        }
      )
      .getOrElse(Future.successful(Right(None)))

  private def createFilePendingDetails(
    conversationId: ConversationId,
    submissionDetails: SubmissionDetails,
    submissionTime: LocalDateTime,
    reportType: ReportType,
    maybeAgentDetails: Option[AgentContactDetails],
    affinityGroup: AffinityGroup,
    fileType: Option[FileType]
  ) =
    FileDetails(
      conversationId,
      submissionDetails.enrolmentId,
      submissionDetails.messageSpecData.messageRefId,
      submissionDetails.messageSpecData.reportingEntityName,
      reportType,
      Pending,
      submissionDetails.fileName,
      submissionTime,
      submissionTime,
      maybeAgentDetails,
      Option(affinityGroup),
      fileType
    )

  private def persistFileDetails(fileDetails: FileDetails): Future[Either[BackendError, Boolean]] =
    fileDetailsRepository
      .insert(fileDetails)
      .map(Right(_))
      .recover(_ => Left(RepositoryError(s"Failed to persist details for file with conversation Id [${fileDetails._id.value}]")))

}
