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

case class SDESAuditResponse(
  correlationId: String,
  fileName: String,
  notification: String,
  checksum: String,
  conversationId: Option[String],
  subscriptionId: Option[String],
  messageRefId: Option[String],
  errorMessage: Option[String],
  fileError: Option[String]
)

object SDESAuditResponse {
  implicit val format: OFormat[SDESAuditResponse] = Json.format[SDESAuditResponse]

  def apply(sdesCallback: SdesCallback,
            conversationId: Option[String] = None,
            subscriptionId: Option[String] = None,
            messageRefId: Option[String] = None,
            error: Option[String] = None,
            fileError: Option[String] = None
  ): SDESAuditResponse =
    new SDESAuditResponse(
      notification = sdesCallback.notification.toString,
      conversationId = conversationId,
      subscriptionId = subscriptionId,
      messageRefId = messageRefId,
      fileName = sdesCallback.filename,
      correlationId = sdesCallback.correlationID.value,
      checksum = sdesCallback.checksum,
      errorMessage = error,
      fileError = fileError
    )
}
