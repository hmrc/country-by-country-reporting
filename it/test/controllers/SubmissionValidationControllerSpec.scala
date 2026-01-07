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
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.{doAnswer, spy, when}
import org.scalatest.wordspec.AnyWordSpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BodyParsers, Request, Result}
import play.api.test.*
import play.api.test.Helpers.*
import play.api.{inject, Application}
import services.validation.XMLValidationService
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector}
import uk.gov.hmrc.http.HttpVerbs.POST

import scala.concurrent.Future
import scala.xml.XML

class SubmissionValidationControllerSpec extends ISpecBase {

  private def authorisedApp(): Application = {

    val mockAppConfig = mock[AppConfig]

    when(mockAppConfig.enrolmentKey).thenReturn((input: String) =>
      input match {
        case "cbc"      => "HMRC-CBC-ORG"
        case "cbcNonUK" => "HMRC-CBC-NONUK-ORG"
        case _          => "HMRC-AS-AGENT"
      }
    )
    when(mockAppConfig.fileUploadXSDFilePath).thenReturn("/xsd/CbcXML_v2.0.xsd")

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

    val mockXmlValidationService = mock[XMLValidationService]

    when(
      mockXmlValidationService.validate(anyString(), anyString())
    ).thenReturn(Right(XML.loadFile("test/resources/cbc/fileUpload/validcbc.xml")))

    new GuiceApplicationBuilder()
      .overrides(
        inject.bind[IdentifierAuthAction].toInstance(authAction),
        inject.bind[AppConfig].toInstance(mockAppConfig),
        inject.bind[XMLValidationService].toInstance(mockXmlValidationService)
      )
      .configure(
        "auditing.enabled" -> "false"
      )
      .build()
  }

  "POST /validate-submission" should {

    "reject unauthenticated requests" in {
      val request =
        FakeRequest(POST, "/country-by-country-reporting/validate-submission")
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

      val upscanUrl       = "/some-upscan-url"
      val conversationId  = "conversationId123"
      val subscriptionId  = "subscriptionId123"
      val fileReferenceId = "fileReferenceId123"
      val validateRequestJsonBody =
        Json.obj("url" -> upscanUrl, "conversationId" -> conversationId, "subscriptionId" -> subscriptionId, "fileReferenceId" -> fileReferenceId)

      val request =
        FakeRequest(POST, "/country-by-country-reporting/validate-submission")
          .withHeaders(CONTENT_TYPE -> "application/json")
          .withBody(validateRequestJsonBody)

      val app = authorisedApp()

      running(app) {
        val result =
          route(app, request).getOrElse(fail("Route not defined"))

        status(result) shouldBe OK
      }
    }
  }

}
