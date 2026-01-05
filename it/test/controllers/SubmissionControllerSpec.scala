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
import connectors.{SDESConnector, SubscriptionConnector}
import controllers.auth.{IdentifierAuthAction, IdentifierAuthActionImpl, IdentifierRequest}
import models.submission.{CBC402, MessageSpecData, NewInformation, SubmissionDetails}
import models.upscan.UploadId
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, spy, when}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParsers, Request, Result}
import play.api.test.*
import play.api.test.Helpers.*
import play.api.{Application, inject}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpVerbs.POST

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SubmissionControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher

  private def unauthorisedApp(): Application = new GuiceApplicationBuilder().build()

  private def authorisedApp(): Application = {

//    val readSubscriptionResponse = DisplaySubscriptionForCBCResponse(
//      displaySubscriptionForCBCResponse = DisplaySubscriptionResponse(
//        responseCommon = ResponseCommon(status= "Pending",statusText = None, processingDate = LocalDate.now().toString, returnParameters = None),
//        responseDetail = ResponseDetail(
//          subscriptionID = "testsubscriptionId",
//          tradingName = None,
//          isGBUser = true,
//          primaryContact = ContactInformation(organisationDetails = OrganisationDetails("testOrg"),"test@test.com",None,None),
//          secondaryContact = None)
//      )
//    )

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
        case "cbc" => "HMRC-CBC-ORG"
        case "cbcNonUK" => "HMRC-CBC-NONUK-ORG"
        case _ => "HMRC-AS-AGENT"
      })
    when(mockAppConfig.maxLargeFileSizeBytes).thenReturn(104857600L)
    when(mockAppConfig.maxNormalFileSizeBytes).thenReturn(1233L)

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

    val mockSDESConnector = mock[SDESConnector]

    when(
      mockSDESConnector.sendFileNotification(any())(any(),any())
    ).thenReturn(
      Future.successful(Right(NO_CONTENT))
    )


    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[IdentifierAuthAction].toInstance(authAction),
        inject.bind[AppConfig].toInstance(mockAppConfig),
        inject.bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        inject.bind[SDESConnector].toInstance(mockSDESConnector)
      )
      .build()
  }

  "POST /submit" should {

    "reject unauthenticated requests" in {
      val request =
        FakeRequest(POST, "/country-by-country-reporting/submit")
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

      val submissionDetails = SubmissionDetails(
        fileName = "cbc-report.xml",
        uploadId = UploadId("7e67633b-596b-454d-b7b1-c85fe3fdf994"),
        enrolmentId = "sub-123",
        fileSize = 1234,
        documentUrl = "test",
        checksum = "test",
        messageSpecData = MessageSpecData("msg-456", CBC402, NewInformation, LocalDate.now(), LocalDate.now(), "GlobalCorp Ltd")
      )

      val jsonBody = Json.toJson(submissionDetails)

      val request =
        FakeRequest(POST, "/country-by-country-reporting/submit")
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
