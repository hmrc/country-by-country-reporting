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

package controllers.auth

import org.apache.pekko.util.Timeout
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{~, Retrieval}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class IdentifierAuthActionSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  type RetrievalType = Enrolments ~ Option[AffinityGroup]

  class Harness(authAction: IdentifierAuthAction) extends InjectedController {

    def onPageLoad(): Action[AnyContent] = authAction { _ =>
      Ok
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  implicit val timeout: Timeout = 5 seconds

  val application: Application = new GuiceApplicationBuilder()
    .configure(
      Configuration("metrics.enabled"              -> "false",
                    "enrolmentKeys.cbc.key"        -> "HMRC-CBC-ORG",
                    "enrolmentKeys.cbc.identifier" -> "cbcId",
                    "auditing.enabled"             -> false
      )
    )
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector)
    )
    .build()

  "Identifier Auth Action" when {
    "the user is not logged in" must {
      "must return unauthorised" in {
        when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[_]]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.failed(new MissingBearerToken))

        val authAction = application.injector.instanceOf[IdentifierAuthAction]
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe UNAUTHORIZED

      }
      "must return UNAUTHORIZED for not known enrolment" in {
        val retrieval: RetrievalType =
          new ~(Enrolments(Set(Enrolment("HMRC-TEST-ORG", Seq(EnrolmentIdentifier("TESTID", "subscriptionID")), "ACTIVE"))), Some(Organisation))
        when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[RetrievalType]]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(retrieval))

        val authAction = application.injector.instanceOf[IdentifierAuthAction]
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe UNAUTHORIZED
      }
    }

    "the user is logged in" must {
      "for an Organisation must return the request" in {
        val retrieval: RetrievalType =
          new ~(Enrolments(Set(Enrolment("HMRC-CBC-ORG", Seq(EnrolmentIdentifier("cbcId", "subscriptionID")), "ACTIVE"))), Some(Organisation))
        when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[RetrievalType]]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(retrieval))

        val authAction = application.injector.instanceOf[IdentifierAuthAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe OK
      }

      "for a Non UK Organisation must return the request" in {
        val retrieval: RetrievalType =
          new ~(Enrolments(Set(Enrolment("HMRC-CBC-NONUK-ORG", Seq(EnrolmentIdentifier("cbcId", "subscriptionID")), "ACTIVE"))), Some(Organisation))
        when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[RetrievalType]]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(retrieval))

        val authAction = application.injector.instanceOf[IdentifierAuthAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe OK
      }

      "for an Agent must return the request" in {
        val retrieval: RetrievalType =
          new ~(Enrolments(Set(Enrolment("HMRC-AS-AGENT", Seq(EnrolmentIdentifier("AgentReferenceNumber", "arn")), "ACTIVE"))), Some(Agent))
        when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[RetrievalType]]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.successful(retrieval))

        val authAction = application.injector.instanceOf[IdentifierAuthAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe OK
      }
    }
  }
}
