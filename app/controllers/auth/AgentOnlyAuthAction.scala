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

import com.google.inject.ImplementedBy
import play.api.http.Status.UNAUTHORIZED
import play.api.mvc.Results.Status
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentOnlyAuthActionImpl @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AgentOnlyAuthAction
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AgentOnlyRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    authorised().retrieve(Retrievals.allEnrolments and Retrievals.affinityGroup) {
      case Enrolments(enrolments) ~ Some(Agent) =>
        val arn =
          (for {
            enrolment <- enrolments.find(_.key.equals("HMRC-AS-AGENT"))
            arn       <- enrolment.getIdentifier("AgentReferenceNumber")
          } yield arn.value)
            .getOrElse(throw new IllegalAccessException("Agent Reference Number Required"))
        block(AgentOnlyRequest(arn, request))
      case _ => Future.successful(Status(UNAUTHORIZED))
    } recover { case _: NoActiveSession =>
      Status(UNAUTHORIZED)
    }
  }
}

@ImplementedBy(classOf[AgentOnlyAuthActionImpl])
trait AgentOnlyAuthAction extends ActionBuilder[AgentOnlyRequest, AnyContent] with ActionFunction[Request, AgentOnlyRequest]
