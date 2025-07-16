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

package models.audit

import models.submission.{FileStatus, ReportType, SubmissionDetails}
import play.api.libs.json._

case class AuditDetailForFileSubmission(conversationId: String,
                                        subscriptionId: String,
                                        fileReferenceId: String,
                                        messageRefId: String,
                                        fileSize: Long,
                                        reportingEntityName: String,
                                        reportType: ReportType,
                                        status: String,
                                        fileName: String,
                                        submittedDatetime: String,
                                        fileType: String
)

object AuditDetailForFileSubmission {
  implicit val format: OFormat[AuditDetailForFileSubmission] = Json.format[AuditDetailForFileSubmission]

  def apply(sumbmissionDetails: SubmissionDetails, fileReferenceId: String, fileStatus: FileStatus, submissionTime: String): AuditDetailForFileSubmission =
    new AuditDetailForFileSubmission(conversationId = sumbmissionDetails.uploadId.value,
                                     subscriptionId = sumbmissionDetails.enrolmentId,
                                     fileReferenceId = fileReferenceId,
                                     messageRefId = sumbmissionDetails.messageSpecData.messageRefId,
                                     fileSize = sumbmissionDetails.fileSize,
                                     reportingEntityName = sumbmissionDetails.messageSpecData.reportingEntityName,
                                     reportType = sumbmissionDetails.messageSpecData.reportType,
                                     status = fileStatus.toString,
                                     fileName = sumbmissionDetails.fileName,
                                     submittedDatetime = submissionTime,
                                     fileType = "XML"
    )

}
