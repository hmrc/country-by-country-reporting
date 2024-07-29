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

package connectors

import base.SpecBase
import com.github.tomakehurst.wiremock.http.RequestMethod
import generators.Generators
import models.agentSubscription.AgentSubscriptionEtmpRequest
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.OK
import wiremock.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class AgentSubscriptionConnectorSpec extends SpecBase with WireMockHelper with IntegrationPatience with Generators with ScalaCheckPropertyChecks {

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWireMock()
  }

  override def afterAll(): Unit = {
    stopWireMock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    resetWireMock()
  }

  override lazy val app: Application = applicationBuilder()
    .configure(
      conf = "microservice.services.update-agent-subscription.port" -> wireMockServer.port(),
      "microservice.services.create-agent-subscription.port"       -> wireMockServer.port(),
      "microservice.services.read-agent-subscription.port"         -> wireMockServer.port(),
      "microservice.services.read-agent-subscription.bearer-token" -> "local-token",
      "auditing.enabled"                                           -> "false"
    )
    .build()

  private lazy val connector: AgentSubscriptionConnector =
    app.injector.instanceOf[AgentSubscriptionConnector]

  private val errorCodes: Gen[Int] = Gen.chooseNum(400, 599)

  "AgentSubscriptionConnector" - {

    "create subscription" - {
      val agentCreateSubscriptionEndpoint = "/dac6/dct51a/v1"

      "must return status as OK for create Subscription" in {
        stubResponse(agentCreateSubscriptionEndpoint, OK, RequestMethod.POST)

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { sub =>
          val result = connector.createSubscription(sub)

          result.futureValue.status mustBe OK
        }
      }

      "must return an error status for invalid create Subscription" in {
        forAll(arbitrary[AgentSubscriptionEtmpRequest], errorCodes) { (sub, errorCode) =>
          stubResponse(agentCreateSubscriptionEndpoint, errorCode, RequestMethod.POST)

          val result = connector.createSubscription(sub)
          result.futureValue.status mustBe errorCode
        }
      }
    }

    "read subscription" - {
      val agentReadSubscriptionEndpoint = "/dac6/dct51c/v1"

      "must return status as OK for read Subscription" in {
        forAll(Gen.alphaNumStr) { agentRefNo =>
          stubResponse(s"$agentReadSubscriptionEndpoint/ARN/$agentRefNo", OK, RequestMethod.GET)

          val result = connector.readSubscription(agentRefNo)

          result.futureValue.status mustBe OK
        }
      }

      "must return an error status for  invalid read Subscription" in {

        forAll(Gen.alphaNumStr, errorCodes) { (agentRefNo, errorCode) =>
          stubResponse(s"$agentReadSubscriptionEndpoint/ARN/$agentRefNo", errorCode, RequestMethod.GET)

          val result = connector.readSubscription(agentRefNo)
          result.futureValue.status mustBe errorCode
        }
      }
    }

    "update subscription" - {
      val agentUpdateSubscriptionEndpoint = "/dac6/dct51b/v1"

      "must return status as OK for update Subscription" in {
        stubResponse(agentUpdateSubscriptionEndpoint, OK, RequestMethod.PUT)

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { sub =>
          val result = connector.updateSubscription(sub)
          result.futureValue.status mustBe OK
        }
      }

      "must return an error status for failed update Subscription" in {
        forAll(arbitrary[AgentSubscriptionEtmpRequest], errorCodes) { (sub, errorCode) =>
          stubResponse(agentUpdateSubscriptionEndpoint, errorCode, RequestMethod.PUT)

          val result = connector.updateSubscription(sub)
          result.futureValue.status mustBe errorCode
        }
      }
    }

  }
}
