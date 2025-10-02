/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.validation

import base.SpecBase
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction}
import models.audit.{Audit, AuditDetailForSubmissionValidation}
import models.submission.{CBC401, MessageSpecData, TestData}
import models.validation._
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchersSugar.eqTo
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}
import services.audit.AuditService
import services.validation.UploadedXmlValidationEngine

import java.time.LocalDateTime
import scala.concurrent.Future

class SubmissionValidationControllerSpec extends SpecBase {

  private val mockValidationEngine: UploadedXmlValidationEngine = mock[UploadedXmlValidationEngine]
  private val mockAuditService: AuditService                    = mock[AuditService]
  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(
        bind[UploadedXmlValidationEngine].toInstance(mockValidationEngine),
        bind[AuditService].toInstance(mockAuditService),
        bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
      )
      .build()

  private lazy val controller = app.injector.instanceOf[SubmissionValidationController]

  private val messageSpecData = MessageSpecData(
    messageRefId = "messageRefId123",
    messageTypeIndic = CBC401,
    reportType = TestData,
    startDate,
    endDate,
    reportingEntityName = "Reporting Entity Name"
  )

  override def beforeEach(): Unit = {
    reset(mockAuditService, mockValidationEngine)
    super.beforeEach()
  }

  "validateSubmission" - {
    val upscanUrl       = "/some-upscan-url"
    val conversationId  = "conversationId123"
    val subscriptionId  = "subscriptionId123"
    val fileReferenceId = "fileReferenceId123"
    val validateRequestJsonBody =
      Json.obj("url" -> upscanUrl, "conversationId" -> conversationId, "subscriptionId" -> subscriptionId, "fileReferenceId" -> fileReferenceId)

    when(mockValidationEngine.validateUploadSubmission(any()))
      .thenReturn(Future.successful(SubmissionValidationSuccess(messageSpecData)))

    "must validate a submission and return OK with success message" in {
      val submissionSuccess = SubmissionValidationSuccess(messageSpecData)

      when(mockValidationEngine.validateUploadSubmission(eqTo(upscanUrl)))
        .thenReturn(Future.successful(submissionSuccess))

      val request = FakeRequest(POST, routes.SubmissionValidationController.validateSubmission.url, FakeHeaders(), validateRequestJsonBody)

      val result: Future[Result] = controller.validateSubmission()(request)

      val expectedAuditDetail = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = Some(messageSpecData.messageRefId),
        messageTypeIndicator = Some(messageSpecData.messageTypeIndic.toString),
        reportingEntityName = Some(messageSpecData.reportingEntityName),
        reportType = Some(messageSpecData.reportType.toString),
        userType = "Organisation",
        fileError = false,
        errorMessage = None,
        errorURL = None,
        validationErrors = None
      )
      val expectedAudit = Audit(expectedAuditDetail)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(submissionSuccess)
      verify(mockAuditService, times(1)).sendAuditEvent(eqTo("FileValidation"), eqTo(Json.toJson(expectedAudit)))(any(), any())
    }

    "must validate a submission and return OK with failure message" in {
      val expectedErrors =
        Seq(
          GenericError(176, Message("xml.empty.field", List("Entity"))),
          GenericError(258, Message("xml.add.a.element", List("Summary")))
        )

      val submissionError = SubmissionValidationFailure(ValidationErrors(expectedErrors))

      when(mockValidationEngine.validateUploadSubmission(eqTo(upscanUrl)))
        .thenReturn(Future.successful(submissionError))

      val request                = FakeRequest(POST, routes.SubmissionValidationController.validateSubmission.url, FakeHeaders(), validateRequestJsonBody)
      val result: Future[Result] = controller.validateSubmission()(request)

      val expectedAuditDetail = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = "Organisation",
        fileError = true,
        errorMessage = Some("Failed to validate XML submission against schema"),
        errorURL = Some("/problem/data-errors"),
        validationErrors = Some(expectedErrors)
      )
      val expectedAudit = Audit(expectedAuditDetail)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(submissionError)
      verify(mockAuditService, times(1)).sendAuditEvent(eqTo("FileValidation"), eqTo(Json.toJson(expectedAudit)))(any(), any())
    }

    "must validate a submission and return BadRequest with error message" in {
      val invalidXmlError = InvalidXmlError("Sax Exception occurred")

      when(mockValidationEngine.validateUploadSubmission(eqTo(upscanUrl)))
        .thenReturn(Future.successful(invalidXmlError))

      val request = FakeRequest(POST, routes.SubmissionValidationController.validateSubmission.url, FakeHeaders(), validateRequestJsonBody)

      val result: Future[Result] = controller.validateSubmission()(request)

      val expectedAuditDetail = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = "Organisation",
        fileError = true,
        errorMessage = Some("Sax Exception occurred"),
        errorURL = Some("/problem/invalid-xml"),
        validationErrors = None
      )
      val expectedAudit = Audit(expectedAuditDetail)

      status(result) mustBe BAD_REQUEST
      verify(mockAuditService, times(1)).sendAuditEvent(eqTo("FileValidation"), eqTo(Json.toJson(expectedAudit)))(any(), any())
    }

    "return InternalServerError and audit when UpscanURL is missing from the request body" in {

      val invalidJsonBody = Json.obj(
        "someOtherField" -> "someValue"
      )
      val request = FakeRequest(POST, routes.SubmissionValidationController.validateSubmission.url, FakeHeaders(), invalidJsonBody)

      val result: Future[Result] = controller.validateSubmission()(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe "Missing upscan URL"
      verify(mockAuditService, times(0)).sendAuditEvent(any(), any())(any(), any())
    }

  }
}
