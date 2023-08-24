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

import connectors.AgentSubscriptionConnector
import models.agentSubscription._
import models.error._
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Json, Reads}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}


class AgentSubscriptionService @Inject()(agentSubscriptionConnector: AgentSubscriptionConnector) extends Logging {

  def createContactInformation(subscriptionRequest: CreateAgentSubscriptionEtmpRequest)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] = {

    agentSubscriptionConnector.createSubscription(subscriptionRequest).map { response =>

      val responseBody = response.body
      response.status match {
        case CREATED => Ok(responseBody)
        case NOT_FOUND => NotFound(responseBody)
        case UNPROCESSABLE_ENTITY =>
          logDownStreamError[BusinessValidationError](responseBody)
          UnprocessableEntity(responseBody)

        case INTERNAL_SERVER_ERROR =>
          logDownStreamError[BackendSAPSystemError](responseBody)
          InternalServerError(responseBody)

        case BAD_REQUEST =>
          logDownStreamError[ErrorDetails](responseBody)
          BadRequest(responseBody)

        case FORBIDDEN =>
          logDownStreamError[ErrorDetails](responseBody)
          Forbidden(responseBody)

        case SERVICE_UNAVAILABLE =>
          logDownStreamError[ErrorDetails](responseBody)
          ServiceUnavailable(responseBody)

        case CONFLICT =>
          logDownStreamError[ErrorDetails](responseBody)
          Conflict(responseBody)

        case _ =>
          logDownStreamError[ErrorDetails](responseBody)
          InternalServerError(responseBody)
      }
    }

  }

  def getContactInformation(agentRefNo: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Either[ReadSubscriptionError, AgentResponseDetail]] = {

    val subscriptionRequest: DisplayAgentSubscriptionForCBCRequest =
      DisplayAgentSubscriptionForCBCRequest(
        DisplayAgentSubscriptionDetails(
          AgentRequestCommonForSubscription(),
          AgentReadSubscriptionRequestDetail(agentRefNo)
        )
      )

    agentSubscriptionConnector.readSubscription(subscriptionRequest).map { response =>
      response.status match {
        case OK =>
          val agentResponseDetail = response.json.as[DisplayAgentSubscriptionForCBCResponse].displayAgentSubscriptionForCBCResponse.responseDetail
          Right(agentResponseDetail)
        case status =>
          logger.warn(s"Read Agent subscription Got Status $status")
          Left(ReadSubscriptionError(status))
      }
    }
  }

  def updateContactInformation(agentRequestDetailForUpdate: AgentRequestDetailForUpdate)
                        (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Either[UpdateSubscriptionError, Unit]] =
    agentSubscriptionConnector.updateSubscription(UpdateAgentSubscriptionForCBCRequest(agentRequestDetailForUpdate)).map { res =>
      res.status match {
        case OK => Right(())
        case status =>
          logger.warn(s"Update Agent Subscription Got Status $status")
          Left(UpdateSubscriptionError(status))
      }
    }

  private def logDownStreamError[T <: DownStreamError](body: String)(implicit reads: Reads[T]): Unit = {
    val error = Try(Json.parse(body).validate[T])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.warn(s"Error with submission: ${value.detail}")
      case _ =>
        logger.warn("Error with submission but return is not a valid json")
    }
  }
}
