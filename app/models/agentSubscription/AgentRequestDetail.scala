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

case class AgentRequestDetail(
  IDType: String,
  IDNumber: String,
  tradingName: Option[String],
  isGBUser: Boolean,
  primaryContact: AgentContactInformation,
  secondaryContact: Option[AgentContactInformation]
)

object AgentRequestDetail {
  implicit val requestDetailFormats: OFormat[AgentRequestDetail] =
    Json.format[AgentRequestDetail]
}

case class AgentCreateSubscriptionRequest(
  requestCommon: AgentRequestCommonForSubscription,
  requestDetail: AgentRequestDetail
)

object AgentCreateSubscriptionRequest {
  implicit val format: OFormat[AgentCreateSubscriptionRequest] =
    Json.format[AgentCreateSubscriptionRequest]
}
