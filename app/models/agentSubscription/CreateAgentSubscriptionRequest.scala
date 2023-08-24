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

case class CreateAgentSubscriptionRequest(
  createAgentSubscriptionForCBCRequest: AgentCreateSubscriptionRequest
)

object CreateAgentSubscriptionRequest {

  implicit val format: OFormat[CreateAgentSubscriptionRequest] = Json.format[CreateAgentSubscriptionRequest]

  implicit class CreateAgentSubscriptionRequestExtension(val req: CreateAgentSubscriptionRequest) extends AnyVal {

    def toEtmpRequest: AgentSubscriptionEtmpRequest = {
      val cbcRequestDetail = req.createAgentSubscriptionForCBCRequest.requestDetail

      AgentSubscriptionEtmpRequest(
        idType = cbcRequestDetail.IDType,
        idNumber = cbcRequestDetail.IDNumber,
        gbUser = cbcRequestDetail.isGBUser,
        primaryContact = Contact(
          email = cbcRequestDetail.primaryContact.email,
          organisation = Option(Organisation(cbcRequestDetail.primaryContact.organisationDetails.organisationName)),
          phone = cbcRequestDetail.primaryContact.phone,
          mobile = cbcRequestDetail.primaryContact.mobile
        ),
        tradingName = cbcRequestDetail.tradingName,
        secondaryContact = cbcRequestDetail.secondaryContact.map { contact =>
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

}
