/*
 * Copyright 2024 HM Revenue & Customs
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

package models.sdes

import models.sdes.NotificationType.NotificationType
import models.submission.ConversationId
import play.api.libs.json._

import java.time.{ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter

object NotificationType extends Enumeration {
  type NotificationType = Value
  val FileReady: Value             = Value("FileReady")
  val FileReceived: Value          = Value("FileReceived")
  val FileProcessed: Value         = Value("FileProcessed")
  val FileProcessingFailure: Value = Value("FileProcessingFailure")

  implicit val writes: Writes[NotificationType] = Writes[NotificationType] { v =>
    JsString(v.toString)
  }

  implicit val reads: Reads[NotificationType] = Reads[NotificationType] { json =>
    json.validate[String].flatMap {
      case "FileReady"             => JsSuccess(FileReady)
      case "FileReceived"          => JsSuccess(FileReceived)
      case "FileProcessed"         => JsSuccess(FileProcessed)
      case "FileProcessingFailure" => JsSuccess(FileProcessingFailure)
      case _                       => JsError("Invalid Notification")
    }
  }
}

case class SdesCallback(notification: NotificationType,
                        filename: String,
                        checksumAlgorithm: Algorithm,
                        checksum: String,
                        correlationID: ConversationId,
                        dateTime: Option[ZonedDateTime],
                        failureReason: Option[String] = None
)

object SdesCallback {

  implicit val dateFormat: Format[ZonedDateTime] = new Format[ZonedDateTime] {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")

    override def reads(json: JsValue): JsResult[ZonedDateTime] =
      json.validate[String].map(ZonedDateTime.parse(_, formatter).withZoneSameInstant(ZoneOffset.UTC))

    override def writes(o: ZonedDateTime): JsValue = JsString(o.format(formatter))
  }

  implicit val format: OFormat[SdesCallback] = Json.format[SdesCallback]
}
