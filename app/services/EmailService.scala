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

import connectors.EmailConnector
import models.agentSubscription.AgentContactDetails
import models.email.{EmailAddress, EmailRequest, EmailTemplate}
import models.error.ReadSubscriptionError
import play.api.Logging
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: EmailConnector, emailTemplate: EmailTemplate, subscriptionService: SubscriptionService)(implicit
                                                                                                                                     executionContext: ExecutionContext
) extends Logging {

  case class EmailResult(emailType: String, result: Option[HttpResponse])

  def sendAndLogEmail(subscriptionId: String, submissionTime: String, messageRefId: String, agentDetails: Option[AgentContactDetails], isUploadSuccessful: Boolean)(implicit
                                                                                                                                                                    hc: HeaderCarrier
  ): Future[Seq[Int]] =
    sendEmail(subscriptionId, submissionTime, messageRefId, agentDetails, isUploadSuccessful) map { responses: Seq[EmailResult] =>
      responses.map {
        case EmailResult(emailType, Some(resp)) =>
          resp.status match {
            case NOT_FOUND => logger.warn(s"$emailType email: The template cannot be found within the email service")
            case BAD_REQUEST => logger.warn(s"$emailType email: Missing parameters from email template: ${resp.body}")
            case ACCEPTED => logger.info(s"$emailType email: Email queued")
            case _ => logger.warn(s"$emailType email: Unhandled status received from email service ${resp.status}")
          }
          resp.status
        case EmailResult("Failed to get contact information", None) =>
          logger.warn("Failed to send email due to ReadSubscriptionError")
          INTERNAL_SERVER_ERROR
        case EmailResult(emailType, _) =>
          logger.warn(s"$emailType email: Failed to send email due to invalid or missing email address")
          NO_CONTENT
      }
    }

  def sendEmail(subscriptionId: String, submissionTime: String, messageRefId: String, agentDetails: Option[AgentContactDetails], isUploadSuccessful: Boolean)(implicit
                                                                                                                                                              hc: HeaderCarrier
  ): Future[Seq[EmailResult]] =
    subscriptionService.getContactInformation(subscriptionId).flatMap {
      case Right(responseDetail) =>
        val emailAddress          = Some(responseDetail.primaryContact.email)
        val contactName           = Some(responseDetail.primaryContact.organisationDetails.organisationName)
        val secondaryEmailAddress = responseDetail.secondaryContact.map(_.email)
        val secondaryName         = responseDetail.secondaryContact.map(_.organisationDetails.organisationName)
        val agentPrimaryEmail     = agentDetails.map(_.subscriptionDetails.primaryContact.email)
        val agentPrimaryName      = agentDetails.map(_.subscriptionDetails.primaryContact.organisationDetails.organisationName)
        val agentSecondaryEmail   = agentDetails.flatMap(_.subscriptionDetails.secondaryContact.map(_.email))
        val agentSecondaryName    = agentDetails.flatMap(_.subscriptionDetails.secondaryContact.map(_.organisationDetails.organisationName))
        val cbcId                 = responseDetail.subscriptionID

        (agentDetails.isDefined, isUploadSuccessful) match {
          case (false, _) =>
            logger.info("Organisation User: Org emails sent to email service")
            for {
              primaryResponse <- send(emailAddress, contactName, emailTemplate.getOrganisationTemplate(isUploadSuccessful), submissionTime, messageRefId, None)
              secondaryResponse <- send(secondaryEmailAddress, secondaryName, emailTemplate.getOrganisationTemplate(isUploadSuccessful), submissionTime, messageRefId, None)
            } yield Seq(EmailResult("Primary Org", primaryResponse), EmailResult("Secondary Org", secondaryResponse))

          case (true, true) =>
            logger.info("Agent User with successful file upload: Agent and Org emails sent to email service")
            for {
              agentPrimaryResponse <- send(agentPrimaryEmail, agentPrimaryName, emailTemplate.getAgentTemplate(isUploadSuccessful), submissionTime, messageRefId, Some(cbcId))
              agentSecondaryResponse <- send(agentSecondaryEmail, agentSecondaryName, emailTemplate.getAgentTemplate(isUploadSuccessful), submissionTime, messageRefId, Some(cbcId))
              primaryResponse <- send(emailAddress, contactName, emailTemplate.getOrganisationTemplate(isUploadSuccessful), submissionTime, messageRefId, None)
              secondaryResponse <- send(secondaryEmailAddress, secondaryName, emailTemplate.getOrganisationTemplate(isUploadSuccessful), submissionTime, messageRefId, None)
            } yield Seq(EmailResult("Primary Agent", agentPrimaryResponse), EmailResult("Secondary Agent", agentSecondaryResponse), EmailResult("Primary Org", primaryResponse), EmailResult("Secondary Org", secondaryResponse))

          case (true, false) =>
            logger.info("Agent User with rejected file: Agent emails sent to email service")
            for {
              agentPrimaryResponse <- send(agentPrimaryEmail, agentPrimaryName, emailTemplate.getAgentTemplate(isUploadSuccessful), submissionTime, messageRefId, Some(cbcId))
              agentSecondaryResponse <- send(agentSecondaryEmail, agentSecondaryName, emailTemplate.getAgentTemplate(isUploadSuccessful), submissionTime, messageRefId, Some(cbcId))
            } yield Seq(EmailResult("Primary Agent", agentPrimaryResponse), EmailResult("Secondary Agent", agentSecondaryResponse))
        }
      case Left(ReadSubscriptionError(value)) =>
        logger.warn(s"Failed to get contact information, received ReadSubscriptionError: $value")
        Future.successful(Seq(EmailResult("Failed to get contact information", None)))
    }

  private def send(emailAddress: Option[String], contactName: Option[String], template: String, submissionTime: String, messageRefId: String, cbcId: Option[String])
  (implicit hc: HeaderCarrier): Future[Option[HttpResponse]] = {
    emailAddress
      .filter(EmailAddress.isValid)
      .fold(Future.successful(Option.empty[HttpResponse])) { email =>
        emailConnector
          .sendEmail(
            EmailRequest.fileUploadSubmission(email, contactName, template, submissionTime, messageRefId, cbcId)
          )
          .map(Some.apply)
      }
  }
}
