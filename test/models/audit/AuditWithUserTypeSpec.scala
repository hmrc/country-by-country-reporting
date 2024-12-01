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

package models.audit

import base.SpecBase
import models.sdes.{NotificationType, SHA256, SdesCallback}
import models.submission.ConversationId
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.AffinityGroup

class AuditWithUserTypeSpec extends SpecBase {

  private val auditWithUserType = AuditWithUserType(
    details = SdesCallback(
      notification = NotificationType.FileReady,
      filename = "filename",
      checksumAlgorithm = SHA256,
      checksum = "checksum",
      correlationID = ConversationId("correlationID"),
      dateTime = None,
      failureReason = None
    ),
    userType = Some(AffinityGroup.Organisation)
  )

  private val json =
    """
      |{
      |"notification": "FileReady",
      |"filename": "filename",
      |"checksumAlgorithm": "SHA-256",
      |"checksum": "checksum",
      |"correlationID": "correlationID",
      |"userType": "Organisation"
      |}
      |""".stripMargin

  "AuditWithUserType" - {
    "marshal" in {
      val jsonObj = AuditWithUserType.writes[SdesCallback].writes(auditWithUserType)
      jsonObj mustBe Json.parse(json).as[JsObject]
    }
  }
}
