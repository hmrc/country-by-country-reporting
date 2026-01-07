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

package controllers

import config.AppConfig
import connectors.SubscriptionConnector
import controllers.auth.{IdentifierAuthAction, IdentifierAuthActionImpl, IdentifierRequest}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, spy, when}
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParsers, Request, Result}
import play.api.test.*
import play.api.test.Helpers.*
import play.api.{inject, Application}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpVerbs.POST

import scala.concurrent.Future

class SubscriptionControllerSpec extends ISpecBase {

  private def authorisedApp(): Application = {

    val readSubscriptionResponse =
      """
        |{
        |  "displaySubscriptionForCBCResponse" : {
        |    "responseCommon" : {
        |      "status" : "Pending",
        |      "processingDate" : "2026-01-05"
        |    },
        |    "responseDetail" : {
        |      "subscriptionID" : "testsubscriptionId",
        |      "isGBUser" : true,
        |      "primaryContact" : [{
        |        "organisation" : {
        |          "organisationName" : "testOrg"
        |        },
        |        "email" : "test@test.com"
        |      }]
        |    }
        |  }
        |}""".stripMargin

    val mockAppConfig = mock[AppConfig]

    when(mockAppConfig.enrolmentKey).thenReturn((input: String) =>
      input match {
        case "cbc"      => "HMRC-CBC-ORG"
        case "cbcNonUK" => "HMRC-CBC-NONUK-ORG"
        case _          => "HMRC-AS-AGENT"
      }
    )

    val realAuthAction = new IdentifierAuthActionImpl(
      authConnector = mock[AuthConnector],
      parser = BodyParsers.Default(),
      config = mockAppConfig
    )

    val authAction = spy(realAuthAction)

    doAnswer { invocation =>
      val request = invocation.getArgument(0, classOf[Request[AnyContent]])

      val block = invocation.getArgument(1).asInstanceOf[Request[AnyContent] => Future[Result]]

      block(IdentifierRequest(request, AffinityGroup.Organisation))
    }.when(authAction).invokeBlock(any(), any())

    val mockSubscriptionConnector = mock[SubscriptionConnector]

    when(
      mockSubscriptionConnector.readSubscriptionInformation(any())(any(), any())
    ).thenReturn(
      Future.successful(HttpResponse(OK, readSubscriptionResponse))
    )

    when(
      mockSubscriptionConnector.updateSubscription(any())(any(), any())
    ).thenReturn(
      Future.successful(HttpResponse(OK, "{}"))
    )

    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[IdentifierAuthAction].toInstance(authAction),
        inject.bind[AppConfig].toInstance(mockAppConfig),
        inject.bind[SubscriptionConnector].toInstance(mockSubscriptionConnector)
      )
      .configure(
        "auditing.enabled" -> "false"
      )
      .build()
  }

  "POST /subscription/read-subscription/:subscriptionId" should {

    "reject unauthenticated requests" in {
      val request =
        FakeRequest(POST, "/country-by-country-reporting/subscription/read-subscription/testid")

      val app = unauthorisedApp()

      running(app) {
        val result = route(app, request).getOrElse {
          fail("Route not defined.")
        }

        status(result) should (be(UNAUTHORIZED) or be(FORBIDDEN))
      }
    }

    "return 200 OK when authorised request" in {

      val request =
        FakeRequest(POST, "/country-by-country-reporting/subscription/read-subscription/testid")

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }
  "POST /subscription/update-subscription" should {

    "reject unauthenticated requests" in {
      val request =
        FakeRequest(POST, "/country-by-country-reporting/subscription/update-subscription")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(
            Json.obj(
              "someField" -> "someValue"
            )
          )

      val app = unauthorisedApp()

      running(app) {
        val result = route(app, request).getOrElse {
          fail("Route not defined.")
        }

        status(result) should (be(UNAUTHORIZED) or be(FORBIDDEN))
      }
    }

    "return 200 OK when authorised request" in {

      val requestDetailJson = Json.parse(
        """
          |{
          | "IDType": "SAFE",
          | "IDNumber": "IDNumber",
          | "tradingName": "Test Trading Name",
          | "isGBUser": true,
          | "primaryContact":
          |   {
          |     "organisation": {
          |       "organisationName": "TestorgName1"
          |     },
          |     "email": "test@email.com",
          |     "phone": "+4411223344"
          |   },
          | "secondaryContact":
          |   {
          |     "organisation": {
          |       "organisationName": "TestorgName2"
          |     },
          |     "email": "test@email.com",
          |     "phone": "+4411223344"
          |   }
          |}""".stripMargin
      )

      val request =
        FakeRequest(POST, "/country-by-country-reporting/subscription/update-subscription")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withJsonBody(requestDetailJson)

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }
}
