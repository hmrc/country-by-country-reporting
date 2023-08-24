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

package models.agentSubscription

import play.api.libs.json.{Json, OFormat}

sealed trait ContactType

final case class Individual(
  firstName: String,
  lastName: String,
  middleName: Option[String] = None
) extends ContactType

object Individual {
  implicit val format: OFormat[Individual] = Json.format[Individual]
}

final case class Organisation(name: String) extends ContactType

object Organisation {
  implicit val format: OFormat[Organisation] = Json.format[Organisation]
}

final case class Contact(
  email: String,
  individual: Option[Individual] = None,
  organisation: Option[Organisation] = None,
  phone: Option[String] = None,
  mobile: Option[String] = None
)

object Contact {
  implicit val format: OFormat[Contact] = Json.format[Contact]
}

final case class CreateAgentSubscriptionEtmpRequest(
  idType: String,
  idNumber: String,
  gbUser: Boolean,
  primaryContact: Contact,
  tradingName: Option[String] = None,
  secondaryContact: Option[Contact] = None
)

object CreateAgentSubscriptionEtmpRequest {
  implicit val format: OFormat[CreateAgentSubscriptionEtmpRequest] = Json.format[CreateAgentSubscriptionEtmpRequest]
}
