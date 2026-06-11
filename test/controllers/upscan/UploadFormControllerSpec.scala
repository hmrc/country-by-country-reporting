/*
 * Copyright 2026 HM Revenue & Customs
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

package controllers.upscan

import base.SpecBase
import models.upscan.{InProgress, Reference, UploadId, UploadSessionDetails}
import org.bson.types.ObjectId
import org.mockito.ArgumentMatchers.any
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.libs.json.Json.obj
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import repositories.upscan.UpScanSessionRepository
import services.upscan.UploadProgressTracker

import scala.concurrent.Future

class UploadFormControllerSpec extends SpecBase {
  val mockUploadProgressTracker: UploadProgressTracker = mock[UploadProgressTracker]
  val mockRepository: UpScanSessionRepository          = mock[UpScanSessionRepository]

  val application: Application = applicationBuilder()
    .overrides(
      bind[UploadProgressTracker].toInstance(mockUploadProgressTracker),
      bind[UpScanSessionRepository].toInstance(mockRepository)
    )
    .build()

  "UploadFormController" - {
    "requestUpload" - {
      "must return a bad request when an invalid UpscanIdentifiers is sent" in {
        val request = FakeRequest("POST", routes.UploadFormController.requestUpload.url).withBody(obj())
        val result  = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }

      "must return ok when a valid UpscanIdentifiers is sent" in {
        val request = FakeRequest("POST", routes.UploadFormController.requestUpload.url).withBody(
          obj(
            "uploadId"      -> obj("value" -> "test-upload-id"),
            "fileReference" -> "test-file-reference"
          )
        )
        when(mockUploadProgressTracker.requestUpload(any(), any())).thenReturn(Future.successful(true))
        val result = route(application, request).value
        status(result) mustEqual OK
      }
    }

    "getDetails" - {
      "Returns not found when no details are found for the given upload id" in {
        when(mockRepository.findByUploadId(any())).thenReturn(Future.successful(None))
        val request = FakeRequest("GET", routes.UploadFormController.getDetails("test-upload-id").url)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND
      }

      "Returns ok when details are found for the given upload id" in {
        val uploadSessionDetails = UploadSessionDetails(
          _id = ObjectId.get(),
          uploadId = UploadId("test-upload-id"),
          reference = Reference("test-file-reference"),
          status = InProgress
        )
        when(mockRepository.findByUploadId(any())).thenReturn(Future.successful(Some(uploadSessionDetails)))
        val request = FakeRequest("GET", routes.UploadFormController.getDetails("test-upload-id").url)
        val result  = route(application, request).value
        status(result) mustEqual OK
        contentAsJson(result) mustEqual Json.toJson(uploadSessionDetails)

      }
    }

    "getStatus" - {
      "Returns not found when no status is found for the given upload id" in {
        when(mockUploadProgressTracker.getUploadResult(any())).thenReturn(Future.successful(None))
        val request = FakeRequest("GET", routes.UploadFormController.getStatus("test-upload-id").url)
        val result  = route(application, request).value
        status(result) mustEqual NOT_FOUND
      }

      "Returns ok when status is found for the given upload id" in {
        when(mockUploadProgressTracker.getUploadResult(any())).thenReturn(Future.successful(Some(InProgress)))
        val request = FakeRequest("GET", routes.UploadFormController.getStatus("test-upload-id").url)
        val result  = route(application, request).value
        status(result) mustEqual OK
      }
    }

  }
}
