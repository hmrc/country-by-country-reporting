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
