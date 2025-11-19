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

import org.apache.pekko.http.javadsl.model.DateTime
import base.SpecBase
import connectors.AgentSubscriptionConnector
import generators.Generators
import models.agentSubscription.{AgentResponseDetail, _}
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
import AgentSubscriptionEtmpRequest._
import models.subscription.{ContactInformation, OrganisationDetails}
import org.mockito.ArgumentMatchers.{eq => eqTo}
import org.scalatest.prop.TableDrivenPropertyChecks

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionRequestServiceSpec
    extends SpecBase
    with BeforeAndAfterEach
    with Generators
    with ScalaCheckPropertyChecks
    with TableDrivenPropertyChecks {

  override def beforeEach(): Unit = reset(mockAgentSubscriptionConnector)

  val mockAgentSubscriptionConnector = mock[AgentSubscriptionConnector]

  "AgentSubscriptionService" - {
    val application = applicationBuilder()
      .overrides(
        bind[AgentSubscriptionConnector].toInstance(mockAgentSubscriptionConnector)
      )
      .build()

    val service = application.injector.instanceOf[AgentSubscriptionService]
    val agentSubscriptionRequestJson = Json.parse(
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

    "createSubscription" - {

      val createAgentSubscriptionRequest = agentSubscriptionRequestJson.as[AgentSubscriptionEtmpRequest]

      "should return BAD_REQUEST when subscriptionForCBCRequest ia invalid" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
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

      "should return NOT_FOUND when EIS request returns 404 status" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(NOT_FOUND, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual NOT_FOUND
        }
      }

      "should return FORBIDDEN when authorisation is invalid" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(FORBIDDEN, Json.obj(), Map.empty[String, Seq[String]])
            )
          )

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual FORBIDDEN
        }
      }

      "should return SERVICE_UNAVAILABLE when EIS becomes unavailable" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
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

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }

      "should return BAD_GATEWAY when EIS request fails with 502 status" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
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

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual BAD_GATEWAY
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
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                Json.toJson(errorDetails),
                Map.empty[String, Seq[String]]
              )
            )
          )

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual CONFLICT
        }
      }

      "should return UNAUTHORIZED when EIS fails with 401 status" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
            )(
              any[HeaderCarrier](),
              any[ExecutionContext]()
            )
        )
          .thenReturn(
            Future.successful(
              HttpResponse(
                UNAUTHORIZED,
                Json.obj(),
                Map.empty[String, Seq[String]]
              )
            )
          )

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual UNAUTHORIZED
        }
      }

      "downstream errors should be recoverable when not in json" in {
        when(
          mockAgentSubscriptionConnector
            .createSubscription(
              any[AgentSubscriptionEtmpRequest]()
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

        forAll(arbitrary[AgentSubscriptionEtmpRequest]) { subscriptionForCBCRequest =>
          val result = service.createContactInformation(subscriptionForCBCRequest)
          status(result) mustEqual SERVICE_UNAVAILABLE
        }
      }
    }

    "readSubscription" - {

      val agentRefNo = "111111111"

      "must correctly retrieve subscription from connector" in {

        val subscriptionResponseJson: String =
          s"""{
             |  "success": {
             |    "processingDate": "2023-05-17T09:26:17Z",
             |    "agent": {
             |      "arn": "$agentRefNo",
             |      "tradingName": "Agent Ltd",
             |      "gbUser": true,
             |      "primaryContact": {
             |        "organisation": {
             |          "name": "Cbc agent"
             |        },
             |        "email": "AgentFirstContactEmail@cbc.com",
             |        "phone": "0123456789",
             |        "mobile": "07123456789"
             |      },
             |      "secondaryContact": {
             |        "organisation": {
             |          "name": "Cbc agent 2"
             |        },
             |        "email": "AgentSecondContactEmail@cbc.com",
             |        "phone": "0123456000",
             |        "mobile": "07123456000"
             |      }
             |    }
             |  }
             |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Right(
            AgentResponseDetail(
              agentRefNo,
              Option("Agent Ltd"),
              isGBUser = true,
              primaryContact = ContactInformation(
                organisationDetails = OrganisationDetails("Cbc agent"),
                email = "AgentFirstContactEmail@cbc.com",
                phone = Option("0123456789"),
                mobile = Option("07123456789")
              ),
              secondaryContact = Option(
                ContactInformation(
                  organisationDetails = OrganisationDetails("Cbc agent 2"),
                  email = "AgentSecondContactEmail@cbc.com",
                  phone = Option("0123456000"),
                  mobile = Option("07123456000")
                )
              )
            )
          )

          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must return a ReadSubscriptionError(NOT_FOUND) when when a 422 response with a 202 error code is received from connector" in {

        val subscriptionResponseJson: String =
          s"""{"errorDetail":{
             |  "errorCode":"202",
             |  "errorMessage":"No Subscription Data Found",
             |  "source":"ETMP",
             |  "sourceFaultDetail":{
             |    "detail":["No Subscription Data Found"]
             |    },
             |    "timestamp":"2023-10-10T11:53:48.263Z",
             |    "correlationId":"db052924-b1b0-444e-8c74-41d9026abccb"
             |  }
             |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Left(ReadSubscriptionError(NOT_FOUND))
          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must return a ReadSubscriptionError(UNPROCESSABLE_ENTITY) when when a 422 response with a any other error code is received from connector" in {

        val subscriptionResponseJson: String =
          s"""{"errorDetail":{
             |  "errorCode":"003",
             |  "errorMessage":"Request could not be processed",
             |  "source":"ETMP",
             |  "sourceFaultDetail":{
             |    "detail":["Request could not be processed"]
             |    },
             |    "timestamp":"2023-10-10T11:53:48.263Z",
             |    "correlationId":"db052924-b1b0-444e-8c74-41d9026abccb"
             |  }
             |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(UNPROCESSABLE_ENTITY, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Left(ReadSubscriptionError(UNPROCESSABLE_ENTITY))
          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must return ReadSubscriptionError from connector when not ok status" in {
        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Left(ReadSubscriptionError(INTERNAL_SERVER_ERROR))
          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must convert primary Individual details to primary Organisation details when only the primary Individual detail is found" in {
        val subscriptionResponseJson: String =
          s"""{
             |  "success": {
             |    "processingDate": "2023-05-17T09:26:17Z",
             |    "agent": {
             |      "arn": "$agentRefNo",
             |      "tradingName": "Agent Ltd",
             |      "gbUser": true,
             |      "primaryContact": {
             |        "individual": {
             |          "firstName": "Joe",
             |          "lastName": "Bloggs"
             |        },
             |        "email": "AgentFirstContactEmail@cbc.com",
             |        "phone": "0123456789",
             |        "mobile": "07123456789"
             |      },
             |      "secondaryContact": {
             |        "individual": {
             |          "firstName": "Alice",
             |          "lastName": "Wonderland"
             |        },
             |        "email": "AgentSecondContactEmail@cbc.com",
             |        "phone": "0123456000",
             |        "mobile": "07123456000"
             |      }
             |    }
             |  }
             |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Right(
            AgentResponseDetail(
              agentRefNo,
              Option("Agent Ltd"),
              isGBUser = true,
              primaryContact = ContactInformation(
                organisationDetails = OrganisationDetails("Joe Bloggs"),
                email = "AgentFirstContactEmail@cbc.com",
                phone = Option("0123456789"),
                mobile = Option("07123456789")
              ),
              secondaryContact = Option(
                ContactInformation(
                  organisationDetails = OrganisationDetails("Alice Wonderland"),
                  email = "AgentSecondContactEmail@cbc.com",
                  phone = Option("0123456000"),
                  mobile = Option("07123456000")
                )
              )
            )
          )

          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must convert secondary Individual details to secondary Organisation details when only the secondary Individual detail is found" in {
        val subscriptionResponseJson: String =
          s"""{
             |  "success": {
             |    "processingDate": "2023-05-17T09:26:17Z",
             |    "agent": {
             |      "arn": "$agentRefNo",
             |      "tradingName": "Agent Ltd",
             |      "gbUser": true,
             |      "primaryContact": {
             |        "individual": {
             |          "firstName": "Joe",
             |          "lastName": "Bloggs"
             |        },
             |        "email": "AgentFirstContactEmail@cbc.com",
             |        "phone": "0123456789",
             |        "mobile": "07123456789"
             |      }
             |    }
             |  }
             |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Right(
            AgentResponseDetail(
              agentRefNo,
              Option("Agent Ltd"),
              isGBUser = true,
              primaryContact = ContactInformation(
                organisationDetails = OrganisationDetails("Joe Bloggs"),
                email = "AgentFirstContactEmail@cbc.com",
                phone = Option("0123456789"),
                mobile = Option("07123456789")
              ),
              secondaryContact = None
            )
          )

          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must return ReadSubscriptionError when there is no contact found in response" in {
        val subscriptionResponseJson: String =
          s"""{
             |  "success": {
             |    "processingDate": "2023-05-17T09:26:17Z",
             |    "agent": {
             |      "arn": "$agentRefNo",
             |      "tradingName": "Agent Ltd",
             |      "gbUser": true
             |    }
             |  }
             |}""".stripMargin

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Left(ReadSubscriptionError(UNPROCESSABLE_ENTITY))

          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }

      "must return ReadSubscriptionError when unable tp parse response" in {
        val subscriptionResponseJson: String = """{ "key": "invalid response" }"""

        when(mockAgentSubscriptionConnector.readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

        val result = service.getContactInformation(agentRefNo)

        whenReady(result) { sub =>
          sub mustBe Left(ReadSubscriptionError(UNPROCESSABLE_ENTITY))

          verify(mockAgentSubscriptionConnector, times(1)).readSubscription(eqTo(agentRefNo))(any[HeaderCarrier](), any[ExecutionContext]())
        }
      }
    }

    "UpdateSubscription" - {
      val updateAgentSubscriptionRequest = agentSubscriptionRequestJson.as[AgentSubscriptionEtmpRequest]

      "must  return UpdateSubscription with OK status when connector response with ok status" in {
        when(mockAgentSubscriptionConnector.updateSubscription(any[AgentSubscriptionEtmpRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(HttpResponse(OK, "Good Response")))

        val result = service.updateContactInformation(updateAgentSubscriptionRequest)

        whenReady(result) { sub =>
          verify(mockAgentSubscriptionConnector, times(1)).updateSubscription(any[AgentSubscriptionEtmpRequest]())(any[HeaderCarrier](),
                                                                                                                   any[ExecutionContext]()
          )
          sub mustBe Right(())
        }
      }

      val scenarios = Table(
        ("expectedStatusCode", "description"),
        (NOT_FOUND, "NOT_FOUND"),
        (UNAUTHORIZED, "UNAUTHORIZED"),
        (BAD_GATEWAY, "BAD_GATEWAY"),
        (UNPROCESSABLE_ENTITY, "UNPROCESSABLE_ENTITY"),
        (INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR"),
        (BAD_REQUEST, "BAD_REQUEST"),
        (FORBIDDEN, "FORBIDDEN"),
        (SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE"),
        (CONFLICT, "CONFLICT")
      )

      forAll(scenarios) { (expectedStatusCode, description) =>
        s"must return UpdateSubscriptionError when connector responds with $description" in {
          when(mockAgentSubscriptionConnector.updateSubscription(any[AgentSubscriptionEtmpRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
            .thenReturn(Future.successful(HttpResponse(expectedStatusCode, "")))

          val result = service.updateContactInformation(updateAgentSubscriptionRequest)

          whenReady(result) { sub =>
            verify(mockAgentSubscriptionConnector, times(1)).updateSubscription(any[AgentSubscriptionEtmpRequest]())(any[HeaderCarrier](),
                                                                                                                     any[ExecutionContext]()
            )
            sub mustBe Left(UpdateSubscriptionError(expectedStatusCode))
          }
        }
      }
    }
  }

}
