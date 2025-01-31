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

package models.error

import julienrf.json.derived
import play.api.libs.json.OFormat
import uk.gov.hmrc.auth.core.AffinityGroup

trait BackendError {
  def detail: String
}

final case class SdesSubmissionError(status: Int) extends BackendError {
  override def detail: String = s"SDES submission failed with status $status"
}
final case class RepositoryError(detail: String) extends BackendError
final case class SubmissionServiceError(detail: String, userType: Option[AffinityGroup] = None) extends BackendError

object SubmissionServiceError {
  implicit val format: OFormat[SubmissionServiceError] = derived.oformat()
}
