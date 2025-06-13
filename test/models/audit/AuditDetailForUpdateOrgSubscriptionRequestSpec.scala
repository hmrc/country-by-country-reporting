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
import models.subscription.RequestDetailForUpdate
import play.api.libs.json.Json

class AuditDetailForUpdateOrgSubscriptionRequestSpec extends SpecBase {

  "AuditDetailsForUpdateOrgSubscriptionRequest" - {
    "marshal with all values" in {

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
          |        },
          |      "secondaryContact":
          |        {
          |          "organisation": {
          |            "organisationName": "orgName2"
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
          |      "hasSecondContact": true,
          |      "secondContactName": "orgName2",
          |      "secondContactEmail": "test@email.com",
          |      "secondContactPhoneNumber": "+4411223344"
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForUpdateOrgSubscriptionRequest]

      val jsonObj = AuditDetailForUpdateOrgSubscriptionRequest(requestDetailForUpdate)
      jsonObj mustBe auditDetailRequest
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
