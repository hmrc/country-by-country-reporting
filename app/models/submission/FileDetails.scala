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

import models.agentSubscription.AgentContactDetails
import play.api.libs.json._
import uk.gov.hmrc.auth.core.AffinityGroup

import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.Singleton

@Singleton
case class FileDetails(_id: ConversationId,
                       subscriptionId: String,
                       messageRefId: String,
                       reportingEntityName: String,
                       reportType: ReportType,
                       status: FileStatus,
                       name: String,
                       submitted: LocalDateTime,
                       lastUpdated: LocalDateTime,
                       agentDetails: Option[AgentContactDetails] = None,
                       userType: Option[AffinityGroup] = None,
                       fileType: Option[FileType] = None
)
object FileDetails {
  final val localDateTimeReads: Reads[LocalDateTime] =
    Reads
      .at[String](__ \ "$date" \ "$numberLong")
      .map(dateTime => Instant.ofEpochMilli(dateTime.toLong).atZone(ZoneOffset.UTC).toLocalDateTime)

  final val localDateTimeWrites: Writes[LocalDateTime] =
    Writes
      .at[String](__ \ "$date" \ "$numberLong")
      .contramap(_.toInstant(ZoneOffset.UTC).toEpochMilli.toString)

  implicit val mongoDateTime: Format[LocalDateTime] = Format(localDateTimeReads, localDateTimeWrites)
  final val reads: Reads[FileDetails]               = Json.reads[FileDetails]
  final val writes: Writes[FileDetails] = (fileDetails: FileDetails) =>
    Json.writes[FileDetails].writes(fileDetails) match {
      case obj: JsObject => obj + ("conversationId" -> Json.toJson(fileDetails._id))
      case other         => other
    }
  implicit val format: Format[FileDetails] = Format(reads, writes)

  def logMessageForStaleReport(fileDetails: FileDetails): String = {
    val sentTo = fileDetails.fileType match {
      case Some(LargeFile)  => "SDES"
      case Some(NormalFile) => "EIS"
      case _                => "EIS"
    }
    s"Stale file found - Report Type: ${fileDetails.reportType}, ConversationId: ${fileDetails._id.value}, Filename: ${fileDetails.name}, File Type: ${fileDetails.fileType
      .getOrElse("")}, Sent to $sentTo"
  }
}
