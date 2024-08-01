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

import base.SpecBase
import config.AppConfig
import connectors.SubmissionConnector
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction, IdentifierRequest}
import controllers.dateTimeNow
import generators.Generators
import models.agentSubscription.{AgentContactDetails, AgentResponseDetail}
import models.error.{ReadSubscriptionError, RepositoryError, SdesSubmissionError, SubmissionServiceError}
import models.submission._
import models.subscription.ResponseDetail
import models.xml.XmlHandler
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.http.Status.OK
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.mvc.Request
import repositories.submission.FileDetailsRepository
import services.validation.XMLValidationService
import services.{AgentSubscriptionService, DataExtraction, SubscriptionService, TransformService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import java.io.IOException
import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.xml.{Elem, NodeSeq, TopScope}

class SubmissionServiceSpec extends SpecBase with IntegrationPatience with Generators with ScalaCheckDrivenPropertyChecks {

  private val mockAppConfig                = mock[AppConfig]
  private val mockDataExtraction           = mock[DataExtraction]
  private val mockTransformService         = mock[TransformService]
  private val mockXmlHandler               = mock[XmlHandler]
  private val mockXmlValidationService     = mock[XMLValidationService]
  private val mockAgentSubscriptionService = mock[AgentSubscriptionService]
  private val mockSubscriptionService      = mock[SubscriptionService]
  private val mockSdesService              = mock[SDESService]
  private val mockSubmissionConnector      = mock[SubmissionConnector]
  private val mockFileDetailsRepository    = mock[FileDetailsRepository]
  private val mockRequest                  = mock[Request[JsValue]]

  private val fileDetailsArgCaptor: ArgumentCaptor[FileDetails] = ArgumentCaptor.forClass(classOf[FileDetails])

  override def beforeEach(): Unit =
    reset(
      mockAppConfig,
      mockDataExtraction,
      mockTransformService,
      mockXmlHandler,
      mockXmlValidationService,
      mockAgentSubscriptionService,
      mockSubscriptionService,
      mockSdesService,
      mockSubmissionConnector,
      mockFileDetailsRepository,
      mockRequest
    )

  override lazy val app: Application = applicationBuilder()
    .overrides(
      bind[AppConfig].toInstance(mockAppConfig),
      bind[DataExtraction].toInstance(mockDataExtraction),
      bind[TransformService].toInstance(mockTransformService),
      bind[XmlHandler].toInstance(mockXmlHandler),
      bind[XMLValidationService].toInstance(mockXmlValidationService),
      bind[AgentSubscriptionService].toInstance(mockAgentSubscriptionService),
      bind[SubscriptionService].toInstance(mockSubscriptionService),
      bind[SDESService].toInstance(mockSdesService),
      bind[SubmissionConnector].toInstance(mockSubmissionConnector),
      bind[FileDetailsRepository].toInstance(mockFileDetailsRepository),
      bind[IdentifierAuthAction].to[FakeIdentifierAuthAction],
      bind(classOf[Clock]).toInstance(fixedClock)
    )
    .build()

  private val submissionService = app.injector.instanceOf[SubmissionService]

  "SubmissionService" - {
    "submitLargeFile" - {
      "must send notification to SDES and persist the file details with Pending status" in {
        forAll { (agentRefNo: String, submissionDetails: SubmissionDetails, agentDetails: AgentResponseDetail, orgDetails: ResponseDetail) =>
          implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

          val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)

          mockSuccessfulFileNotification(submissionDetails, orgDetails, conversationId)
          mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
          mockSuccessfulAgentDetailsRetrieval(agentRefNo, agentDetails)
          mockSuccessfulFileDetailsPersistence()

          val result = submissionService.submitLargeFile(submissionDetails)

          result.futureValue.value mustBe conversationId
          assertOnFileDetails(agentRefNo, submissionDetails, agentDetails, conversationId)
        }
      }

      "must return error when a failure occurs during SDES notification" in {
        forAll {
          (
            agentRefNo: String,
            submissionDetails: SubmissionDetails,
            agentDetails: AgentResponseDetail,
            orgDetails: ResponseDetail,
            errorStatus: Short
          ) =>
            implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

            mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
            mockSuccessfulAgentDetailsRetrieval(agentRefNo, agentDetails)
            mockFailedFileNotification(errorStatus)

            val result = submissionService.submitLargeFile(submissionDetails)

            result.futureValue.left.value mustBe SdesSubmissionError(errorStatus)
        }
      }

      "must return error when a failure occurs during org contact details retrieval" in {
        forAll { (agentRefNo: String, submissionDetails: SubmissionDetails, errorStatus: Short) =>
          implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

          mockFailedOrgDetailsRetrieval(errorStatus)

          val result = submissionService.submitLargeFile(submissionDetails)

          result.futureValue.left.value mustBe ReadSubscriptionError(errorStatus)
        }
      }

      "must return error when a failure occurs during agent contact details retrieval" in {
        forAll { (agentRefNo: String, submissionDetails: SubmissionDetails, orgDetails: ResponseDetail, errorStatus: Short) =>
          implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

          mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
          mockFailedAgentDetailsRetrieval(errorStatus)

          val result = submissionService.submitLargeFile(submissionDetails)

          result.futureValue.left.value mustBe ReadSubscriptionError(errorStatus)
        }
      }

      "must return error when unable to persist file details" in {
        forAll { (agentRefNo: String, submissionDetails: SubmissionDetails, agentDetails: AgentResponseDetail, orgDetails: ResponseDetail) =>
          implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

          val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)

          mockSuccessfulFileNotification(submissionDetails, orgDetails, conversationId)
          mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
          mockSuccessfulAgentDetailsRetrieval(agentRefNo, agentDetails)
          mockFailedFileDetailsPersistence()

          val result = submissionService.submitLargeFile(submissionDetails)

          result.futureValue.left.value mustBe a[RepositoryError]
        }
      }
    }

    "submitNormalFile" - {

      val xmlSubmissionFilePath = "xml-submission-file-path"
      val testXml               = Elem("CBC_OECD", "CBC_OECD", xml.Null, TopScope, minimizeEmpty = true)

      "must submit file to EIS and persist the file details with Pending status" in {
        forAll { (agentRefNo: String, submissionDetails: SubmissionDetails, agentDetails: AgentResponseDetail, orgDetails: ResponseDetail) =>
          implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

          val conversationId      = ConversationId.fromUploadId(submissionDetails.uploadId)
          val submissionMetaData  = SubmissionMetaData.build(dateTimeNow, conversationId, submissionDetails.fileName)
          val agentContactDetails = AgentContactDetails(agentRefNo, agentDetails)

          mockSuccessfulReportTypeExtraction(submissionDetails)
          when(
            mockTransformService.addSubscriptionDetailsToSubmission(any[NodeSeq], mEq(orgDetails), mEq(submissionMetaData), mEq(Option(agentContactDetails)))
          )
            .thenReturn(testXml)
          when(mockAppConfig.submissionXSDFilePath).thenReturn(xmlSubmissionFilePath)
          when(mockXmlHandler.load(submissionDetails.documentUrl)).thenReturn(Success(testXml))
          when(mockXmlValidationService.validate(any[NodeSeq], mEq(xmlSubmissionFilePath))).thenReturn(Right(testXml))
          mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
          mockSuccessfulAgentDetailsRetrieval(agentRefNo, agentDetails)
          mockSuccessfulSubmissionToEIS(conversationId)
          mockSuccessfulFileDetailsPersistence()

          val result = submissionService.submitNormalFile(submissionDetails)

          result.futureValue.value mustBe conversationId
          assertOnFileDetails(agentRefNo, submissionDetails, agentDetails, conversationId)
        }
      }

      "must return error when a failure occurs during file submission to EIS" in {
        forAll {
          (
            agentRefNo: String,
            submissionDetails: SubmissionDetails,
            agentDetails: AgentResponseDetail,
            orgDetails: ResponseDetail,
            errorStatus: Short
          ) =>
            implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

            val conversationId      = ConversationId.fromUploadId(submissionDetails.uploadId)
            val submissionMetaData  = SubmissionMetaData.build(dateTimeNow, conversationId, submissionDetails.fileName)
            val agentContactDetails = AgentContactDetails(agentRefNo, agentDetails)

            mockSuccessfulReportTypeExtraction(submissionDetails)
            when(
              mockTransformService.addSubscriptionDetailsToSubmission(any[NodeSeq], mEq(orgDetails), mEq(submissionMetaData), mEq(Option(agentContactDetails)))
            )
              .thenReturn(testXml)
            when(mockAppConfig.submissionXSDFilePath).thenReturn(xmlSubmissionFilePath)
            when(mockXmlHandler.load(submissionDetails.documentUrl)).thenReturn(Success(testXml))
            when(mockXmlValidationService.validate(any[NodeSeq], mEq(xmlSubmissionFilePath))).thenReturn(Right(testXml))
            mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
            mockSuccessfulAgentDetailsRetrieval(agentRefNo, agentDetails)
            mockFailedSubmissionToEIS(errorStatus)

            val result = submissionService.submitNormalFile(submissionDetails)

            result.futureValue.left.value mustBe a[SubmissionServiceError]
        }
      }

      "must return error when a failure occurs during org contact details retrieval" in {
        forAll {
          (
            agentRefNo: String,
            submissionDetails: SubmissionDetails,
            agentDetails: AgentResponseDetail,
            orgDetails: ResponseDetail,
            errorStatus: Short
          ) =>
            implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

            val conversationId      = ConversationId.fromUploadId(submissionDetails.uploadId)
            val submissionMetaData  = SubmissionMetaData.build(dateTimeNow, conversationId, submissionDetails.fileName)
            val agentContactDetails = AgentContactDetails(agentRefNo, agentDetails)

            mockSuccessfulReportTypeExtraction(submissionDetails)
            when(
              mockTransformService.addSubscriptionDetailsToSubmission(any[NodeSeq], mEq(orgDetails), mEq(submissionMetaData), mEq(Option(agentContactDetails)))
            )
              .thenReturn(testXml)
            when(mockAppConfig.submissionXSDFilePath).thenReturn(xmlSubmissionFilePath)
            when(mockXmlHandler.load(submissionDetails.documentUrl)).thenReturn(Success(testXml))
            when(mockXmlValidationService.validate(any[NodeSeq], mEq(xmlSubmissionFilePath))).thenReturn(Right(testXml))
            mockFailedOrgDetailsRetrieval(errorStatus)

            val result = submissionService.submitNormalFile(submissionDetails)

            result.futureValue.left.value mustBe ReadSubscriptionError(errorStatus)
        }
      }

      "must return error when a failure occurs during agent contact details retrieval" in {
        forAll {
          (
            agentRefNo: String,
            submissionDetails: SubmissionDetails,
            agentDetails: AgentResponseDetail,
            orgDetails: ResponseDetail,
            errorStatus: Short
          ) =>
            implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

            val conversationId      = ConversationId.fromUploadId(submissionDetails.uploadId)
            val submissionMetaData  = SubmissionMetaData.build(dateTimeNow, conversationId, submissionDetails.fileName)
            val agentContactDetails = AgentContactDetails(agentRefNo, agentDetails)

            mockSuccessfulReportTypeExtraction(submissionDetails)
            when(
              mockTransformService.addSubscriptionDetailsToSubmission(any[NodeSeq], mEq(orgDetails), mEq(submissionMetaData), mEq(Option(agentContactDetails)))
            )
              .thenReturn(testXml)
            when(mockAppConfig.submissionXSDFilePath).thenReturn(xmlSubmissionFilePath)
            when(mockXmlHandler.load(submissionDetails.documentUrl)).thenReturn(Success(testXml))
            when(mockXmlValidationService.validate(any[NodeSeq], mEq(xmlSubmissionFilePath))).thenReturn(Right(testXml))
            mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
            mockFailedAgentDetailsRetrieval(errorStatus)

            val result = submissionService.submitNormalFile(submissionDetails)

            result.futureValue.left.value mustBe ReadSubscriptionError(errorStatus)
        }
      }

      "must return error when unable to load xml file" in {
        forAll {
          (
            agentRefNo: String,
            submissionDetails: SubmissionDetails,
            agentDetails: AgentResponseDetail,
            orgDetails: ResponseDetail
          ) =>
            implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

            val conversationId      = ConversationId.fromUploadId(submissionDetails.uploadId)
            val submissionMetaData  = SubmissionMetaData.build(dateTimeNow, conversationId, submissionDetails.fileName)
            val agentContactDetails = AgentContactDetails(agentRefNo, agentDetails)

            mockSuccessfulReportTypeExtraction(submissionDetails)
            when(
              mockTransformService.addSubscriptionDetailsToSubmission(any[NodeSeq], mEq(orgDetails), mEq(submissionMetaData), mEq(Option(agentContactDetails)))
            )
              .thenReturn(testXml)
            when(mockAppConfig.submissionXSDFilePath).thenReturn(xmlSubmissionFilePath)
            when(mockXmlHandler.load(submissionDetails.documentUrl)).thenReturn(Failure(new IOException("File load error")))

            val result = submissionService.submitNormalFile(submissionDetails)

            result.futureValue.left.value mustBe a[SubmissionServiceError]
        }
      }

      "must return error when unable to persist file details" in {
        forAll { (agentRefNo: String, submissionDetails: SubmissionDetails, agentDetails: AgentResponseDetail, orgDetails: ResponseDetail) =>
          implicit val fakeIdentifierRequest: IdentifierRequest[JsValue] = IdentifierRequest(mockRequest, AffinityGroup.Agent, Option(agentRefNo))

          val conversationId      = ConversationId.fromUploadId(submissionDetails.uploadId)
          val submissionMetaData  = SubmissionMetaData.build(dateTimeNow, conversationId, submissionDetails.fileName)
          val agentContactDetails = AgentContactDetails(agentRefNo, agentDetails)

          mockSuccessfulReportTypeExtraction(submissionDetails)
          when(
            mockTransformService.addSubscriptionDetailsToSubmission(any[NodeSeq], mEq(orgDetails), mEq(submissionMetaData), mEq(Option(agentContactDetails)))
          )
            .thenReturn(testXml)
          when(mockAppConfig.submissionXSDFilePath).thenReturn(xmlSubmissionFilePath)
          when(mockXmlHandler.load(submissionDetails.documentUrl)).thenReturn(Success(testXml))
          when(mockXmlValidationService.validate(any[NodeSeq], mEq(xmlSubmissionFilePath))).thenReturn(Right(testXml))
          mockSuccessfulOrgDetailsRetrieval(submissionDetails, orgDetails)
          mockSuccessfulAgentDetailsRetrieval(agentRefNo, agentDetails)
          mockSuccessfulSubmissionToEIS(conversationId)
          mockFailedFileDetailsPersistence()

          val result = submissionService.submitNormalFile(submissionDetails)

          result.futureValue.left.value mustBe a[RepositoryError]
        }
      }
    }
  }

  private def assertOnFileDetails(
    agentRefNo: String,
    submissionDetails: SubmissionDetails,
    agentDetails: AgentResponseDetail,
    conversationId: ConversationId
  ) = {
    val actualFileDetails = fileDetailsArgCaptor.getValue
    actualFileDetails._id mustBe conversationId
    actualFileDetails.messageRefId mustBe submissionDetails.messageSpecData.messageRefId
    actualFileDetails.reportingEntityName mustBe submissionDetails.messageSpecData.reportingEntityName
    actualFileDetails.reportType mustBe submissionDetails.messageSpecData.reportType
    actualFileDetails.status mustBe Pending
    actualFileDetails.name mustBe submissionDetails.fileName
    actualFileDetails.submitted mustBe dateTimeNow
    actualFileDetails.lastUpdated mustBe dateTimeNow
    actualFileDetails.agentDetails mustBe Option(AgentContactDetails(agentRefNo, agentDetails))
  }

  private def mockSuccessfulFileDetailsPersistence() =
    when(mockFileDetailsRepository.insert(fileDetailsArgCaptor.capture())).thenReturn(Future.successful(true))

  private def mockFailedFileDetailsPersistence() =
    when(mockFileDetailsRepository.insert(any[FileDetails])).thenReturn(Future.failed(new Exception("Repository error")))

  private def mockSuccessfulAgentDetailsRetrieval(agentRefNo: String, agentDetails: AgentResponseDetail) =
    when(mockAgentSubscriptionService.getContactInformation(mEq(agentRefNo))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Right(agentDetails)))

  private def mockFailedAgentDetailsRetrieval(errorStatus: Short) =
    when(mockAgentSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Left(ReadSubscriptionError(errorStatus))))

  private def mockSuccessfulFileNotification(submissionDetails: SubmissionDetails, orgDetails: ResponseDetail, conversationId: ConversationId) =
    when(mockSdesService.sendFileNotification(mEq(submissionDetails), mEq(orgDetails), mEq(conversationId))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Right(conversationId)))

  private def mockSuccessfulOrgDetailsRetrieval(submissionDetails: SubmissionDetails, orgDetails: ResponseDetail) =
    when(mockSubscriptionService.getContactInformation(mEq(submissionDetails.enrolmentId))(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Right(orgDetails)))

  private def mockFailedOrgDetailsRetrieval(errorStatus: Short) =
    when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(Future.successful(Left(ReadSubscriptionError(errorStatus))))

  private def mockFailedFileNotification(errorStatus: Short) =
    when(mockSdesService.sendFileNotification(any[SubmissionDetails], any[ResponseDetail], any[ConversationId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(Left(SdesSubmissionError(errorStatus))))

  private def mockSuccessfulReportTypeExtraction(submissionDetails: SubmissionDetails) =
    when(mockDataExtraction.getReportType(mEq(submissionDetails.messageSpecData.messageTypeIndic), any[Elem]))
      .thenReturn(submissionDetails.messageSpecData.reportType)

  private def mockSuccessfulSubmissionToEIS(conversationId: ConversationId) =
    when(mockSubmissionConnector.submitDisclosure(any[NodeSeq], mEq(conversationId))(any[HeaderCarrier]))
      .thenReturn(Future.successful(HttpResponse(OK, "submission response")))

  private def mockFailedSubmissionToEIS(errorStatus: Short) =
    when(mockSubmissionConnector.submitDisclosure(any[NodeSeq], any[ConversationId])(any[HeaderCarrier]))
      .thenReturn(Future.successful(HttpResponse(errorStatus, "Submission error")))
}
