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

import base.SpecBase
import models.agentSubscription.AgentContactDetails
import models.submission._
import models.xml.ValidationErrors
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.http.Status._
import play.api.inject.bind
import play.api.libs.json.JsValue
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, route, running, status, writeableOf_AnyContentAsXml, POST}
import repositories.submission.FileDetailsRepository
import services.EmailService
import services.audit.AuditService
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

class EISResponseControllerSpec extends SpecBase with BeforeAndAfterEach {

  private val randomUUID                               = UUID.randomUUID()
  val mockFileDetailsRepository: FileDetailsRepository = mock[FileDetailsRepository]
  val mockEmailService: EmailService                   = mock[EmailService]
  val mockAuditService: AuditService                   = mock[AuditService]

  override def beforeEach(): Unit = {
    reset(mockFileDetailsRepository, mockEmailService, mockAuditService)
    super.beforeEach()
  }

  val application: Application = applicationBuilder()
    .overrides(
      bind[FileDetailsRepository].toInstance(mockFileDetailsRepository),
      bind[EmailService].toInstance(mockEmailService),
      bind[AuditService].toInstance(mockAuditService)
    )
    .build()

  val xml: NodeSeq = <gsm:BREResponse xmlns:gsm="http://www.hmrc.gsi.gov.uk/gsm">
    <requestCommon>
      <receiptDate>2001-12-17T09:30:47.400Z</receiptDate>
      <regime>AEOI</regime>
      <conversationID>{randomUUID}</conversationID>
      <schemaVersion>1.0.0</schemaVersion>
    </requestCommon>
    <requestDetail>
      <GenericStatusMessage>
        <ValidationErrors>
          <FileError>
            <Code>50009</Code>
            <Details>Duplicate message ref IDs</Details>
          </FileError>
          <RecordError>
            <Code>80000</Code>
            <Details>Duplicate doc ref IDs</Details>
            <DocRefIDInError>CBCUSER001DHSJEURUT20001</DocRefIDInError>
            <DocRefIDInError>CBCUSER001DHSJEURUT20002</DocRefIDInError>
          </RecordError>
        </ValidationErrors>
        <ValidationResult>
          <Status>Rejected</Status>
        </ValidationResult>
      </GenericStatusMessage>
    </requestDetail>
  </gsm:BREResponse>

  "EISResponseController" - {
    "must return NoContent when input xml is valid" in {
      val fileDetails =
        FileDetails(
          ConversationId("conversationId123456"),
          "subscriptionId",
          "messageRefId",
          "Reporting Entity",
          TestData,
          Accepted,
          "file1.xml",
          LocalDateTime.now(),
          LocalDateTime.now()
        )

      when(mockFileDetailsRepository.findByConversationId(any[ConversationId])).thenReturn(Future.successful(Some(fileDetails)))
      when(mockFileDetailsRepository.updateStatus(any[String], any[FileStatus])).thenReturn(Future.successful(Some(fileDetails)))
      when(
        mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Seq(ACCEPTED)))
      when(mockAuditService.sendAuditEvent(any(), any())(any(), any())).thenReturn(Future.successful(Success))

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
        .withXmlBody(xml)

      val result = route(application, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockFileDetailsRepository, times(1)).updateStatus(any[String](), any[FileStatus]())
      verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])
    }

    "must return FORBIDDEN when auth token fails the validation" in {
      when(
        mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Seq(ACCEPTED)))

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"wrong")
        .withXmlBody(xml)

      val result = route(application, request).value

      status(result) mustEqual FORBIDDEN
    }

    "must not send an email when on the fast journey and file upload is Rejected" in {
      val fileDetails =
        FileDetails(
          ConversationId("conversationId123456"),
          "subscriptionId",
          "messageRefId",
          "Reporting Entity",
          TestData,
          Rejected(ValidationErrors(None, None)),
          "file1.xml",
          LocalDateTime.now(),
          LocalDateTime.now()
        )
      when(mockFileDetailsRepository.findByConversationId(any[ConversationId])).thenReturn(Future.successful(Some(fileDetails)))
      when(mockAuditService.sendAuditEvent(any[String], any[JsValue])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Success))
      when(mockFileDetailsRepository.updateStatus(any[String], any[FileStatus])).thenReturn(Future.successful(Some(fileDetails)))
      when(
        mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Seq(ACCEPTED)))

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
        .withXmlBody(xml)

      val result = route(application, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockEmailService, times(0)).sendAndLogEmail(any[String],
                                                         any[String],
                                                         any[String],
                                                         any[Option[AgentContactDetails]],
                                                         any[Boolean],
                                                         any[ReportType]
      )(any[HeaderCarrier])
      verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])
    }

    "must not send an email when on the fast journey and file upload is Pending" in {
      val fileDetails =
        FileDetails(
          ConversationId("conversationId123456"),
          "subscriptionId",
          "messageRefId",
          "Reporting Entity",
          TestData,
          Pending,
          "file1.xml",
          LocalDateTime.now(),
          LocalDateTime.now()
        )
      when(mockFileDetailsRepository.findByConversationId(any[ConversationId])).thenReturn(Future.successful(Some(fileDetails)))
      when(mockAuditService.sendAuditEvent(any[String], any[JsValue])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Success))
      when(mockFileDetailsRepository.updateStatus(any[String], any[FileStatus])).thenReturn(Future.successful(Some(fileDetails)))
      when(
        mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Seq(ACCEPTED)))

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
        .withXmlBody(xml)

      val result = route(application, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockEmailService, times(0)).sendAndLogEmail(any[String],
                                                         any[String],
                                                         any[String],
                                                         any[Option[AgentContactDetails]],
                                                         any[Boolean],
                                                         any[ReportType]
      )(any[HeaderCarrier])
      verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])
    }

    "must send an email when on the slow journey and file upload is Accepted" in {
      val fileDetails =
        FileDetails(
          ConversationId("conversationId123456"),
          "subscriptionId",
          "messageRefId",
          "Reporting Entity",
          TestData,
          Accepted,
          "file1.xml",
          LocalDateTime.now().minusSeconds(11),
          LocalDateTime.now()
        )
      when(mockFileDetailsRepository.findByConversationId(any[ConversationId])).thenReturn(Future.successful(Some(fileDetails)))
      when(mockAuditService.sendAuditEvent(any[String], any[JsValue])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Success))
      when(mockFileDetailsRepository.updateStatus(any[String], any[FileStatus])).thenReturn(Future.successful(Some(fileDetails)))
      when(
        mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Seq(ACCEPTED)))

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
        .withXmlBody(xml)

      val result = route(application, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockEmailService, times(1)).sendAndLogEmail(any[String],
                                                         any[String],
                                                         any[String],
                                                         any[Option[AgentContactDetails]],
                                                         any[Boolean],
                                                         any[ReportType]
      )(any[HeaderCarrier])
      verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])
    }

    "must send an email when on the slow journey and file upload is Rejected" in {
      val fileDetails =
        FileDetails(
          ConversationId("conversationId123456"),
          "subscriptionId",
          "messageRefId",
          "Reporting Entity",
          TestData,
          Rejected(ValidationErrors(None, None)),
          "file1.xml",
          LocalDateTime.now().minusSeconds(11),
          LocalDateTime.now()
        )
      when(mockFileDetailsRepository.findByConversationId(any[ConversationId])).thenReturn(Future.successful(Some(fileDetails)))
      when(mockAuditService.sendAuditEvent(any[String], any[JsValue])(any[HeaderCarrier], any[ExecutionContext])).thenReturn(Future.successful(Success))
      when(mockFileDetailsRepository.updateStatus(any[String], any[FileStatus])).thenReturn(Future.successful(Some(fileDetails)))
      when(
        mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(Seq(ACCEPTED)))

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
        .withXmlBody(xml)

      val result = route(application, request).value

      status(result) mustEqual NO_CONTENT
      verify(mockEmailService, times(1)).sendAndLogEmail(any[String],
                                                         any[String],
                                                         any[String],
                                                         any[Option[AgentContactDetails]],
                                                         any[Boolean],
                                                         any[ReportType]
      )(any[HeaderCarrier])
      verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])
    }

    "must return BadRequest when input xml is invalid" in {

      val invalidXml = <test>invalid</test>

      val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
        .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
        .withXmlBody(invalidXml)

      val result = route(application, request).value

      status(result) mustEqual BAD_REQUEST
      verify(mockFileDetailsRepository, never).updateStatus(any[String](), any[FileStatus]())
    }

    "must return InternalServerError on failing to update the status" in {
      when(mockFileDetailsRepository.findByConversationId(any[ConversationId])).thenReturn(Future.failed(new RuntimeException("Failed to get file details")))
      when(mockAuditService.sendAuditEvent(any(), any())(any(), any())).thenReturn(Future.successful(Success))
      when(mockFileDetailsRepository.updateStatus(any[String](), any[FileStatus]())).thenReturn(Future.successful(None))

      running(application) {
        val request = FakeRequest(POST, routes.EISResponseController.processEISResponse.url)
          .withHeaders("x-conversation-id" -> randomUUID.toString, HeaderNames.authorisation -> s"Bearer token")
          .withXmlBody(xml)

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
        verify(mockFileDetailsRepository, times(1)).updateStatus(any[String](), any[FileStatus]())
      }
    }
  }
}
