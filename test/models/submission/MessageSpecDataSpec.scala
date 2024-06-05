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

package models.submission

import base.SpecBase
import play.api.libs.json.{JsResultException, JsString, Json}

class MessageSpecDataSpec extends SpecBase {

  "MessageSpecDataSpec" - {
    "must serialize MessageSpec for MessageTypeIndic CBC401" in {
      val msd = MessageSpecData("XDSG111111", CBC401, "Reporting Entity", NewInformation)
      val expectedJson =
        Json.parse("""{"messageRefId":"XDSG111111","messageTypeIndic":"CBC401","reportingEntityName":"Reporting Entity","reportType":"NEW_INFORMATION"}""")
      Json.toJson(msd) mustBe expectedJson
    }
    "must deserialize MessageSpec for MessageTypeIndic CBC401" in {
      val json =
        Json.parse("""{"messageRefId":"XDSG333333","messageTypeIndic":"CBC401","reportingEntityName":"Reporting Entity","reportType":"NEW_INFORMATION"}""")
      val expected = MessageSpecData("XDSG333333", CBC401, "Reporting Entity", NewInformation)

      json.as[MessageSpecData] mustEqual expected
    }
    "must serialize MessageSpec for MessageTypeIndic CBC402" in {
      val msd = MessageSpecData("XDSG111111", CBC402, "Reporting Entity", NewInformation)
      val expectedJson =
        Json.parse("""{"messageRefId":"XDSG111111","messageTypeIndic":"CBC402","reportingEntityName":"Reporting Entity","reportType":"NEW_INFORMATION"}""")
      Json.toJson(msd) mustBe expectedJson
    }
    "must deserialize MessageSpec for MessageTypeIndic CBC402" in {
      val json =
        Json.parse("""{"messageRefId":"XDSG333333","messageTypeIndic":"CBC402","reportingEntityName":"Reporting Entity","reportType":"NEW_INFORMATION"}""")
      val expected = MessageSpecData("XDSG333333", CBC402, "Reporting Entity", NewInformation)

      json.as[MessageSpecData] mustEqual expected
    }
    "must fail to deserialize for any other MessageTypeIndic value" in {
      val json =
        Json.parse("""{"messageRefId":"XDSG333333","messageTypeIndic":"CBC123","reportingEntityName":"Reporting Entity","reportType":"NEW_INFORMATION"}""")

      a[JsResultException] must be thrownBy (json.as[MessageSpecData])
    }
  }

  "ReportType" - {

    "serialization" - {
      "must serialize TestData" in {
        Json.toJson[ReportType](TestData) mustBe JsString("TEST_DATA")
      }
      "must serialize NewInformation" in {
        Json.toJson[ReportType](NewInformation) mustBe JsString("NEW_INFORMATION")
      }
      "must serialize DeletionOfAllInformation" in {
        Json.toJson[ReportType](DeletionOfAllInformation) mustBe JsString("DELETION_OF_ALL_INFORMATION")
      }
      "must serialize NewInformationForExistingReport" in {
        Json.toJson[ReportType](NewInformationForExistingReport) mustBe JsString("NEW_INFORMATION_FOR_EXISTING_REPORT")
      }
      "must serialize CorrectionForExistingReport" in {
        Json.toJson[ReportType](CorrectionForExistingReport) mustBe JsString("CORRECTION_FOR_EXISTING_REPORT")
      }
      "must serialize DeletionForExistingReport" in {
        Json.toJson[ReportType](DeletionForExistingReport) mustBe JsString("DELETION_FOR_EXISTING_REPORT")
      }
      "must serialize CorrectionAndDeletionForExistingReport" in {
        Json.toJson[ReportType](CorrectionAndDeletionForExistingReport) mustBe JsString("CORRECTION_AND_DELETION_FOR_EXISTING_REPORT")
      }
      "must serialize CorrectionForReportingEntity" in {
        Json.toJson[ReportType](CorrectionForReportingEntity) mustBe JsString("CORRECTION_FOR_REPORTING_ENTITY")
      }
    }

    "deserialization" - {
      "must deserialize TestData" in {
        JsString("TEST_DATA").as[ReportType] mustBe TestData
      }
      "must deserialize NewInformation" in {
        JsString("NEW_INFORMATION").as[ReportType] mustBe NewInformation
      }
      "must deserialize DeletionOfAllInformation" in {
        JsString("DELETION_OF_ALL_INFORMATION").as[ReportType] mustBe DeletionOfAllInformation
      }
      "must deserialize NewInformationForExistingReport" in {
        JsString("NEW_INFORMATION_FOR_EXISTING_REPORT").as[ReportType] mustBe NewInformationForExistingReport
      }
      "must deserialize CorrectionForExistingReport" in {
        JsString("CORRECTION_FOR_EXISTING_REPORT").as[ReportType] mustBe CorrectionForExistingReport
      }
      "must deserialize DeletionForExistingReport" in {
        JsString("DELETION_FOR_EXISTING_REPORT").as[ReportType] mustBe DeletionForExistingReport
      }
      "must deserialize CorrectionAndDeletionForExistingReport" in {
        JsString("CORRECTION_AND_DELETION_FOR_EXISTING_REPORT").as[ReportType] mustBe CorrectionAndDeletionForExistingReport
      }
      "must deserialize CorrectionForReportingEntity" in {
        JsString("CORRECTION_FOR_REPORTING_ENTITY").as[ReportType] mustBe CorrectionForReportingEntity
      }
      "must not deserialize an invalid report type" in {
        a[JsResultException] must be thrownBy (JsString("ReportType").as[ReportType])
      }
    }

  }
}
