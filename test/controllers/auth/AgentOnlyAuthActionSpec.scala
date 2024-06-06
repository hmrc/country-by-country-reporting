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
import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Action, AnyContent, InjectedController}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import play.api.{Application, Configuration}
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.{~, Retrieval}
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthConnector, Enrolment, Enrolments, MissingBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

class AgentOnlyAuthActionSpec extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {

  class Harness(authAction: AgentOnlyAuthAction) extends InjectedController {

    def onPageLoad(): Action[AnyContent] = authAction { _ =>
      Ok
    }
  }

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  implicit val timeout: Timeout = 5 seconds

  val application: Application = new GuiceApplicationBuilder()
    .configure(Configuration("metrics.enabled" -> "false", "auditing.enabled" -> "false"))
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector)
    )
    .build()

  "Agent Only Auth Action" when {
    "the user is not logged in" must {
      "must return unauthorised" in {
        when(mockAuthConnector.authorise(any[Predicate](), any[Retrieval[_]]())(any[HeaderCarrier](), any[ExecutionContext]()))
          .thenReturn(Future.failed(new MissingBearerToken))

        val authAction = application.injector.instanceOf[AgentOnlyAuthAction]
        val controller = new Harness(authAction)
        val result     = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe UNAUTHORIZED

      }
    }

    "the user is logged in" must {
      "must return the request" in {

        type RetrievalType = Enrolments ~ Option[AffinityGroup]

        val authRetrievals: RetrievalType = new ~(
          Enrolments(
            Set(
              Enrolment("HMRC-AS-AGENT").withIdentifier("AgentReferenceNumber", "arn123")
            )
          ),
          Some(AffinityGroup.Agent)
        )

        when(mockAuthConnector.authorise(any(), any[Retrieval[RetrievalType]])(any(), any()))
          .thenReturn(Future.successful(authRetrievals))

        val authAction = application.injector.instanceOf[AgentOnlyAuthAction]
        val controller = new Harness(authAction)

        val result = controller.onPageLoad()(FakeRequest("", ""))
        status(result) mustBe OK
      }
    }
  }
}
