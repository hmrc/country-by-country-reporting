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

package connectors

import base.SpecBase
import generators.Generators
import models.sdes._
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT}
import wiremock.WireMockHelper

import scala.concurrent.ExecutionContext.Implicits.global

class SDESConnectorSpec extends SpecBase with IntegrationPatience with WireMockHelper with Generators with ScalaCheckPropertyChecks {

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
    .configure(conf = "microservice.services.sdes.port" -> wireMockServer.port())
    .build()

  lazy val connector: SDESConnector = app.injector.instanceOf[SDESConnector]

  private val statusCodes = Table(
    ("sdesStatusCode", "expectedConnectorResult"),
    (NO_CONTENT, Right(NO_CONTENT)),
    (BAD_REQUEST, Left(BAD_REQUEST)),
    (INTERNAL_SERVER_ERROR, Left(INTERNAL_SERVER_ERROR))
  )

  "SDESConnector" - {
    forAll(statusCodes) { (sdesStatusCode, expectedConnectorResult) =>
      s"sendFileNotification must return $expectedConnectorResult when SDES returns status $sdesStatusCode" in {
        stubResponse("/sdes-stub/notification/fileready", sdesStatusCode)

        forAll { fileTransferNotification: FileTransferNotification =>
          val result = connector.sendFileNotification(fileTransferNotification)

          result.futureValue mustBe expectedConnectorResult
        }
      }
    }
  }

}
