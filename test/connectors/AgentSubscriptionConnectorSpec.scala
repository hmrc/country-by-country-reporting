/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import base.{SpecBase, WireMockServerHandler}
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import generators.Generators
import models.agentSubscription.{CreateAgentSubscriptionRequest, DisplayAgentSubscriptionForCBCRequest, UpdateAgentSubscriptionForCBCRequest}
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.OK

import scala.concurrent.ExecutionContext.Implicits.global

class AgentSubscriptionConnectorSpec extends SpecBase with WireMockServerHandler with Generators with ScalaCheckPropertyChecks {

  override lazy val app: Application = applicationBuilder()
    .configure(
      conf = "microservice.services.update-agent-subscription.port" -> server.port(),
      "microservice.services.create-agent-subscription.port"        -> server.port(),
      "microservice.services.read-agent-subscription.port"          -> server.port(),
      "microservice.services.read-agent-subscription.bearer-token"  -> "local-token",
      "auditing.enabled" -> "false"
    )
    .build()

  lazy val connector: AgentSubscriptionConnector =
    app.injector.instanceOf[AgentSubscriptionConnector]

  private val errorCodes: Gen[Int] = Gen.chooseNum(400, 599)

  "AgentSubscriptionConnector" - {

    "create subscription" - {
      "must return status as OK for read Subscription" in {
        stubResponse(
          "/dac/DCTxxc/v1",
          OK
        )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { sub =>
          val result = connector.createSubscription(sub)

          result.futureValue.status mustBe OK
        }
      }

      "must return an error status for  invalid read Subscription" in {

        forAll(arbitrary[CreateAgentSubscriptionRequest], errorCodes) { (sub, errorCode) =>
          stubResponse(
            "/dac/DCTxxc/v1",
            errorCode
          )

          val result = connector.createSubscription(sub)
          result.futureValue.status mustBe errorCode
        }
      }
    }

    "read subscription" - {
      "must return status as OK for read Subscription" in {
        stubResponse(
          "/dac/DCTxxd/v1",
          OK
        )

        forAll(arbitrary[DisplayAgentSubscriptionForCBCRequest]) { sub =>
          val result = connector.readSubscription(sub)

          result.futureValue.status mustBe OK
        }
      }

      "must return an error status for  invalid read Subscription" in {

        forAll(arbitrary[DisplayAgentSubscriptionForCBCRequest], errorCodes) { (sub, errorCode) =>
          stubResponse(
            "/dac/DCTxxd/v1",
            errorCode
          )

          val result = connector.readSubscription(sub)
          result.futureValue.status mustBe errorCode
        }
      }
    }

    "update subscription" - {
      "must return status as OK for update Subscription" in {
        stubResponse(
          "/dac/DCTxxe/v1",
          OK
        )

        forAll(arbitrary[UpdateAgentSubscriptionForCBCRequest]) { sub =>
          val result = connector.updateSubscription(sub)
          result.futureValue.status mustBe OK
        }
      }

      "must return an error status for failed update Subscription" in {

        forAll(arbitrary[UpdateAgentSubscriptionForCBCRequest], errorCodes) { (sub, errorCode) =>
          stubResponse(
            "/dac/DCTxxe/v1",
            errorCode
          )

          val result = connector.updateSubscription(sub)
          result.futureValue.status mustBe errorCode
        }
      }
    }

  }

  private def stubResponse(
    expectedUrl: String,
    expectedStatus: Int
  ): StubMapping =
    server.stubFor(
      post(urlEqualTo(expectedUrl))
        .willReturn(
          aResponse()
            .withStatus(expectedStatus)
        )
    )
}
