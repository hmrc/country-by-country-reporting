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
import controllers.auth.{IdentifierAuthAction, IdentifierAuthActionImpl, IdentifierRequest}
import models.submission.{ConversationId, FileDetails, Pending, TestData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{doAnswer, spy, when}
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, BodyParsers, Request, Result}
import play.api.test.*
import play.api.test.Helpers.*
import play.api.{inject, Application}
import repositories.submission.FileDetailsRepository
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.Future

class FileDetailsControllerSpec extends ISpecBase {

  private def authorisedApp(): Application = {

    val dateNow     = LocalDate.now()
    val dateTimeNow = LocalDateTime.now()
    val fileDetails: FileDetails = FileDetails(
      ConversationId("conversationId123456"),
      "subscriptionId",
      "messageRefId",
      "Reporting Entity",
      TestData,
      Pending,
      "file1.xml",
      dateTimeNow,
      dateTimeNow,
      None,
      Some(Organisation),
      None,
      dateNow,
      dateNow
    )

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

      block(IdentifierRequest(request, Organisation))
    }.when(authAction).invokeBlock(any(), any())

    val mockFileDetailsRepository = mock[FileDetailsRepository]

    val conversationId = ConversationId("testid")
    when(mockFileDetailsRepository.findByConversationId(conversationId)).thenReturn(Future.successful(Some(fileDetails)))
    when(mockFileDetailsRepository.findBySubscriptionId("testsubscriptionId")).thenReturn(Future.successful(Seq(fileDetails)))
    when(mockFileDetailsRepository.findStatusByConversationId(conversationId)).thenReturn(Future.successful(Some(Pending)))

    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[IdentifierAuthAction].toInstance(authAction),
        inject.bind[AppConfig].toInstance(mockAppConfig),
        inject.bind[FileDetailsRepository].toInstance(mockFileDetailsRepository)
      )
      .configure(
        "auditing.enabled" -> "false"
      )
      .build()
  }

  "GET /files/:conversationId/details" should {

    "reject unauthenticated requests" in {
      val request = FakeRequest(GET, "/country-by-country-reporting/files/testid/details")

      val app = unauthorisedApp()

      running(app) {
        val result = route(app, request).getOrElse {
          fail("Route not defined.")
        }

        status(result) should (be(UNAUTHORIZED) or be(FORBIDDEN))
      }
    }

    "return 200 OK when authorised request" in {

      val request = FakeRequest(GET, "/country-by-country-reporting/files/testid/details")

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }
  "GET /files/details/:subscriptionId" should {

    "reject unauthenticated requests" in {
      val request = FakeRequest(GET, "/country-by-country-reporting/files/details/testsubscriptionId")

      val app = unauthorisedApp()

      running(app) {
        val result = route(app, request).getOrElse {
          fail("Route not defined.")
        }

        status(result) should (be(UNAUTHORIZED) or be(FORBIDDEN))
      }
    }

    "return 200 OK when authorised request" in {

      val request = FakeRequest(GET, "/country-by-country-reporting/files/details/testsubscriptionId")

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }
  "GET /files/:conversationId/status" should {

    "reject unauthenticated requests" in {
      val request = FakeRequest(GET, "/country-by-country-reporting/files/testid/status")

      val app = unauthorisedApp()

      running(app) {
        val result = route(app, request).getOrElse {
          fail("Route not defined.")
        }

        status(result) should (be(UNAUTHORIZED) or be(FORBIDDEN))
      }
    }

    "return 200 OK when authorised request" in {

      val request = FakeRequest(GET, "/country-by-country-reporting/files/testid/status")

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }

}
