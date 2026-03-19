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

package models.audit

import base.SpecBase
import models.agentSubscription.{AgentClientDetails, AgentSubscriptionEtmpRequest}
import play.api.libs.json.Json

class AuditDetailForUpdateAgentRequestSpec extends SpecBase {
  val requestDetailJson = Json.parse("""
      |{
      |      "idType": "ARN",
      |      "idNumber": "ARN1234567",
      |      "tradingName": "Trading Name",
      |      "gbUser": true,
      |      "primaryContact":
      |        {
      |          "organisation": {
      |            "name": "orgName1"
      |          },
      |          "email": "test@email.com",
      |          "phone": "+4411223344"
      |        },
      |      "secondaryContact":
      |        {
      |          "organisation": {
      |            "name": "orgName2"
      |          },
      |          "email": "test@email.com",
      |          "phone": "+4411223344"
      |        }
      |}
      |""".stripMargin)

  val agentSubscriptionEtmpRequest: AgentSubscriptionEtmpRequest = requestDetailJson.as[AgentSubscriptionEtmpRequest]

  "AuditDetailsForUpdateAgentRequest " - {
    "marshal with all values" in {

      val agentClientDetails = AgentClientDetails(Some("cbcId-123"), Some("Test Business"))

      val result = AuditDetailForUpdateAgentRequest(agentSubscriptionEtmpRequest, agentClientDetails)
      result.subscriptionId mustBe Some("cbcId-123")
      result.reportingEntityName mustBe Some("Test Business")
      result.firstContactName mustBe Some("orgName1")
      result.firstContactEmail mustBe "test@email.com"
      result.firstContactPhoneNumber mustBe Some("+4411223344")
      result.hasSecondContact mustBe true
      result.secondContactName mustBe Some("orgName2")
      result.secondContactEmail mustBe Some("test@email.com")
      result.secondContactPhoneNumber mustBe Some("+4411223344")

    }

    "subscription and reportingEntityName have none values when AgentClientDetails has none values" in {
      val agentClientDetails = AgentClientDetails(None, None)

      val result = AuditDetailForUpdateAgentRequest(agentSubscriptionEtmpRequest, agentClientDetails)
      result.subscriptionId mustBe None
      result.reportingEntityName mustBe None
      result.firstContactName mustBe Some("orgName1")
      result.firstContactEmail mustBe "test@email.com"
      result.firstContactPhoneNumber mustBe Some("+4411223344")
      result.hasSecondContact mustBe true
      result.secondContactName mustBe Some("orgName2")
      result.secondContactEmail mustBe Some("test@email.com")
      result.secondContactPhoneNumber mustBe Some("+4411223344")
    }
  }

}
