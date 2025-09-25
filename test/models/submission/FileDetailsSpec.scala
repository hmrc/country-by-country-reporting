/*
 * Copyright 2025 HM Revenue & Customs
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

package models.submission

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.Json

import java.time.LocalDateTime

class FileDetailsSpec extends AnyFreeSpec with Matchers {

  "FileDetails" - {
    "should contain conversationId when serialised" in {

      val fileDetails = FileDetails(
        _id = ConversationId("conversationId"),
        subscriptionId = "submissionId",
        messageRefId = "messageRefId",
        reportingEntityName = "reportingEntityName",
        reportType = TestData,
        status = Accepted,
        name = "name",
        submitted = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
        agentDetails = None,
        userType = None,
        fileType = None,
        reportingPeriodStartDate = LocalDateTime.now(),
        reportingPeriodEndDate = LocalDateTime.now()
      )

      val json = Json.toJson(fileDetails)
      (json \ "conversationId").as[String] shouldBe "conversationId"
    }

    "should contain relevant information to be logged when record is stale" in {
      val fileDetails = FileDetails(
        _id = ConversationId("conversationId"),
        subscriptionId = "submissionId",
        messageRefId = "messageRefId",
        reportingEntityName = "reportingEntityName",
        reportType = TestData,
        status = Accepted,
        name = "report.xml",
        submitted = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now(),
        agentDetails = None,
        userType = None,
        fileType = Some(LargeFile),
        reportingPeriodStartDate = LocalDateTime.now(),
        reportingPeriodEndDate = LocalDateTime.now()
      )

      val expectedMessageLargeFile =
        s"Stale file found - Report Type: TestData, ConversationId: conversationId, Filename: report.xml, File Type: LargeFile, Sent to SDES"
      val expectedMessageNormalFile =
        s"Stale file found - Report Type: TestData, ConversationId: conversationId, Filename: report.xml, File Type: NormalFile, Sent to EIS"

      FileDetails.logMessageForStaleReport(fileDetails) shouldBe expectedMessageLargeFile
      FileDetails.logMessageForStaleReport(fileDetails.copy(fileType = Some(NormalFile))) shouldBe expectedMessageNormalFile
    }

  }
}
