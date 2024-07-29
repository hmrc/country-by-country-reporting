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

package controllers.submission

import base.SpecBase
import config.AppConfig
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction, IdentifierRequest}
import controllers.routes._
import generators.Generators
import models.error.SubmissionServiceError
import models.submission.{ConversationId, SubmissionDetails}
import org.mockito.ArgumentMatchers.{any, eq => mEq}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.submission.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class SubmissionControllerSpec extends SpecBase with Generators with ScalaCheckDrivenPropertyChecks {

  private val mockAppConf           = mock[AppConfig]
  private val mockSubmissionService = mock[SubmissionService]

  override def beforeEach(): Unit = reset(mockAppConf, mockSubmissionService)

  "Submission controller" - {

    "must treat file having size <= maxNormalFileSizeBytes as normal file" in {
      forAll(arbitrary[SubmissionDetails]) { submissionDetails =>
        val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)

        when(mockAppConf.maxNormalFileSizeBytes).thenReturn(submissionDetails.fileSize)
        when(mockAppConf.maxLargeFileSizeBytes).thenReturn(submissionDetails.fileSize + 1)
        when(mockSubmissionService.submitNormalFile(mEq(submissionDetails))(any[IdentifierRequest[JsValue]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(conversationId)))

        val application = buildApplication()
        running(application) {
          val request = FakeRequest(POST, SubmissionController.submitDisclosure.url).withBody(Json.toJson(submissionDetails))

          val result = route(application, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(conversationId)
        }
      }
    }

    "must treat file having size > maxNormalFileSizeBytes as large file" in {
      forAll(arbitrary[SubmissionDetails]) { submissionDetails =>
        val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)

        when(mockAppConf.maxNormalFileSizeBytes).thenReturn(submissionDetails.fileSize - 1)
        when(mockAppConf.maxLargeFileSizeBytes).thenReturn(submissionDetails.fileSize + 1)
        when(mockSubmissionService.submitLargeFile(mEq(submissionDetails))(any[IdentifierRequest[JsValue]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Right(conversationId)))

        val application = buildApplication()
        running(application) {
          val request = FakeRequest(POST, SubmissionController.submitDisclosure.url).withBody(Json.toJson(submissionDetails))

          val result = route(application, request).value

          status(result) mustBe OK
          contentAsJson(result) mustBe Json.toJson(conversationId)
        }
      }
    }

    "must return INTERNAL_SERVER_ERROR when file is larger than maxLargeFileSizeBytes" in {
      forAll(arbitrary[SubmissionDetails]) { submissionDetails =>
        when(mockAppConf.maxNormalFileSizeBytes).thenReturn(0)
        when(mockAppConf.maxLargeFileSizeBytes).thenReturn(submissionDetails.fileSize - 1)

        val application = buildApplication()
        running(application) {
          val request = FakeRequest(POST, SubmissionController.submitDisclosure.url).withBody(Json.toJson(submissionDetails))

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
          verifyZeroInteractions(mockSubmissionService)
        }
      }
    }

    "must return InternalServerError when a service error occurs during normal file submission" in {
      forAll(arbitrary[SubmissionDetails]) { submissionDetails =>
        when(mockAppConf.maxNormalFileSizeBytes).thenReturn(submissionDetails.fileSize)
        when(mockAppConf.maxLargeFileSizeBytes).thenReturn(submissionDetails.fileSize + 1)
        when(mockSubmissionService.submitNormalFile(mEq(submissionDetails))(any[IdentifierRequest[JsValue]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(SubmissionServiceError("Some service error"))))

        val application = buildApplication()
        running(application) {
          val request = FakeRequest(POST, SubmissionController.submitDisclosure.url).withBody(Json.toJson(submissionDetails))

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }

    "must return INTERNAL_SERVER_ERROR when a service error occurs during large file submission" in {
      forAll(arbitrary[SubmissionDetails]) { submissionDetails =>
        when(mockAppConf.maxNormalFileSizeBytes).thenReturn(submissionDetails.fileSize - 1)
        when(mockAppConf.maxLargeFileSizeBytes).thenReturn(submissionDetails.fileSize + 1)
        when(mockSubmissionService.submitLargeFile(mEq(submissionDetails))(any[IdentifierRequest[JsValue]], any[HeaderCarrier]))
          .thenReturn(Future.successful(Left(SubmissionServiceError("Some service error"))))

        val application = buildApplication()
        running(application) {
          val request = FakeRequest(POST, SubmissionController.submitDisclosure.url).withBody(Json.toJson(submissionDetails))

          val result = route(application, request).value

          status(result) mustBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }

  private def buildApplication() =
    applicationBuilder()
      .overrides(
        bind[AppConfig].toInstance(mockAppConf),
        bind[SubmissionService].toInstance(mockSubmissionService),
        bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
      )
      .build()
}
