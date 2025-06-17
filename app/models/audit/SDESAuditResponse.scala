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

import models.sdes.SdesCallback
import play.api.libs.json.{Json, OFormat}

import java.time.ZonedDateTime

case class SDESAuditResponse(
  correlationId: String,
  fileName: String,
  notification: String,
  checkSumAlgorithm: String,
  checksum: String,
  dateTime: Option[ZonedDateTime],
  conversationId: Option[String],
  subscriptionId: Option[String],
  messageRefId: Option[String],
  errorMessage: Option[String],
  fileError: Boolean
)

object SDESAuditResponse {
  implicit val format: OFormat[SDESAuditResponse] = Json.format[SDESAuditResponse]

  def apply(sdesCallback: SdesCallback,
            conversationId: Option[String] = None,
            subscriptionId: Option[String] = None,
            messageRefId: Option[String] = None,
            error: Option[String] = None,
            fileError: Boolean
  ): SDESAuditResponse =
    new SDESAuditResponse(
      notification = sdesCallback.notification.toString,
      conversationId = conversationId,
      subscriptionId = subscriptionId,
      messageRefId = messageRefId,
      fileName = sdesCallback.filename,
      correlationId = sdesCallback.correlationID.value,
      checkSumAlgorithm = sdesCallback.checksumAlgorithm.toString,
      checksum = sdesCallback.checksum,
      dateTime = sdesCallback.dateTime,
      errorMessage = error,
      fileError = fileError
    )
}
