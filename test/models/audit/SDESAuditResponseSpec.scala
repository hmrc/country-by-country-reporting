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

package models.audit

import base.SpecBase
import models.sdes.SdesCallback
import models.subscription.RequestDetailForUpdate
import play.api.libs.json.Json

class SDESAuditResponseSpec extends SpecBase {

  "SDESAuditResponse" - {
    "marshal with all values" in {
      val detailJson = Json.parse("""
                                    |{
                                    |  "notification": "FileProcessed",
                                    |  "filename": "conv-789",
                                    |  "checksumAlgorithm": "sub-123",
                                    |  "checksum": "1222374536363abef3633",
                                    |  "correlationID": "2024-08-07T10:13:09.429Z",
                                    |  "dateTime": "2025-06-17T16:45:45Z[Europe/London]",
                                    |  "failureReason": null
                                    |}
                                    |""".stripMargin)
      val fileDetails = detailJson.as[SdesCallback]
      val auditDetail = Json.parse("""
                                     |{
                                     |    "notification" : "FileProcessed",
                                     |    "conversationId" : "conv-789",
                                     |    "subscriptionId" : "sub-123",
                                     |    "messageRefId" : "msg-456",
                                     |    "fileName" : "test file.xml",
                                     |    "correlationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "checksumAlgorithm" : "SHA-256",
                                     |    "checksum" : "1222374536363abef3633",
                                     |    "dateTime" : "2024-08-07T10:13:09.429Z",
                                     |    "fileError" : true,
                                     |    "errorMessage" : "Virus Found"
                                     |}
                                     |""".stripMargin)
      val auditDetailRequest = auditDetail.as[SDESAuditResponse]
      val jsonObj = SDESAuditResponse(fileDetails)
      auditDetailRequest mustBe jsonObj
    }

    "marshal without optional values" in {

      val requestDetailJson = Json.parse("""
          |{
          |      "IDType": "SAFE",
          |      "IDNumber": "IDNumber",
          |      "tradingName": "Trading Name",
          |      "isGBUser": true,
          |      "primaryContact":
          |        {
          |          "organisation": {
          |            "organisationName": "orgName1"
          |          },
          |          "email": "test@email.com",
          |          "phone": "+4411223344"
          |        }
          |}
          |""".stripMargin)
      val requestDetailForUpdate = requestDetailJson.as[RequestDetailForUpdate]

      val auditDetail = Json.parse("""
          |{
          |      "subscriptionId": "IDNumber",
          |      "reportingEntityName": "Trading Name",
          |      "firstContactName": "orgName1",
          |      "firstContactEmail": "test@email.com",
          |      "firstContactPhoneNumber": "+4411223344",
          |      "hasSecondContact": false
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForUpdateOrgSubscriptionRequest]

      val jsonObj = AuditDetailForUpdateOrgSubscriptionRequest(requestDetailForUpdate)
      jsonObj mustBe auditDetailRequest
    }
  }
}
