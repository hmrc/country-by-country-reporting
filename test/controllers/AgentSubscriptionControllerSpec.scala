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

package controllers

import base.SpecBase
import controllers.auth.{AgentOnlyAuthAction, FakeAgentOnlyAuthAction}
import generators.Generators
import models.agentSubscription.{AgentResponseDetail, AgentSubscriptionEtmpRequest, CreateAgentSubscriptionRequest}
import models.error._
import org.apache.pekko.http.javadsl.model.DateTime
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary.arbitrary
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.mvc.Results.{BadRequest, Conflict, Forbidden, InternalServerError, NotFound, ServiceUnavailable}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.AgentSubscriptionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionControllerSpec extends SpecBase with Generators with ScalaCheckPropertyChecks {

  val mockAgentSubscriptionService: AgentSubscriptionService =
    mock[AgentSubscriptionService]

  val application: Application = applicationBuilder()
    .overrides(
      bind[AgentSubscriptionService].toInstance(mockAgentSubscriptionService),
      bind[AgentOnlyAuthAction].to[FakeAgentOnlyAuthAction]
    )
    .build()

  "AgentSubscriptionController" - {

    val responseDetailString: String =
      """
        |{
        |"subscriptionID": "111111111",
        |"tradingName": "",
        |"isGBUser": true,
        |"primaryContact": [
        |{
        |"email": "",
        |"phone": "",
        |"mobile": "",
        |"organisation": {
        |"organisationName": "orgName"
        |}
        |}
        |],
        |"secondaryContact": [
        |{
        |"email": "",
        |"organisation": {
        |"organisationName": ""
        |}
        |}
        |]
        |}""".stripMargin

    val responseDetail = Json.parse(responseDetailString)

    val requestDetailJson = Json.parse("""
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

    "createSubscription" - {
      "should return BadRequest when subscriptionForCBCRequest ia invalid" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              BadRequest(Json.obj())
            )
          )

        val request =
          FakeRequest(
            POST,
            routes.AgentSubscriptionController.createSubscription.url
          )
            .withJsonBody(Json.parse("""{"value": "field"}"""))

        val result = route(application, request).value
        status(result) mustEqual BAD_REQUEST
      }

      "should return BadRequest when one is encountered" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              BadRequest(Json.obj())
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual BAD_REQUEST
        }
      }

      "should return Forbidden when authorisation is invalid" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              Forbidden(Json.obj())
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual FORBIDDEN
        }
      }

      "should return ServiceUnavailable when EIS becomes unavailable" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              ServiceUnavailable(Json.obj())
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }

      "should return InternalServerError when EIS fails" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              InternalServerError(Json.obj())
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual INTERNAL_SERVER_ERROR
        }
      }

      "should return Conflict when one occurs" in {
        val errorDetails = ErrorDetails(
          ErrorDetail(
            DateTime.now().toString,
            Some("xx"),
            "409",
            "CONFLICT",
            "",
            Some(SourceFaultDetail(Seq("a", "b")))
          )
        )
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              Conflict(Json.toJson(errorDetails))
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual CONFLICT
        }
      }

      "should return NotFound for unspecified errors" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              NotFound(Json.obj())
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual NOT_FOUND
        }
      }

      "must return ServiceUnavailable when downstream errors occur" in {
        when(
          mockAgentSubscriptionService
            .createContactInformation(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              ServiceUnavailable(Json.obj())
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionRequest]) { subscriptionForCBCRequest =>
          val request =
            FakeRequest(
              POST,
              routes.AgentSubscriptionController.createSubscription.url
            )
              .withJsonBody(Json.toJson(subscriptionForCBCRequest))

          val result = route(application, request).value
          status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }
    }

    "ReadSubscription" - {

      "should return OK when getContactInformation is valid" in {
        when(mockAgentSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Right(responseDetail.as[AgentResponseDetail])))

        val request = FakeRequest(POST, routes.AgentSubscriptionController.readSubscription.url)

        val result = route(application, request).value
        status(result) mustEqual OK

      }

      "should return InternalServerError when getContactInformation fails" in {
        when(mockAgentSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Left(ReadSubscriptionError(500))))

        val request =
          FakeRequest(
            POST,
            routes.AgentSubscriptionController.readSubscription.url
          )

        val result = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }

    "UpdateSubscription" - {

      "should return OK when updateContactInformation was successful" in {
        when(mockAgentSubscriptionService.updateContactInformation(any[AgentSubscriptionEtmpRequest])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Right(())))

        val request =
          FakeRequest(
            POST,
            routes.AgentSubscriptionController.updateSubscription.url
          ).withJsonBody(requestDetailJson)

        val result = route(application, request).value
        status(result) mustEqual OK

      }

      "should return InternalServerError when updateContactInformation fails" in {
        when(mockAgentSubscriptionService.updateContactInformation(any[AgentSubscriptionEtmpRequest])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(Future.successful(Left(UpdateSubscriptionError(500))))

        val request =
          FakeRequest(
            POST,
            routes.AgentSubscriptionController.updateSubscription.url
          ).withJsonBody(requestDetailJson)

        val result = route(application, request).value
        status(result) mustEqual INTERNAL_SERVER_ERROR

      }
    }
  }
}
