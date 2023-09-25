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
import models.subscription.{ContactInformation, OrganisationDetails}
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsSuccess, Json, Reads}
import play.api.mvc.Result
import play.api.mvc.Results._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}
import cats.syntax.all._


class AgentSubscriptionService @Inject()(agentSubscriptionConnector: AgentSubscriptionConnector) extends Logging {

  def createContactInformation(subscriptionRequest: AgentSubscriptionEtmpRequest)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Result] = {

    agentSubscriptionConnector.createSubscription(subscriptionRequest).map { response =>

      val responseBody = response.body
      response.status match {
        case CREATED => Ok(responseBody)
        case _       => handleError(response, Create)
      }
    }

  }

  def getContactInformation(agentRefNo: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Either[ReadSubscriptionError, AgentResponseDetail]] =
    agentSubscriptionConnector.readSubscription(agentRefNo).map { response =>
      response.status match {
        case OK =>
          val validatedResponse = Try(Json.parse(response.body).validate[DisplayAgentSubscriptionResponse])
          validatedResponse match {
            case Success(JsSuccess(value, _)) =>
              convertEtmpDisplayAgentResponseToCbcResponse(value)
            case _ =>
              logger.warn("Failed to parse display agent subscription response json")
              Left(ReadSubscriptionError(UNPROCESSABLE_ENTITY))
          }
        case status =>
          logger.warn(s"Read Agent subscription Got Status $status")
          Left(ReadSubscriptionError(status))
      }
    }

  def updateContactInformation(agentRequestDetailForUpdate: AgentSubscriptionEtmpRequest)
                        (implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Either[UpdateSubscriptionError, Unit]] =
    agentSubscriptionConnector.updateSubscription(agentRequestDetailForUpdate).map { response =>
      response.status match {
        case OK     => Right(())
        case status =>
          handleError(response, Update)
          Left(UpdateSubscriptionError(status))
      }
    }

  private def handleError(errorResponse: HttpResponse, action: AgentSubscriptionAction): Result = {
    val errorResponseBody = errorResponse.body
    val errorStatus = errorResponse.status

    require(
      isServerError(errorStatus) || isClientError(errorStatus),
      s"Status code [$errorStatus] must be an error code"
    )

    errorStatus match {
      case NOT_FOUND =>
        logger.warn(s"[$action] submission returned not found: $errorResponseBody")
        NotFound(errorResponseBody)
      case UNAUTHORIZED =>
        logger.warn(s"[$action] submission returned unauthorised: $errorResponseBody")
        Unauthorized(errorResponseBody)
      case BAD_GATEWAY =>
        logger.warn(s"[$action] submission returned bad gateway: $errorResponseBody")
        BadGateway(errorResponseBody)
      case UNPROCESSABLE_ENTITY =>
        logDownStreamError[BusinessValidationError](errorResponse, action)
        UnprocessableEntity(errorResponseBody)
      case INTERNAL_SERVER_ERROR =>
        logDownStreamError[BackendSAPSystemError](errorResponse, action)
        InternalServerError(errorResponseBody)
      case BAD_REQUEST =>
        logDownStreamError[ErrorDetails](errorResponse, action)
        BadRequest(errorResponseBody)
      case FORBIDDEN =>
        logDownStreamError[ErrorDetails](errorResponse, action)
        Forbidden(errorResponseBody)
      case SERVICE_UNAVAILABLE =>
        logDownStreamError[ErrorDetails](errorResponse, action)
        ServiceUnavailable(errorResponseBody)
      case CONFLICT =>
        logDownStreamError[ErrorDetails](errorResponse, action)
        Conflict(errorResponseBody)
      case _ =>
        logDownStreamError[ErrorDetails](errorResponse, action)
        InternalServerError(errorResponseBody)
    }
  }

  private def logDownStreamError[T <: DownStreamError](
    response: HttpResponse,
    action: AgentSubscriptionAction
  )(implicit reads: Reads[T]): Unit = {
    val error = Try(Json.parse(response.body).validate[T])
    error match {
      case Success(JsSuccess(value, _)) =>
        logger.warn(s"Error with [$action] submission: ${value.detail}")
      case _ =>
        logger.warn(s"Failed to parse [$action] submission json")
    }
  }

  private def convertEtmpDisplayAgentResponseToCbcResponse(
    agentResponse: DisplayAgentSubscriptionResponse
  ): Either[ReadSubscriptionError, AgentResponseDetail] = {
    val agent = agentResponse.success.agent
    val primaryContact = resolveContacts(agent.primaryContact)
    val secondaryContact = agent.secondaryContact.map(resolveContacts)

    for {
      mainContact <- primaryContact
      otherContact <- secondaryContact.sequence
    } yield AgentResponseDetail(agent.arn, agent.tradingName, agent.gbUser, mainContact, otherContact)

  }

  private def resolveContacts(contact: Contact): Either[ReadSubscriptionError, ContactInformation] = {
    (contact.organisation, contact.individual) match {
      case (Some(org), _) =>
        Right(ContactInformation(OrganisationDetails(org.name), contact.email, contact.phone, contact.mobile))
      case (None, Some(individual)) =>
        val orgContactFromIndividualName = OrganisationDetails(s"${individual.firstName} ${individual.lastName}")
        Right(ContactInformation(orgContactFromIndividualName, contact.email, contact.phone, contact.mobile))
      case (None, None) => // This should never happen but adding it here to cover all cases
        logger.warn(s"Neither an Organisation nor individual contact found in ETMP display agent contact response")
        Left(ReadSubscriptionError(UNPROCESSABLE_ENTITY))
    }
  }
}
