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
import play.api.Logger
import play.api.libs.json._

case class AgentResponseDetail(subscriptionID: String,
                               tradingName: Option[String],
                               isGBUser: Boolean,
                               primaryContact: ContactInformation,
                               secondaryContact: Option[ContactInformation]
)

object AgentResponseDetail {
  val logger = Logger.apply(getClass)
  implicit lazy val reads: Reads[AgentResponseDetail] = {
    import play.api.libs.functional.syntax._

    (
      (__ \ "subscriptionID").read[String] and
        (__ \ "tradingName").readNullable[String] and
        (__ \ "isGBUser").read[Boolean] and
        (__ \ "primaryContact").read[Seq[ContactInformation]] and
        (__ \ "secondaryContact").readNullable[Seq[ContactInformation]]
    ) { (subscriptionID, tradingName, isGBUser, primaryContact, secondaryContact) =>
      logger.warn(s"AgentResponseDetail: received ${primaryContact.size} primary contacts and ${secondaryContact.getOrElse(0)} secondaryContacts")
      AgentResponseDetail(subscriptionID, tradingName, isGBUser, primaryContact.head, secondaryContact.map(_.head))
    }
  }

  implicit val writes: OWrites[AgentResponseDetail] = Json.writes[AgentResponseDetail]

}

case class AgentReturnParameters(paramName: String, paramValue: String)

object AgentReturnParameters {
  implicit val format: Format[AgentReturnParameters] = Json.format[AgentReturnParameters]
}

case class AgentResponseCommon(status: String, statusText: Option[String], processingDate: String, returnParameters: Option[Seq[AgentReturnParameters]])

object AgentResponseCommon {
  implicit val format: Format[AgentResponseCommon] = Json.format[AgentResponseCommon]
}

case class DisplayAgentSubscriptionResponse(responseCommon: AgentResponseCommon, responseDetail: AgentResponseDetail)

object DisplayAgentSubscriptionResponse {
  implicit val format: OFormat[DisplayAgentSubscriptionResponse] = Json.format[DisplayAgentSubscriptionResponse]
}

case class DisplayAgentSubscriptionForCBCResponse(displayAgentSubscriptionForCBCResponse: DisplayAgentSubscriptionResponse)

object DisplayAgentSubscriptionForCBCResponse {
  implicit val format: OFormat[DisplayAgentSubscriptionForCBCResponse] = Json.format[DisplayAgentSubscriptionForCBCResponse]
}
