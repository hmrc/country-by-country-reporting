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
import models.audit.Audit
import models.upscan.{ReadyCallbackBody, Reference, UploadDetails}
import org.mockito.ArgumentMatchers.any
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import services.upscan.UpScanCallbackDispatcher

import java.time.Instant
import scala.concurrent.Future

class UploadCallbackControllerSpec extends SpecBase {
  val mockUpscanCallbackDispatcher: UpScanCallbackDispatcher = mock[UpScanCallbackDispatcher]

  val application = applicationBuilder()
    .overrides(bind[UpScanCallbackDispatcher].toInstance(mockUpscanCallbackDispatcher))
    .build()

  val readyCallbackBody: ReadyCallbackBody = ReadyCallbackBody(
    reference = Reference("test-file-reference"),
    downloadUrl = "https://example.com/download/test-file",
    uploadDetails = UploadDetails(
      uploadTimestamp = Instant.parse("2024-01-20T12:30:45Z"),
      checksum = "abc123checksum",
      fileMimeType = "text/xml",
      fileName = "test-file.xml",
      size = 1024L
    )
  )

  "UploadCallbackController" - {
    "must write Audit[JsValue] as a JSON object containing only details" in {
      val controller = application.injector.instanceOf[UploadCallbackController]

      val audit = Audit[JsValue](
        details = Json.obj("reference" -> "test-file-reference", "status" -> "READY")
      )

      Json.toJson(audit)(controller.writes) mustEqual Json.obj(
        "details" -> Json.obj("reference" -> "test-file-reference", "status" -> "READY")
      )
    }

    "must return OK when a valid ReadyCallbackBody callback is sent" in {
      when(mockUpscanCallbackDispatcher.handleCallback(any())(any())).thenReturn(Future.successful(true))

      val json = Json.obj(
        "fileStatus"  -> "READY",
        "reference"   -> "test-file-reference",
        "downloadUrl" -> readyCallbackBody.downloadUrl,
        "uploadDetails" -> Json.obj(
          "uploadTimestamp" -> "2024-01-20T12:30:45Z",
          "checksum"        -> readyCallbackBody.uploadDetails.checksum,
          "fileMimeType"    -> readyCallbackBody.uploadDetails.fileMimeType,
          "fileName"        -> readyCallbackBody.uploadDetails.fileName,
          "size"            -> readyCallbackBody.uploadDetails.size
        )
      )

      val request = FakeRequest("POST", routes.UploadCallbackController.callback().url).withBody(json)
      val result  = route(application, request).value
      status(result) mustEqual OK
    }

    "must return BadRequest when an invalid callback body is sent" in {
      val request = FakeRequest("POST", routes.UploadCallbackController.callback().url)
        .withBody(Json.obj("invalid" -> "body"))
      val result = route(application, request).value
      status(result) mustEqual BAD_REQUEST
    }
  }
}
