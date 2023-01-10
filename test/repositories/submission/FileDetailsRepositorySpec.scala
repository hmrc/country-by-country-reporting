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

package repositories.submission

import base.SpecBase
import metrics.MetricsService
import models.agentSubscription.AgentResponseDetail
import models.submission._
import models.subscription.{ContactInformation, OrganisationDetails}
import models.xml.{FileErrorCode, FileErrors, ValidationErrors}
import play.api.Configuration
import services.AgentDetails
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

class FileDetailsRepositorySpec extends SpecBase with DefaultPlayMongoRepositorySupport[FileDetails] {

  lazy val config              = app.injector.instanceOf[Configuration]
  lazy val metricsService      = app.injector.instanceOf[MetricsService]
  override lazy val repository = new FileDetailsRepository(mongoComponent, config, metricsService)

  val dateTimeNow: LocalDateTime = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
  val fileDetails: FileDetails =
    FileDetails(
      ConversationId("conversationId123456"),
      "subscriptionId",
      "messageRefId",
      "Reporting Entity",
      Pending,
      "file1.xml",
      dateTimeNow,
      dateTimeNow
    )

  val agentPrimaryContact: ContactInformation = ContactInformation(
    OrganisationDetails("agentName"),
    "agent@email.com",
    None,
    None
  )

  val agentSecondaryContact: ContactInformation = ContactInformation(
    OrganisationDetails("agentOtherName"),
    "agentSecond@email.com",
    None,
    None
  )
  val agentDetails: AgentDetails = AgentDetails("ARN", AgentResponseDetail("subscriptionId2", None, isGBUser = true, agentPrimaryContact, Some(agentSecondaryContact)))

  val agentFileDetails: FileDetails = FileDetails(
    ConversationId("conversationId1234567"),
    "subscriptionId2",
    "messageRefId2",
    "Reporting Entity",
    Pending,
    "file1.xml",
    dateTimeNow,
    dateTimeNow,
    Some(agentDetails)
  )

  "Insert" - {
    "must insert FileDetails" in {
      val res = repository.insert(fileDetails)
      whenReady(res) { result =>
        result mustBe true
      }
    }
    "must insert FileDetails for an Agent" in {
      val res = repository.insert(agentFileDetails)
      whenReady(res) { result =>
        result mustBe true
      }
    }

    "must read FileDetails by SubscriptionId" in {
      val insert = repository.insert(fileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.findBySubscriptionId("subscriptionId")
      whenReady(res) { result =>
        result mustBe Seq(fileDetails)
      }
    }

    "must read FileDetails by SubscriptionId for an Agent" in {
      val insert = repository.insert(agentFileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.findBySubscriptionId("subscriptionId2")
      whenReady(res) { result =>
        result mustBe Seq(agentFileDetails)
      }
    }

    "must read FileDetails by ConversationId" in {
      val insert = repository.insert(fileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.findByConversationId(ConversationId("conversationId123456"))
      whenReady(res) { result =>
        result mustBe Some(fileDetails)
      }
    }

    "must read FileDetails by ConversationId for an Agent" in {
      val insert = repository.insert(agentFileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.findByConversationId(ConversationId("conversationId1234567"))
      whenReady(res) { result =>
        result mustBe Some(agentFileDetails)
      }
    }

    "must read FileDetails by ConversationId doesn't exists" in {
      val insert = repository.insert(fileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.findByConversationId(ConversationId("conversationId12345678"))
      whenReady(res) { result =>
        result mustBe None
      }
    }

    "must update FileDetails status to Accepted by ConversationId" in {
      val insert = repository.insert(fileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.updateStatus("conversationId123456", Accepted)
      whenReady(res) { result =>
        result must matchPattern {
          case Some(FileDetails(ConversationId("conversationId123456"), "subscriptionId", "messageRefId", "Reporting Entity", Accepted, "file1.xml", _, _, _)) =>
        }
      }
    }

    "must update FileDetails status to Accepted by ConversationId for an Agent" in {
      val insert = repository.insert(agentFileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.updateStatus("conversationId1234567", Accepted)
      whenReady(res) { result =>
        result must matchPattern {
          case Some(FileDetails(ConversationId("conversationId1234567"), "subscriptionId2", "messageRefId2", "Reporting Entity", Accepted, "file1.xml", _, _, _)) =>
        }
      }
    }

    "must update FileDetails status to Rejected by ConversationId" in {
      val insert = repository.insert(fileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.updateStatus("conversationId123456",
                                        Rejected(ValidationErrors(Some(Seq(FileErrors(FileErrorCode.FailedSchemaValidation, Some("details")))), None))
      )

      whenReady(res) { result =>
        result must matchPattern {
          case Some(
                FileDetails(ConversationId("conversationId123456"),
                            "subscriptionId",
                            "messageRefId",
                            "Reporting Entity",
                            Rejected(ValidationErrors(Some(Seq(FileErrors(FileErrorCode.FailedSchemaValidation, Some("details")))), None)),
                            "file1.xml",
                            _,
                            _,
                            None
                )
              ) =>
        }
      }
    }

    "must read FileStatus by ConversationId" in {
      val insert = repository.insert(fileDetails)
      whenReady(insert) { result =>
        result mustBe true
      }
      val res = repository.findStatusByConversationId(ConversationId("conversationId123456"))
      whenReady(res) { result =>
        result mustBe Some(Pending)
      }
    }

  }

}
