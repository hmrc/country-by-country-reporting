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

package services.audit

import play.api.libs.json.{Json, OWrites, Reads}

case class AuditDetail(
  fileSize: Long,
  conversationId: String,
  subscriptionId: String,
  messageRefId: Option[String],
  messageTypeIndicator: Option[String],
  reportingEntityName: Option[String],
  reportType: Option[String],
  userType: String,
  fileError: Boolean,
  errorMessage: Option[String] = None,
  errorURL: Option[String] = None,
  validationErrors: Option[Seq[AuditValidationError]] = None
)

object AuditDetail {
  implicit val writes: OWrites[AuditDetail] = Json.writes[AuditDetail]
  implicit val reads: Reads[AuditDetail]    = Json.reads[AuditDetail]
}

case class AuditValidationError(code: String, message: String)

object AuditValidationError {
  implicit val writes: OWrites[AuditValidationError] = Json.writes[AuditValidationError]
  implicit val reads: Reads[AuditValidationError]    = Json.reads[AuditValidationError]
}
