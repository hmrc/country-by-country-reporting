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

import base.SpecBase
import config.AppConfig
import connectors.EmailConnector
import generators.Generators
import models.agentSubscription.{AgentContactDetails, AgentResponseDetail}
import models.email.EmailRequest
import models.error.ReadSubscriptionError
import models.submission._
import models.subscription.{ContactInformation, OrganisationDetails, ResponseDetail}
import org.mockito.ArgumentMatchers.{any, eq => mockitoEq}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import utils.DateTimeFormatUtil

import java.time.LocalDateTime
import scala.concurrent.{ExecutionContext, Future}

class EmailServiceSpec extends SpecBase with Generators with ScalaCheckPropertyChecks with BeforeAndAfterEach {

  override def beforeEach: Unit =
    reset(
      mockEmailConnector,
      mockSubscriptionService
    )

  private val mockEmailConnector: EmailConnector = mock[EmailConnector]
  private val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]
  private implicit val mockHeaderCarrier: HeaderCarrier = mock[HeaderCarrier]

  override lazy val app: Application = new GuiceApplicationBuilder()
    .overrides(
      bind[EmailConnector].toInstance(mockEmailConnector),
      bind[SubscriptionService].toInstance(mockSubscriptionService)
    )
    .build()

  val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val emailService: EmailService = app.injector.instanceOf[EmailService]

  val primaryContact = ContactInformation(
    OrganisationDetails("organisationName"),
    "test@email.com",
    None,
    None
  )

  val secondaryContact = ContactInformation(
    OrganisationDetails("OtherName"),
    "second@email.com",
    None,
    None
  )

  val agentPrimaryContact = ContactInformation(
    OrganisationDetails("agentName"),
    "agent@email.com",
    None,
    None
  )

  val agentSecondaryContact = ContactInformation(
    OrganisationDetails("agentOtherName"),
    "agentSecond@email.com",
    None,
    None
  )

  val submissionTime = DateTimeFormatUtil.displayFormattedDate(LocalDateTime.now)
  val messageRefId = "messageRefId"
  val subscriptionId = "subscriptionId"
  val tradingName = "tradingName"
  val reportType = TestData

  val agentDetails = AgentContactDetails("ARN", AgentResponseDetail(subscriptionId, Option(tradingName), isGBUser = true, agentPrimaryContact, Some(agentSecondaryContact)))
  val agentSingleContactDetails = AgentContactDetails("ARN", AgentResponseDetail(subscriptionId, Option(tradingName), isGBUser = true, agentPrimaryContact, None))

  "Email Service" - {
    "sendAndLogEmail" - {
      "must submit to the email connector with valid details and return ACCEPTED and NO_CONTENT for a Primary contact only" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, None)))
          )

        val result = emailService.sendAndLogEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result mustBe Seq(ACCEPTED, NO_CONTENT)

          verify(mockEmailConnector, times(1)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }
      "must submit to the email connector and return NOT_FOUND and NO_CONTENT for a Primary contact only when the template is missing" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(NOT_FOUND, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, None)))
          )

        val result = emailService.sendAndLogEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result mustBe Seq(NOT_FOUND, NO_CONTENT)

          verify(mockEmailConnector, times(1)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }
      "must submit to the email connector and return BAD_REQUEST and NO_CONTENT for a Primary contact only when email service rejects request" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(BAD_REQUEST, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, None)))
          )

        val result = emailService.sendAndLogEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result mustBe Seq(BAD_REQUEST, NO_CONTENT)

          verify(mockEmailConnector, times(1)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }
    }
    "sendEmail" - {

      "must submit to the email connector when 1 set of valid details provided" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, None)))
          )

        val result = emailService.sendEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(Some(ACCEPTED), None)

          verify(mockEmailConnector, times(1)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }

      "must submit to the email connector twice when 2 sets of valid details provided" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, Some(secondaryContact))))
          )

        val result = emailService.sendEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(Some(ACCEPTED), Some(ACCEPTED))

          verify(mockEmailConnector, times(2)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }

      "must submit to the email connector four times when Agent and client both have 2 sets of valid details provided for successful file upload" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, Some(secondaryContact))))
          )

        val result = emailService.sendEmail(subscriptionId, submissionTime, messageRefId, Some(agentDetails), isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(Some(ACCEPTED), Some(ACCEPTED), Some(ACCEPTED), Some(ACCEPTED))

          verify(mockEmailConnector, times(4)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }

      "must submit to the email connector once for Agent only when Agent and client both have 1 set of valid details provided for failed file upload" in {

        val emailRequest = EmailRequest(
          List(agentPrimaryContact.email),
          appConfig.emailAgentUnsuccessfulTemplate,
          Map(
            "dateSubmitted" -> submissionTime,
            "messageRefId" -> messageRefId,
            "contactName" -> agentPrimaryContact.organisationDetails.organisationName,
            "cbcId" -> subscriptionId,
            "clientTradingName" -> tradingName,
            "reportType" -> reportType.toString
          )
        )

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, Option(tradingName), isGBUser = true, primaryContact, None)))
          )

        val result = emailService.sendEmail(subscriptionId, submissionTime, messageRefId, Some(agentSingleContactDetails), isUploadSuccessful = false, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(Some(ACCEPTED), None)

          verify(mockEmailConnector, times(1)).sendEmail(mockitoEq(emailRequest))(any[HeaderCarrier])
        }
      }

      "must submit to the email connector 3 times when Agent has 2 sets and client has 1 set of valid details provided for successful file upload" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, None)))
          )

        val result = emailService.sendEmail(subscriptionId, submissionTime, messageRefId, Some(agentDetails), isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(Some(ACCEPTED), Some(ACCEPTED), Some(ACCEPTED), None)

          verify(mockEmailConnector, times(3)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }

      "must submit to the email connector 3 times when Agent has 1 set and client has 2 sets of valid details provided for successful file upload" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact, Some(secondaryContact))))
          )

        val result = emailService.sendEmail(subscriptionId, submissionTime, messageRefId, Some(agentSingleContactDetails), isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(Some(ACCEPTED), None, Some(ACCEPTED), Some(ACCEPTED))

          verify(mockEmailConnector, times(3)).sendEmail(any[EmailRequest])(any[HeaderCarrier])
        }
      }

      "must fail to submit to the email connector when invalid email address provided" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Right(ResponseDetail(subscriptionId, None, isGBUser = true, primaryContact.copy(email = "test"), Some(secondaryContact))))
          )

        val result =
          emailService.sendEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(None, Some(ACCEPTED))
        }
      }

      "must fail to submit to the email connector when failing to retrieve contact details" in {

        when(mockEmailConnector.sendEmail(any[EmailRequest])(any[HeaderCarrier]))
          .thenReturn(
            Future.successful(HttpResponse(ACCEPTED, ""))
          )

        when(mockSubscriptionService.getContactInformation(any[String])(any[HeaderCarrier], any[ExecutionContext]))
          .thenReturn(
            Future.successful(Left(ReadSubscriptionError(INTERNAL_SERVER_ERROR)))
          )

        val result =
          emailService.sendEmail(subscriptionId, submissionTime, messageRefId, None, isUploadSuccessful = true, reportType)

        whenReady(result) { result =>
          result.map(_.result.map(_.status)) mustBe Seq(None)
        }
      }
    }
    "getReportTypeMessage" - {

      "mus give correct message for respective Report Type" in {

        emailService.getReportTypeMessage(TestData) mustBe "email.reportType.TestData"
        emailService.getReportTypeMessage(NewInformation) mustBe "The file contains new information for the reporting period."
        emailService.getReportTypeMessage(DeletionOfAllInformation) mustBe "The file contains a deletion of all previously reported information for this reporting period."
        emailService.getReportTypeMessage(NewInformationForExistingReport) mustBe "The file contains new information for an existing report."
        emailService.getReportTypeMessage(CorrectionForExistingReport) mustBe "The file contains corrections for an existing report."
        emailService.getReportTypeMessage(DeletionForExistingReport) mustBe "The file contains deletions for an existing report."
        emailService.getReportTypeMessage(CorrectionAndDeletionForExistingReport) mustBe "The file contains corrections and deletions for an existing report."
        emailService.getReportTypeMessage(CorrectionForReportingEntity) mustBe "The file contains a correction for the ReportingEntity."

      }
    }

  }
}
