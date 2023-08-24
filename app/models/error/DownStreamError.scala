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

package models.error

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

sealed trait DownStreamError {
  def detail: String
}

final case class BusinessErrorDetail(processingDate: LocalDateTime, code: String, text: String)

object BusinessErrorDetail {
  implicit val format: OFormat[BusinessErrorDetail] = Json.format[BusinessErrorDetail]
}

final case class BusinessValidationError(errors: BusinessErrorDetail) extends DownStreamError {
  override def detail: String = s"code: ${errors.code}, message: ${errors.text}"
}

object BusinessValidationError {
  implicit val format: OFormat[BusinessValidationError] = Json.format[BusinessValidationError]
}

final case class BackendSAPSystemErrorDetail(code: String, message: String, logID: String)

object BackendSAPSystemErrorDetail {
  implicit val format: OFormat[BackendSAPSystemErrorDetail] = Json.format[BackendSAPSystemErrorDetail]
}

final case class BackendSAPSystemError(error: BackendSAPSystemErrorDetail) extends DownStreamError {
  override def detail: String = s"code: ${error.code}, message: ${error.message}, logId: ${error.logID}"
}

object BackendSAPSystemError {
  implicit val format: OFormat[BackendSAPSystemError] = Json.format[BackendSAPSystemError]
}

final case class SourceFaultDetail(detail: Seq[String])

object SourceFaultDetail {
  implicit val format: OFormat[SourceFaultDetail] = Json.format[SourceFaultDetail]
}

final case class ErrorDetail(
  timestamp: String,
  correlationId: Option[String],
  errorCode: String,
  errorMessage: String,
  source: String,
  sourceFaultDetail: Option[SourceFaultDetail]
)

object ErrorDetail {
  implicit val format: OFormat[ErrorDetail] = Json.format[ErrorDetail]
}

final case class ErrorDetails(errorDetail: ErrorDetail) extends DownStreamError {
  override def detail: String = s"${errorDetail.sourceFaultDetail.map(_.detail.mkString)}"
}

object ErrorDetails {
  implicit val format: OFormat[ErrorDetails] = Json.format[ErrorDetails]
}
