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

// models/upscan/UpscanAuditDetails.scala
package models.audit

import models.upscan.{FailedCallbackBody, ReadyCallbackBody}
import play.api.libs.json.{Json, OFormat}

case class UpscanAuditDetails(
  reference: String,
  fileStatus: String,
  downloadUrl: Option[String] = None,
  fileName: Option[String] = None,
  fileMimeType: Option[String] = None,
  size: Option[Long] = None,
  checksum: Option[String] = None,
  failureReason: Option[String] = None,
  message: Option[String] = None,
  conversationId: Option[String] = None,
  subscriptionId: Option[String] = None,
  messageRefId: Option[String] = None,
  error: Option[String] = None,
  fileError: Option[String] = None
)

object UpscanAuditDetails {
  implicit val format: OFormat[UpscanAuditDetails] = Json.format[UpscanAuditDetails]

  def apply(readyCallback: ReadyCallbackBody): UpscanAuditDetails =
    UpscanAuditDetails(
      reference = readyCallback.reference.value,
      fileStatus = "READY",
      downloadUrl = Some(readyCallback.downloadUrl),
      fileName = Some(readyCallback.uploadDetails.fileName),
      fileMimeType = Some(readyCallback.uploadDetails.fileMimeType),
      size = Some(readyCallback.uploadDetails.size),
      checksum = Some(readyCallback.uploadDetails.checksum)
    )

  def apply(failedCallback: FailedCallbackBody): UpscanAuditDetails =
    UpscanAuditDetails(
      reference = failedCallback.reference.value,
      fileStatus = "FAILED",
      failureReason = Some(failedCallback.failureDetails.failureReason),
      message = Some(failedCallback.failureDetails.message)
    )
}
