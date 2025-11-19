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

package models.xml

import play.api.libs.json.{Json, OFormat}

import scala.xml.NodeSeq

case class RecordError(code: RecordErrorCode, details: Option[String], docRefIDInError: Option[Seq[String]])

object RecordError {

  given XmlReads[RecordError] with
    def read(xml: NodeSeq): RecordError =
      val code            = fromXml[RecordErrorCode](xml \# "Code")
      val detailsText     = xml \# "Details"
      val details         = Option.when(detailsText.nonEmpty)(detailsText.text.trim)
      val docRefIDs       = (xml \# "DocRefIDInError").map(_.text.trim).filter(_.nonEmpty)
      val docRefIDInError = if docRefIDs.nonEmpty then Some(docRefIDs) else None
      RecordError(code, details, docRefIDInError)

  given format: OFormat[RecordError] = Json.format[RecordError]
}
