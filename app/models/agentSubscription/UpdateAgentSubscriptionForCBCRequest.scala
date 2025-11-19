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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

case class AgentRequestCommonForUpdate(regime: String,
                                       receiptDate: String,
                                       acknowledgementReference: String,
                                       originatingSystem: String,
                                       requestParameters: Option[Seq[AgentRequestParameter]]
)

object AgentRequestCommonForUpdate {
  implicit val format: OFormat[AgentRequestCommonForUpdate] = Json.format[AgentRequestCommonForUpdate]

  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
  private val mdtp      = "MDTP"

  def apply(regime: String): AgentRequestCommonForUpdate = {
    // Generate a 32 chars UUID without hyphens
    val acknowledgementReference = UUID.randomUUID().toString.replace("-", "")
    AgentRequestCommonForUpdate(
      regime = regime,
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = acknowledgementReference,
      originatingSystem = mdtp,
      requestParameters = None
    )
  }
}

case class AgentRequestDetailForUpdate(IDType: String,
                                       IDNumber: String,
                                       tradingName: Option[String],
                                       isGBUser: Boolean,
                                       primaryContact: AgentContactInformation,
                                       secondaryContact: Option[AgentContactInformation]
)

object AgentRequestDetailForUpdate {

  implicit val reads: Reads[AgentRequestDetailForUpdate] = (
    (__ \ "IDType").read[String] and
      (__ \ "IDNumber").read[String] and
      (__ \ "tradingName").readNullable[String] and
      (__ \ "isGBUser").read[Boolean] and
      (__ \ "primaryContact").read[AgentContactInformation] and
      (__ \ "secondaryContact").readNullable[AgentContactInformation]
  )((idt, idr, tn, gb, pc, sc) => AgentRequestDetailForUpdate(idt, idr, tn, gb, pc, sc))

  implicit lazy val writes: Writes[AgentRequestDetailForUpdate] = (
    (__ \ "IDType").write[String] and
      (__ \ "IDNumber").write[String] and
      (__ \ "tradingName").writeNullable[String] and
      (__ \ "isGBUser").write[Boolean] and
      (__ \ "primaryContact").write[AgentContactInformation] and
      (__ \ "secondaryContact").writeNullable[AgentContactInformation]
  )(r => (r.IDType, r.IDNumber, r.tradingName, r.isGBUser, r.primaryContact, r.secondaryContact))

  implicit class UpdateAgentSubscriptionRequestExtension(val req: AgentRequestDetailForUpdate) extends AnyVal {

    def toUpdateEtmpRequest: AgentSubscriptionEtmpRequest =
      AgentSubscriptionEtmpRequest(
        idType = req.IDType,
        idNumber = req.IDNumber,
        gbUser = req.isGBUser,
        primaryContact = Contact(
          email = req.primaryContact.email,
          organisation = Option(Organisation(req.primaryContact.organisationDetails.organisationName)),
          phone = req.primaryContact.phone,
          mobile = req.primaryContact.mobile
        ),
        tradingName = req.tradingName,
        secondaryContact = req.secondaryContact.map { contact =>
          Contact(
            email = contact.email,
            organisation = Option(Organisation(contact.organisationDetails.organisationName)),
            phone = contact.phone,
            mobile = contact.mobile
          )
        }
      )
  }
}

case class UpdateAgentSubscriptionDetails(requestCommon: AgentRequestCommonForUpdate, requestDetail: AgentRequestDetailForUpdate)

object UpdateAgentSubscriptionDetails {
  implicit val format: OFormat[UpdateAgentSubscriptionDetails] = Json.format[UpdateAgentSubscriptionDetails]
}

case class UpdateAgentSubscriptionForCBCRequest(updateAgentSubscriptionForCBCRequest: UpdateAgentSubscriptionDetails)

object UpdateAgentSubscriptionForCBCRequest {
  implicit val format: OFormat[UpdateAgentSubscriptionForCBCRequest] = Json.format[UpdateAgentSubscriptionForCBCRequest]

  def apply(requestDetail: AgentRequestDetailForUpdate): UpdateAgentSubscriptionForCBCRequest =
    UpdateAgentSubscriptionForCBCRequest(UpdateAgentSubscriptionDetails(AgentRequestCommonForUpdate("CBC"), requestDetail))
}
