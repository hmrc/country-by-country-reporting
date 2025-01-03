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

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.auth.core.AffinityGroup

object AuditType {
  val eisResponse    = "CountryByCountryReportingEISResponse"
  val sdesResponse   = "CountryByCountryReportingSDESResponse"
  val fileSubmission = "CountryByCountryReportingFileSubmission"
}

final case class AuditWithUserType[T](details: T, userType: Option[AffinityGroup] = None)

object AuditWithUserType {

  implicit def writes[T: OWrites]: OWrites[AuditWithUserType[T]] = (auditWithUserType: AuditWithUserType[T]) => {
    val detailsJson  = Json.toJsObject(auditWithUserType.details)
    val userTypeJson = auditWithUserType.userType.map(userType => Json.obj("userType" -> userType)).getOrElse(Json.obj())
    detailsJson ++ userTypeJson
  }
}
