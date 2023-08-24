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

package services

import akka.http.javadsl.model.DateTime
import base.SpecBase
import connectors.AgentSubscriptionConnector
import generators.Generators
import models.agentSubscription._
import models.error._
import org.mockito.ArgumentMatchers.any
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.http.Status._
import play.api.inject.bind
import play.api.libs.json.Json
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import CreateAgentSubscriptionEtmpRequest._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionRequestServiceSpec extends SpecBase with BeforeAndAfterEach with Generators with ScalaCheckPropertyChecks {

  override def beforeEach(): Unit = reset(mockAgentSubscriptionConnector)

  val mockAgentSubscriptionConnector = mock[AgentSubscriptionConnector]


  "AgentSubscriptionService" - {
    val application = applicationBuilder()
      .overrides(
        bind[AgentSubscriptionConnector].toInstance(mockAgentSubscriptionConnector)
      )
      .build()

    val service = application.injector.instanceOf[AgentSubscriptionService]

    val requestDetailJson = Json.parse(
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
    val requestDetailForUpdate = requestDetailJson.as[AgentRequestDetailForUpdate]

      
    "createSubscription" - {
      val createAgentSubscriptionJson = Json.parse(
      """
        |{
        |  "idType" : "ARN",
        |  "idNumber" : "JARN0000158",
        |  "tradingName" : "tshfhygzjqdlvuk",
        |  "gbUser" : false,
        |  "primaryContact" : {
        |    "individual" : {
        |      "firstName" : "pamio",
        |      "middleName" : "lxtt",
        |      "lastName" : "hcuamjmixp"
        |    },
        |    "email" : "mj@gmailqqq.com",
        |    "phone" : "(+351) 282 43 50 50",
        |    "mobile" : "555-8909"
        |  },
        |  "secondaryContact" : {
        |    "individual" : {
        |      "firstName" : "pwpesonhpntyq",
        |      "middleName" : "bp",
        |      "lastName" : "hieieiy"
        |    },
        |    "email" : "djwkxescl@gmail.com",
        |    "phone" : "1-234 567.89/01 ext.1234",
        |    "mobile" : "(0055)(123)8575973"
        |  }
        |}
        |""".stripMargin
      )

      val createAgentSubscriptionRequest = createAgentSubscriptionJson.as[CreateAgentSubscriptionEtmpRequest]

      "should return BAD_REQUEST when subscriptionForCBCRequest ia invalid" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(400, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        val result = service.createContactInformation(createAgentSubscriptionRequest)
        status(result) mustEqual BAD_REQUEST
      }

      "should return BAD_REQUEST when one is encountered" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(400, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual BAD_REQUEST
        }
      }

      "should return FORBIDDEN when authorisation is invalid" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(403, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual FORBIDDEN
        }
      }

      "should return SERVICE_UNAVAILABLE when EIS becomes unavailable" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(503, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }

      "should return INTERNAL_SERVER_ERROR when EIS fails" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_GATEWAY,
                Json.obj(),
                Map.empty[String, Seq[String]]
              )
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual INTERNAL_SERVER_ERROR
        }
      }

      "should return CONFLICT when one occurs" in {
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
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                409,
                Json.toJson(errorDetails),
                Map.empty[String, Seq[String]]
              )
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual CONFLICT
        }
      }

      "should return NOT_FOUND for unspecified errors" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(404, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual NOT_FOUND
        }
      }

      "downstream errors should be recoverable when not in json" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[CreateAgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(503, "Not Available", Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[CreateAgentSubscriptionEtmpRequest]) {
          subscriptionForCBCRequest =>
            val result = service.createContactInformation(subscriptionForCBCRequest)
            status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }
    }
    "readSubscription" - {

      "must correctly retrieve subscription from connector" in {

        val subscriptionResponseJson: String =
          """
            |{
            |"displayAgentSubscriptionForCBCResponse": {
            |"responseCommon": {
            |"status": "OK",
            |"processingDate": "2020-08-09T11:23:45Z"
            |},
            |"responseDetail": {
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
            |}
            |}
            |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(any[DisplayAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

        val result = service.getContactInformation("111111111")

        whenReady(result) { _ =>
          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(any[DisplayAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        }
      }

      "must retrieve ReadSubscriptionError from connector when not ok status" in {
        when(mockAgentSubscriptionConnector.readSubscription(any[DisplayAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val result = service.getContactInformation("111111111")

        whenReady(result) { sub =>
          sub mustBe Left(ReadSubscriptionError(500))
          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(any[DisplayAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](),
            any[ExecutionContext]()
          )
        }
      }
    }

    "UpdateSubscription" - {
      "must  return UpdateSubscription with OK status when connector response with ok status" in {
        when(mockAgentSubscriptionConnector.updateSubscription(any[UpdateAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, "Good Response")))

        val result = service.updateContactInformation(requestDetailForUpdate)

        whenReady(result) { sub =>
          verify(mockAgentSubscriptionConnector, times(1)).updateSubscription(any[UpdateAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
          sub mustBe Right(())
        }
      }

      "must have UpdateSubscriptionError when connector response with not ok status" in {
        when(mockAgentSubscriptionConnector.updateSubscription(any[UpdateAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val result = service.updateContactInformation(requestDetailForUpdate)

        whenReady(result) { sub =>
          verify(mockAgentSubscriptionConnector, times(1)).updateSubscription(any[UpdateAgentSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
          sub mustBe Left(UpdateSubscriptionError(500))
        }
      }
    }
  }

}
