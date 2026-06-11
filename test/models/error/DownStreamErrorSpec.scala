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

package models.error

import base.SpecBase
import play.api.libs.json.Json

import java.time.LocalDateTime

class DownStreamErrorSpec extends SpecBase {

  "BusinessErrorDetail" - {
    "must serialize to JSON" in {
      val processingDate = LocalDateTime.parse("2024-01-20T12:30:45")
      val detail         = BusinessErrorDetail(processingDate, "422", "Invalid payload")

      Json.toJson(detail) mustBe Json.obj(
        "processingDate" -> "2024-01-20T12:30:45",
        "code"           -> "422",
        "text"           -> "Invalid payload"
      )
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "processingDate" -> "2024-01-20T12:30:45",
        "code"           -> "422",
        "text"           -> "Invalid payload"
      )

      json.as[BusinessErrorDetail] mustBe BusinessErrorDetail(
        processingDate = LocalDateTime.parse("2024-01-20T12:30:45"),
        code = "422",
        text = "Invalid payload"
      )
    }
  }

  "BusinessValidationError" - {
    "must serialize to JSON" in {
      val processingDate = LocalDateTime.parse("2024-01-20T12:30:45")
      val error = BusinessValidationError(
        BusinessErrorDetail(processingDate, "422", "Invalid payload")
      )

      Json.toJson(error) mustBe Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2024-01-20T12:30:45",
          "code"           -> "422",
          "text"           -> "Invalid payload"
        )
      )
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "errors" -> Json.obj(
          "processingDate" -> "2024-01-20T12:30:45",
          "code"           -> "422",
          "text"           -> "Invalid payload"
        )
      )

      json.as[BusinessValidationError] mustBe BusinessValidationError(
        BusinessErrorDetail(
          processingDate = LocalDateTime.parse("2024-01-20T12:30:45"),
          code = "422",
          text = "Invalid payload"
        )
      )
    }
  }

  "BackendSAPSystemError" - {
    "must serialize to JSON" in {
      val error = BackendSAPSystemError(
        BackendSAPSystemErrorDetail(
          code = "500",
          message = "Internal SAP error",
          logID = "log-123"
        )
      )

      Json.toJson(error) mustBe Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Internal SAP error",
          "logID"   -> "log-123"
        )
      )
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "error" -> Json.obj(
          "code"    -> "500",
          "message" -> "Internal SAP error",
          "logID"   -> "log-123"
        )
      )

      json.as[BackendSAPSystemError] mustBe BackendSAPSystemError(
        BackendSAPSystemErrorDetail(
          code = "500",
          message = "Internal SAP error",
          logID = "log-123"
        )
      )
    }
  }

  "SourceFaultDetail" - {
    "must serialize to JSON" in {
      val sourceFaultDetail = SourceFaultDetail(Seq("error-1", "error-2"))

      Json.toJson(sourceFaultDetail) mustBe Json.obj(
        "detail" -> Json.arr("error-1", "error-2")
      )
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "detail" -> Json.arr("error-1", "error-2")
      )

      json.as[SourceFaultDetail] mustBe SourceFaultDetail(Seq("error-1", "error-2"))
    }

  }

  "ErrorDetail" - {
    "must serialize to JSON" in {
      val errorDetail = ErrorDetail(
        timestamp = "2024-01-20T12:30:45Z",
        correlationId = Some("corr-123"),
        errorCode = "400",
        errorMessage = "Bad request",
        source = "ETMP",
        sourceFaultDetail = Some(SourceFaultDetail(Seq("fault-1", "fault-2")))
      )

      Json.toJson(errorDetail) mustBe Json.obj(
        "timestamp"     -> "2024-01-20T12:30:45Z",
        "correlationId" -> "corr-123",
        "errorCode"     -> "400",
        "errorMessage"  -> "Bad request",
        "source"        -> "ETMP",
        "sourceFaultDetail" -> Json.obj(
          "detail" -> Json.arr("fault-1", "fault-2")
        )
      )
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "timestamp"     -> "2024-01-20T12:30:45Z",
        "correlationId" -> "corr-123",
        "errorCode"     -> "400",
        "errorMessage"  -> "Bad request",
        "source"        -> "ETMP",
        "sourceFaultDetail" -> Json.obj(
          "detail" -> Json.arr("fault-1", "fault-2")
        )
      )

      json.as[ErrorDetail] mustBe ErrorDetail(
        timestamp = "2024-01-20T12:30:45Z",
        correlationId = Some("corr-123"),
        errorCode = "400",
        errorMessage = "Bad request",
        source = "ETMP",
        sourceFaultDetail = Some(SourceFaultDetail(Seq("fault-1", "fault-2")))
      )
    }
  }

  "ErrorDetails" - {
    "must serialize to JSON" in {
      val errorDetails = ErrorDetails(
        ErrorDetail(
          timestamp = "2024-01-20T12:30:45Z",
          correlationId = Some("corr-123"),
          errorCode = "400",
          errorMessage = "Bad request",
          source = "ETMP",
          sourceFaultDetail = Some(SourceFaultDetail(Seq("fault-1", "fault-2")))
        )
      )

      Json.toJson(errorDetails) mustBe Json.obj(
        "errorDetail" -> Json.obj(
          "timestamp"     -> "2024-01-20T12:30:45Z",
          "correlationId" -> "corr-123",
          "errorCode"     -> "400",
          "errorMessage"  -> "Bad request",
          "source"        -> "ETMP",
          "sourceFaultDetail" -> Json.obj(
            "detail" -> Json.arr("fault-1", "fault-2")
          )
        )
      )
    }

    "must deserialize from JSON" in {
      val json = Json.obj(
        "errorDetail" -> Json.obj(
          "timestamp"     -> "2024-01-20T12:30:45Z",
          "correlationId" -> "corr-123",
          "errorCode"     -> "400",
          "errorMessage"  -> "Bad request",
          "source"        -> "ETMP",
          "sourceFaultDetail" -> Json.obj(
            "detail" -> Json.arr("fault-1", "fault-2")
          )
        )
      )

      json.as[ErrorDetails] mustBe ErrorDetails(
        ErrorDetail(
          timestamp = "2024-01-20T12:30:45Z",
          correlationId = Some("corr-123"),
          errorCode = "400",
          errorMessage = "Bad request",
          source = "ETMP",
          sourceFaultDetail = Some(SourceFaultDetail(Seq("fault-1", "fault-2")))
        )
      )
    }
  }

}
