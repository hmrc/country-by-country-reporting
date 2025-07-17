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

package models.audit

import models.validation.GenericError
import play.api.libs.json.{JsNull, Json, OWrites, Reads}

case class AuditDetailForSubmissionValidation(
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
  validationErrors: Option[Seq[GenericError]] = None
)

object AuditDetailForSubmissionValidation {

  implicit val reads: Reads[AuditDetailForSubmissionValidation] = Json.reads[AuditDetailForSubmissionValidation]

  implicit val writes: OWrites[AuditDetailForSubmissionValidation] = OWrites { detail =>
    val baseJson = Json.writes[AuditDetailForSubmissionValidation].writes(detail)

    detail.validationErrors match {
      case Some(errs) =>
        val formattedErrors = errs
          .map(e => s"${e.lineNumber} -> ${e.message.messageKey} (${e.message.args.mkString(", ")})")
          .mkString("; ")
        baseJson + ("validationErrors" -> Json.toJson(formattedErrors))
      case None =>
        if (baseJson.value.get("validationErrors").contains(JsNull)) {
          baseJson - "validationErrors"
        } else {
          baseJson
        }
    }
  }
}
