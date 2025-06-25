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

import models.submission.FileDetails
import models.xml
import models.xml.BREResponse
import models.xml.ValidationStatus.rejected
import play.api.libs.json._

case class ValidationErrors(fileError: Option[Seq[FileError]], recordError: Option[Seq[RecordError]])

object ValidationErrors {
  implicit val format: OFormat[ValidationErrors] = Json.format[ValidationErrors]
}

case class FileError(code: String, details: Option[String])

object FileError {
  implicit val format: OFormat[FileError] = Json.format[FileError]
}

case class RecordError(code: String, details: Option[String], docRefIDInError: Option[Seq[String]])

object RecordError {
  implicit val format: OFormat[RecordError] = Json.format[RecordError]
}

case class AuditDetailForEISResponse(regime: String,
                                     conversationId: String,
                                     subscriptionId: Option[String],
                                     messageRefId: Option[String],
                                     status: String,
                                     validationErrors: Option[ValidationErrors]
)

object AuditDetailForEISResponse {
  implicit val format: OFormat[AuditDetailForEISResponse] = Json.format[AuditDetailForEISResponse]

  def apply(breResponse: BREResponse, fileDetails: Option[FileDetails]): AuditDetailForEISResponse = {
    val (subscriptionId, messageRefId) = (
      for (details <- fileDetails) yield details.subscriptionId,
      for (details <- fileDetails) yield details.messageRefId
    )
    val genericStatusMessage = breResponse.genericStatusMessage

    val validationErrors: Option[ValidationErrors] =
      if (genericStatusMessage.status == rejected) {
        Some(populateValidationErrors(genericStatusMessage.validationErrors))
      } else None

    AuditDetailForEISResponse(
      regime = breResponse.regime,
      conversationId = breResponse.conversationID,
      subscriptionId = subscriptionId,
      messageRefId = messageRefId,
      status = genericStatusMessage.status.toString,
      validationErrors = validationErrors
    )
  }

  private def populateValidationErrors(validationErrors: xml.ValidationErrors) = ValidationErrors(
    fileError = for (errors <- validationErrors.fileError) yield errors.map(fileErr => FileError(fileErr.code.code, fileErr.details)),
    recordError =
      for (errors <- validationErrors.recordError)
        yield errors.map(recordErr => RecordError(recordErr.code.code, recordErr.details, recordErr.docRefIDInError))
  )

}
