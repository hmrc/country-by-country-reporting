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
import models.submission.ConversationId
import models.upscan.UploadId
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import wiremock.WireMockHelper

import java.util.UUID

class SubmissionConnectorSpec extends SpecBase with GuiceOneAppPerSuite with WireMockHelper with IntegrationPatience {

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

  private val submissionUrl = "/dac6/dct52c/v1"

  private val conversationId = ConversationId.fromUploadId(UploadId(UUID.randomUUID().toString))

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .configure(
      "microservice.services.submission.port"         -> wireMockServer.port(),
      "microservice.services.submission.environment"  -> "local",
      "microservice.services.submission.bearer-token" -> "token"
    )
    .build()

  lazy val connector: SubmissionConnector = app.injector.instanceOf[SubmissionConnector]

  "Submission Connector" - {
    "should return OK" - {
      "when the backend returns a valid successful response" in {
        stubResponse(submissionUrl, OK)

        val xml = <test></test>

        whenReady(connector.submitDisclosure(xml, conversationId)) { result =>
          result.status mustBe OK
        }
      }
    }

    "throw an exception" - {
      "when upscan returns a 4xx response" in {
        stubResponse(submissionUrl, BAD_REQUEST)

        val xml    = <test></test>
        val result = connector.submitDisclosure(xml, conversationId)

        result.futureValue.status mustBe BAD_REQUEST
      }

      "when upscan returns 5xx response" in {
        stubResponse(submissionUrl, SERVICE_UNAVAILABLE)

        val xml    = <test></test>
        val result = connector.submitDisclosure(xml, conversationId)
        result.futureValue.status mustBe SERVICE_UNAVAILABLE
      }

      "when upscan returns 408 response" in {
        stubResponse(submissionUrl, REQUEST_TIMEOUT)

        val xml    = <test></test>
        val result = connector.submitDisclosure(xml, conversationId)
        result.futureValue.status mustBe REQUEST_TIMEOUT
      }
    }

  }
}
