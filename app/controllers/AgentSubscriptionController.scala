/*
 * Copyright 2022 HM Revenue & Customs
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

import controllers.auth.AgentOnlyAuthAction
import models.agentSubscription.{AgentRequestDetailForUpdate, CreateAgentSubscriptionRequest}
import models.error.{ReadSubscriptionError, UpdateSubscriptionError}
import play.api.Logging
import play.api.libs.json.{JsResult, JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.AgentSubscriptionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionController @Inject()(
  authenticate: AgentOnlyAuthAction,
  cc: ControllerComponents,
  agentSubscriptionService: AgentSubscriptionService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createSubscription(): Action[JsValue] = authenticate(parse.json).async { implicit request =>
    val subscriptionSubmissionResult
    : JsResult[CreateAgentSubscriptionRequest] =
      request.body.validate[CreateAgentSubscriptionRequest]

    subscriptionSubmissionResult.fold(
      _ =>
        Future.successful(
          BadRequest("CreateAgentSubscriptionRequest is invalid")
        ),
      arn =>
        agentSubscriptionService.createContactInformation(arn)
    )
  }

  def readSubscription(): Action[AnyContent] = authenticate.async { implicit request =>
    agentSubscriptionService.getContactInformation(request.arn).map {
      case Right(value) => Ok(Json.toJson(value))
      case Left(ReadSubscriptionError(value)) =>
        logger.warn(s"AgentReadSubscriptionError $value")
        InternalServerError(s"AgentReadSubscriptionError $value")
    }
  }

  def updateSubscription(): Action[JsValue] = authenticate.async(parse.json) { implicit request =>
    val updateSubscriptionResult: JsResult[AgentRequestDetailForUpdate] =
      request.body.validate[AgentRequestDetailForUpdate]

    updateSubscriptionResult.fold(
      invalid =>
        Future.successful {
          logger.warn(s"Agent updateContactInformation Json Validation Failed: $invalid")
          InternalServerError("Json Validation Failed")
        },
      validReq =>
        agentSubscriptionService.updateContactInformation(validReq).map {
          case Right(_) => Ok
          case Left(UpdateSubscriptionError(value)) =>
            logger.warn(s"Agent updateContactInformation $value")
            InternalServerError(s"Agent updateContactInformation $value")
        }
    )

  }

}
