/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers

import config.AppConfig
import connectors.SubmissionConnector
import controllers.auth.{IdentifierAuthAction, IdentifierRequest}
import models.error.ReadSubscriptionError
import models.submission.{ConversationId, FileDetails, Pending, SubmissionMetaData}
import models.subscription.ResponseDetail
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Request}
import play.api.{Logger, Logging}
import repositories.submission.FileDetailsRepository
import services.validation.XMLValidationService
import services.{AgentDetails, AgentSubscriptionService, SubscriptionService, TransformService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpReads.is2xx
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Right
import scala.xml.NodeSeq

class SubmissionController @Inject() (
  authenticate: IdentifierAuthAction,
  cc: ControllerComponents,
  transformService: TransformService,
  readSubscriptionService: SubscriptionService,
  agentReadSubscriptionService: AgentSubscriptionService,
  submissionConnector: SubmissionConnector,
  fileDetailsRepository: FileDetailsRepository,
  xmlValidationService: XMLValidationService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitDisclosure: Action[NodeSeq] = authenticate.async(parse.xml) { implicit request =>
    val xml                      = request.body
    val fileName                 = (xml \ "fileName").text
    val messageRefId             = (xml \\ "MessageRefId").text
    val reportingEntityName      = (xml \\ "ReportingEntity" \\ "Entity" \\ "Name").head.text
    val subscriptionId           = (xml \ "enrolmentID").text
    val submissionTime           = dateTimeNow()
    val conversationId           = ConversationId()
    val uploadedXmlNode: NodeSeq = xml \ "file" \ "CBC_OECD"
    val submissionDetails = FileDetails(conversationId, subscriptionId, messageRefId, reportingEntityName, Pending, fileName, submissionTime, submissionTime)

    val submissionMetaData = SubmissionMetaData.build(submissionTime, conversationId, fileName)

    val result = for {
      org        <- readSubscriptionService.getContactInformation(subscriptionId)
      agentMayBe <- getAgentDetails
    } yield (org, agentMayBe) match {
      case (Right(orgDetails), Right(agentDetailsMayBe)) =>
        addSubscriptionDetails(conversationId, uploadedXmlNode, submissionMetaData, orgDetails, submissionDetails, agentDetailsMayBe)
      case (errorOrg, errorAgent) =>
        logger.warn(s"ReadSubscriptionError Organisation $errorOrg")
        logger.warn(s"ReadSubscriptionError Organisation $errorAgent")
        Future.successful(InternalServerError)
    }
    result.flatten
  }

  private def addSubscriptionDetails(conversationId: ConversationId,
                                     uploadedXmlNode: NodeSeq,
                                     submissionMetaData: SubmissionMetaData,
                                     value: ResponseDetail,
                                     submissionDetails: FileDetails,
                                     agentDetails: Option[AgentDetails]
  )(implicit request: Request[NodeSeq]) = {
    val submissionXml: NodeSeq = transformService.addSubscriptionDetailsToSubmission(uploadedXmlNode, value, submissionMetaData, agentDetails)
    val sanitisedXml           = scala.xml.Utility.trim(scala.xml.XML.loadString(submissionXml.mkString)) //trim only behaves correctly with xml.Elem
    val validatedResponse      = xmlValidationService.validate(xml = sanitisedXml, filePath = appConfig.submissionXSDFilePath)

    validatedResponse match {
      case Left(value) =>
        logger.warn(s"Xml Validation Error $value")
        Future.successful(InternalServerError)
      case Right(_) =>
        submissionConnector.submitDisclosure(submissionXml, conversationId).flatMap { httpResponse =>
          httpResponse.status match {
            case status if is2xx(status) => fileDetailsRepository.insert(submissionDetails).map(_ => Ok(Json.toJson(conversationId)))
            case _                       => Future.successful(httpResponse.handleResponse(implicitly[Logger](logger)))
          }
        }
    }
  }

  private def getAgentDetails(implicit request: IdentifierRequest[NodeSeq]): Future[Either[ReadSubscriptionError, Option[AgentDetails]]] =
    request.arn
      .map(agentRefNo =>
        agentReadSubscriptionService.getContactInformation(agentRefNo).map {
          case Right(value) => Right(Some(AgentDetails(agentRefNo, value)))

          case Left(ReadSubscriptionError(value)) =>
            logger.warn(s"ReadSubscriptionError $value")
            Left(ReadSubscriptionError(value))
        }
      )
      .getOrElse(Future.successful(Right(None)))

}
