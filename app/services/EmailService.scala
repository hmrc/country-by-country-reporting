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
import models.submission._
import play.api.Logging
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject() (emailConnector: EmailConnector, emailTemplate: EmailTemplate, subscriptionService: SubscriptionService)(implicit
  executionContext: ExecutionContext
) extends Logging {

  case class EmailResult(emailType: String, result: Option[HttpResponse])

  def sendAndLogEmail(subscriptionId: String,
                      submissionTime: String,
                      messageRefId: String,
                      agentDetails: Option[AgentContactDetails],
                      isUploadSuccessful: Boolean,
                      reportType: ReportType
  )(implicit
    hc: HeaderCarrier
  ): Future[Seq[Int]] =
    sendEmail(subscriptionId, submissionTime, messageRefId, agentDetails, isUploadSuccessful, reportType) map { responses: Seq[EmailResult] =>
      responses.map {
        case EmailResult(emailType, Some(resp)) =>
          resp.status match {
            case NOT_FOUND   => logger.warn(s"$emailType email: The template cannot be found within the email service")
            case BAD_REQUEST => logger.warn(s"$emailType email: Missing parameters from email template: ${resp.body}")
            case ACCEPTED    => logger.info(s"$emailType email: Email queued")
            case _           => logger.warn(s"$emailType email: Unhandled status received from email service ${resp.status}")
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

  def sendEmail(subscriptionId: String,
                submissionTime: String,
                messageRefId: String,
                agentDetails: Option[AgentContactDetails],
                isUploadSuccessful: Boolean,
                reportType: ReportType
  )(implicit hc: HeaderCarrier): Future[Seq[EmailResult]] =
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
        val tradingName           = responseDetail.tradingName
        val reportTypeContent     = getReportTypeMessage(reportType)

        lazy val orgEmails: Seq[Future[EmailResult]] = Seq(
          send(emailAddress,
               contactName,
               emailTemplate.getOrganisationTemplate(isUploadSuccessful),
               submissionTime,
               messageRefId,
               None,
               None,
               Some(reportTypeContent)
          )
            .map(res => EmailResult("Primary Org", res)),
          send(
            secondaryEmailAddress,
            secondaryName,
            emailTemplate.getOrganisationTemplate(isUploadSuccessful),
            submissionTime,
            messageRefId,
            None,
            None,
            Some(reportTypeContent)
          )
            .map(res => EmailResult("Secondary Org", res))
        )

        lazy val agentEmails: Seq[Future[EmailResult]] = Seq(
          send(
            agentPrimaryEmail,
            agentPrimaryName,
            emailTemplate.getAgentTemplate(isUploadSuccessful),
            submissionTime,
            messageRefId,
            Some(cbcId),
            tradingName,
            Some(reportTypeContent)
          ).map(res => EmailResult("Primary Agent", res)),
          send(
            agentSecondaryEmail,
            agentSecondaryName,
            emailTemplate.getAgentTemplate(isUploadSuccessful),
            submissionTime,
            messageRefId,
            Some(cbcId),
            tradingName,
            Some(reportTypeContent)
          ).map(res => EmailResult("Secondary Agent", res))
        )

        (agentDetails.isDefined, isUploadSuccessful) match {
          case (false, _) =>
            logger.info("Organisation User: Org emails sent to email service")
            Future.sequence(orgEmails)
          case (true, true) =>
            logger.info("Agent User with successful file upload: Agent and Org emails sent to email service")
            Future.sequence(agentEmails ++ orgEmails)
          case (true, false) =>
            logger.info("Agent User with rejected file: Agent emails sent to email service")
            Future.sequence(agentEmails)
        }
      case Left(ReadSubscriptionError(value)) =>
        logger.warn(s"Failed to get contact information, received ReadSubscriptionError: $value")
        Future.successful(Seq(EmailResult("Failed to get contact information", None)))
    }

  private def send(emailAddress: Option[String],
                   contactName: Option[String],
                   template: String,
                   submissionTime: String,
                   messageRefId: String,
                   cbcId: Option[String],
                   tradingName: Option[String],
                   reportType: Option[String]
  )(implicit hc: HeaderCarrier): Future[Option[HttpResponse]] =
    emailAddress
      .filter(EmailAddress.isValid)
      .map { email =>
        emailConnector
          .sendEmail(
            EmailRequest.fileUploadSubmission(email, contactName, template, submissionTime, messageRefId, cbcId, tradingName, reportType)
          )
          .map(Some.apply)
      }
      .getOrElse(Future.successful(None))

  def getReportTypeMessage(reportType: ReportType): String = {
    val reportTypeKeyMapMap: Map[ReportType, String] =
      Map(
        TestData                               -> "The file contains Test data.",
        NewInformation                         -> "The file contains new information for the reporting period.",
        DeletionOfAllInformation               -> "The file contains a deletion of all previously reported information for this reporting period.",
        NewInformationForExistingReport        -> "The file contains new information for an existing report.",
        CorrectionForExistingReport            -> "The file contains corrections for an existing report.",
        DeletionForExistingReport              -> "The file contains deletions for an existing report.",
        CorrectionAndDeletionForExistingReport -> "The file contains corrections and deletions for an existing report.",
        CorrectionForReportingEntity           -> "The file contains a correction for the ReportingEntity."
      )
    reportTypeKeyMapMap(reportType)
  }
}
