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
import models.validation.SubmissionValidationSuccess
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
    reportingEntityName = "Reporting Entity Name",
    reportType = TestData
  )

  "validateSubmission" - {

    when(mockValidationEngine.validateUploadSubmission(any()))
      .thenReturn(Future.successful(SubmissionValidationSuccess(messageSpecData)))

    "must validate a submission and return OK with success message" in {

      val upscanUrl = "/some-upscan-url"
      val validJsonBody = Json.obj(
        "url" -> upscanUrl
      )

      val submissionSuccess = SubmissionValidationSuccess(messageSpecData)

      when(mockValidationEngine.validateUploadSubmission(eqTo(upscanUrl)))
        .thenReturn(Future.successful(submissionSuccess))

      val request = FakeRequest(POST, routes.SubmissionValidationController.validateSubmission.url, FakeHeaders(), validJsonBody)
        .withHeaders(
          "X-Conversation-ID" -> "conversationId123",
          "X-Subscription-ID" -> "subscriptionId123"
        )

      val result: Future[Result] = controller.validateSubmission()(request)

      val expectedAuditDetail = AuditDetailForSubmissionValidation(
        conversationId = "conversationId123",
        subscriptionId = "subscriptionId123",
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

    "return InternalServerError and audit when UpscanURL is missing from the request body" in {

      val invalidJsonBody = Json.obj(
        "someOtherField" -> "someValue"
      )
      val request = FakeRequest(POST, routes.SubmissionValidationController.validateSubmission.url, FakeHeaders(), invalidJsonBody)
        .withHeaders(
          "X-Conversation-ID" -> "conversationId123",
          "X-Subscription-ID" -> "subscriptionId123"
        )

      val result: Future[Result] = controller.validateSubmission()(request)

      val expectedErrorMessage = s"Missing or invalid Upscan URL: List((/url,List(JsonValidationError(List(error.path.missing),List()))))"

      val expectedAuditDetail = AuditDetailForSubmissionValidation(
        conversationId = "conversationId123",
        subscriptionId = "subscriptionId123",
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = "Organisation",
        fileError = true,
        errorMessage = Some(expectedErrorMessage),
        errorURL = Some("country-by-country-reporting/problem/missing-url"),
        validationErrors = None
      )

      val expectedValidationAudit = Json.toJson(expectedAuditDetail)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsString(result) mustBe "Missing upscan URL"
      verify(mockAuditService, times(1)).sendAuditEvent(eqTo("FileValidation"), eqTo(expectedValidationAudit))(any(), any())
    }

  }

}
