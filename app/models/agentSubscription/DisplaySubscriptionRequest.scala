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

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

case class AgentRequestParameter(paramName: String, paramValue: String)

object AgentRequestParameter {
  implicit val indentifierFormats: OFormat[AgentRequestParameter] =
    Json.format[AgentRequestParameter]
}

case class AgentRequestCommonForSubscription(
  regime: String,
  receiptDate: String,
  acknowledgementReference: String,
  originatingSystem: String,
  requestParameters: Option[Seq[AgentRequestParameter]]
)

object AgentRequestCommonForSubscription {
  // Format: ISO 8601 YYYY-MM-DDTHH:mm:ssZ e.g. 2020-09-23T16:12:11Zs
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  implicit val agentRequestCommonForSubscriptionFormats: OFormat[AgentRequestCommonForSubscription] =
    Json.format[AgentRequestCommonForSubscription]

  def apply(): AgentRequestCommonForSubscription = {
    // Generate a 32 chars UUID without hyphens
    val acknowledgementReference = UUID.randomUUID().toString.replace("-", "")

    AgentRequestCommonForSubscription(
      regime = "CBC",
      receiptDate = ZonedDateTime.now().format(formatter),
      acknowledgementReference = acknowledgementReference,
      originatingSystem = "MDTP",
      requestParameters = None
    )
  }
}

case class AgentReadSubscriptionRequestDetail(IDType: String, IDNumber: String)

object AgentReadSubscriptionRequestDetail {
  implicit val format: OFormat[AgentReadSubscriptionRequestDetail] =
    Json.format[AgentReadSubscriptionRequestDetail]

  def apply(agentRefNo: String): AgentReadSubscriptionRequestDetail               = new AgentReadSubscriptionRequestDetail("ARN", agentRefNo)
  def apply(IDType: String, IDNumber: String): AgentReadSubscriptionRequestDetail = new AgentReadSubscriptionRequestDetail(IDType, IDNumber)

}
