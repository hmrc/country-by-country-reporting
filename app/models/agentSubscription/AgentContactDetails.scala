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

import models.subscription.ContactInformation
import play.api.libs.json.{__, Json, OFormat, Writes}

case class AgentContactDetails(agentReferenceNumber: String, subscriptionDetails: AgentResponseDetail)
object AgentContactDetails {
  implicit val format: OFormat[AgentContactDetails] = Json.format[AgentContactDetails]

  implicit lazy val agentResponseDetailWrites: Writes[AgentResponseDetail] = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "subscriptionID").write[String] and
        (__ \ "tradingName").writeNullable[String] and
        (__ \ "isGBUser").write[Boolean] and
        (__ \ "primaryContact").write[Seq[ContactInformation]] and
        (__ \ "secondaryContact").writeNullable[Seq[ContactInformation]]
    )(r => (r.subscriptionID, r.tradingName, r.isGBUser, Seq(r.primaryContact), r.secondaryContact.map(Seq(_))))

  }
}
