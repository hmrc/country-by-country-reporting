/*
 * Copyright 2024 HM Revenue & Customs
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

package models.audit

import base.SpecBase
import models.submission._
import models.upscan.UploadId
import play.api.libs.json.Json

class AuditDetailForFileSubmissionSpec extends SpecBase {

  "AuditDetailForFileSubmission" - {

    "marshal with all values when SubmissionDetails" in {

      val submissionDetails = SubmissionDetails(
        fileName = "cbc-report.xml",
        uploadId = UploadId("7e67633b-596b-454d-b7b1-c85fe3fdf994"),
        enrolmentId = "sub-123",
        fileSize = 1234,
        documentUrl = "test",
        checksum = "test",
        messageSpecData = MessageSpecData("msg-456", CBC402, "GlobalCorp Ltd", NewInformation)
      )

      val auditDetail = Json.parse("""
          |{
          |    "conversationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
          |    "subscriptionId" : "sub-123",
          |    "fileReferenceId" : "file-reference-id",
          |    "messageRefId" : "msg-456",
          |    "fileSize" : 1234,
          |    "reportingEntityName" : "GlobalCorp Ltd",
          |    "reportType" : "NEW_INFORMATION",
          |    "status" : "Pending",
          |    "fileName" : "cbc-report.xml",
          |    "submittedDatetime" : "2025-07-25T00:00",
          |    "fileType" : "XML"
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForFileSubmission]

      val jsonObj = AuditDetailForFileSubmission(submissionDetails, "file-reference-id", Pending, "2025-07-25T00:00")
      jsonObj mustBe auditDetailRequest
    }
  }
}
