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

import connectors.AgentSubscriptionConnector
import controllers.auth.{AgentOnlyAuthAction, AgentOnlyAuthActionImpl, AgentOnlyRequest}
import models.agentSubscription.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, spy, when}
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParsers, Request, Result}
import play.api.test.*
import play.api.test.Helpers.*
import play.api.{inject, Application}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpVerbs.POST

import scala.concurrent.Future

class AgentSubscriptionControllerSpec extends ISpecBase {

  val subscriptionResponseJson: String =
    s"""{
       |  "success": {
       |    "processingDate": "2023-05-17T09:26:17Z",
       |    "agent": {
       |      "arn": "111111111",
       |      "tradingName": "Test Ltd",
       |      "gbUser": true,
       |      "primaryContact": {
       |        "organisation": {
       |          "name": "Test Org"
       |        },
       |        "email": "test1@test.com",
       |        "phone": "0123456789",
       |        "mobile": "0123456789"
       |      },
       |      "secondaryContact": {
       |        "organisation": {
       |          "name": "Test Org 2"
       |        },
       |        "email": "test2@test.com",
       |        "phone": "0123456000",
       |        "mobile": "0123456789"
       |      }
       |    }
       |  }
       |}""".stripMargin

  private def authorisedApp(): Application = {

    val realAuthAction = new AgentOnlyAuthActionImpl(
      authConnector = mock[AuthConnector],
      parser = BodyParsers.Default()
    )

    val authAction = spy(realAuthAction)

    doAnswer { invocation =>
      val request = invocation.getArgument(0, classOf[Request[AnyContent]])

      val block = invocation.getArgument(1).asInstanceOf[Request[AnyContent] => Future[Result]]

      block(AgentOnlyRequest("testAgentReferenceNumber", request))
    }.when(authAction).invokeBlock(any(), any())

    val mockConnector = mock[AgentSubscriptionConnector]

    when(
      mockConnector.createSubscription(any())(any(), any())
    ).thenReturn(
      Future.successful(HttpResponse(CREATED, "{}"))
    )

    when(
      mockConnector.readSubscription(any())(any(), any())
    ).thenReturn(
      Future.successful(HttpResponse(OK, subscriptionResponseJson))
    )

    when(
      mockConnector.updateSubscription(any())(any(), any())
    ).thenReturn(
      Future.successful(HttpResponse(OK, "{}"))
    )

    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[AgentOnlyAuthAction].toInstance(authAction),
        inject.bind[AgentSubscriptionConnector].toInstance(mockConnector)
      )
      .configure(
        "auditing.enabled" -> "false"
      )
      .build()
  }

  "POST /agent/subscription/create-subscription" should {

    "reject unauthenticated requests" in {
      val request =
        FakeRequest(POST, "/country-by-country-reporting/agent/subscription/create-subscription")
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

        status(result) should be(UNAUTHORIZED)
      }
    }

    "return 200 OK when authorised request" in {

      val validCreateSubscriptionRequest = CreateAgentSubscriptionRequest(
        createAgentSubscriptionForCBCRequest = AgentCreateSubscriptionRequest(
          requestCommon = AgentRequestCommonForSubscription.apply(),
          requestDetail = AgentRequestDetail(
            "TestIDType",
            "TestIDNumber",
            None,
            true,
            AgentContactInformation(
              organisationDetails = AgentDetails("testOrganisation"),
              email = "test@test.com",
              phone = None,
              mobile = None
            ),
            None
          )
        )
      )

      val jsonBody = Json.toJson(validCreateSubscriptionRequest)

      val request =
        FakeRequest(POST, "/country-by-country-reporting/agent/subscription/create-subscription")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(jsonBody)

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }

  "POST /agent/subscription/read-subscription" should {

    "reject unauthenticated requests" in {
      val request = FakeRequest(POST, "/country-by-country-reporting/agent/subscription/read-subscription")

      val app = unauthorisedApp()

      running(app) {
        val result = route(app, request).getOrElse {
          fail("Route not defined.")
        }

        status(result) should be(UNAUTHORIZED)
      }
    }

    "return 200 OK when authorised request" in {

      val request = FakeRequest(POST, "/country-by-country-reporting/agent/subscription/read-subscription")

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }

  "POST /agent/subscription/update-subscription" should {

    "reject unauthenticated requests" in {
      val request = FakeRequest(POST, "/country-by-country-reporting/agent/subscription/update-subscription")
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

        status(result) should be(UNAUTHORIZED)
      }
    }

    "return 200 OK when authorised request" in {

      val validRequest = AgentRequestDetailForUpdate(
        "TestIDType",
        "TestIDNumber",
        None,
        true,
        AgentContactInformation(
          organisationDetails = AgentDetails("testOrganisation"),
          email = "test@test.com",
          phone = None,
          mobile = None
        ),
        None
      )

      val jsonBody = Json.toJson(validRequest)

      val request = FakeRequest(POST, "/country-by-country-reporting/agent/subscription/update-subscription")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withBody(jsonBody)

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }

}
