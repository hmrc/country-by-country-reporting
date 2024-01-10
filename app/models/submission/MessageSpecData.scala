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

package models.submission

import play.api.libs.json._

sealed trait MessageTypeIndic
case object CBC401 extends MessageTypeIndic
case object CBC402 extends MessageTypeIndic

object MessageTypeIndic {

  def fromString(typeIndic: String): MessageTypeIndic = typeIndic.toUpperCase match {
    case "CBC401" => CBC401
    case "CBC402" => CBC402
    case _        => throw new NoSuchElementException
  }

  implicit val writes: Writes[MessageTypeIndic] = Writes[MessageTypeIndic] {
    case CBC401 => JsString("CBC401")
    case CBC402 => JsString("CBC402")
  }
  implicit val reads: Reads[MessageTypeIndic] = Reads[MessageTypeIndic] {
    case JsString("CBC401") => JsSuccess(CBC401)
    case JsString("CBC402") => JsSuccess(CBC402)
    case value              => JsError(s"Unexpected value of _type: $value")
  }
}

sealed trait ReportType
case object TestData extends ReportType
case object NewInformation extends ReportType
case object DeletionOfAllInformation extends ReportType
case object NewInformationForExistingReport extends ReportType
case object CorrectionForExistingReport extends ReportType
case object DeletionForExistingReport extends ReportType
case object CorrectionAndDeletionForExistingReport extends ReportType
case object CorrectionForReportingEntity extends ReportType

object ReportType {
  implicit val format: OFormat[ReportType] = {
    implicit def testDataFormats: OFormat[TestData.type] = Json.format[TestData.type]
    implicit def newInfoFormats: OFormat[NewInformation.type] = Json.format[NewInformation.type]
    implicit def deletionFormats: OFormat[DeletionOfAllInformation.type] = Json.format[DeletionOfAllInformation.type]
    implicit def newInfoForExistingFormats: OFormat[NewInformationForExistingReport.type] = Json.format[NewInformationForExistingReport.type]
    implicit def correctionForExistingFormats: OFormat[CorrectionForExistingReport.type] = Json.format[CorrectionForExistingReport.type]
    implicit def deletionForExistingFormats: OFormat[DeletionForExistingReport.type] = Json.format[DeletionForExistingReport.type]
    implicit def correctionAndDeletionForExistingFormats: OFormat[CorrectionAndDeletionForExistingReport.type] = Json.format[CorrectionAndDeletionForExistingReport.type]
    implicit def correctionFormats: OFormat[CorrectionForReportingEntity.type] = Json.format[CorrectionForReportingEntity.type]

    Json.format[ReportType]
  }
}

case class MessageSpecData(messageRefId: String, messageTypeIndic: MessageTypeIndic, reportingEntityName: String, reportType: ReportType = CorrectionForReportingEntity)

object MessageSpecData {
  implicit val format: OFormat[MessageSpecData] = Json.format[MessageSpecData]
}
