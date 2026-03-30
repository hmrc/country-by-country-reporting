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

package models.agentSubscription

import base.SpecBase
import play.api.libs.json.{JsValue, Json}

class AgentRequestDetailForUpdatePayloadSpec extends SpecBase {

  "AgentRequestDetailForUpdatePayload" - {
    "must deserialize from json" in {
      val payload =
        """
            |{
            |      "IDType": "ARN",
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
            |        "cbcId": "cbcId",
            |        "agentClient":"some-client"
            |}
            |""".stripMargin
      val expectedJson =
        """{"IDType":"ARN","IDNumber":"IDNumber","tradingName":"Trading Name","isGBUser":true,"primaryContact":{"organisation":{"organisationName":"orgName1"},"email":"test@email.com","phone":"+4411223344"},"cbcId":"cbcId","agentClient":"some-client"}"""
      val json: JsValue                      = Json.parse(payload)
      val agentRequestDetailForUpdatePayload = json.as[AgentRequestDetailForUpdatePayload]
      Json.toJson(agentRequestDetailForUpdatePayload) mustEqual Json.parse(expectedJson)

    }
  }
}
