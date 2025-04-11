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

package connectors

import com.google.inject.Inject
import config.AppConfig
import models.agentSubscription.AgentSubscriptionEtmpRequest
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionConnector @Inject() (
  val config: AppConfig,
  val http: HttpClientV2
) {

  def createSubscription(agentSubscription: AgentSubscriptionEtmpRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-agent-subscription"

    http
      .post(url"${config.serviceUrl(serviceName)}")
      .setHeader(extraHeaders(serviceName): _*)
      .withBody(Json.toJson(agentSubscription))
      .execute[HttpResponse]
  }

  def readSubscription(agentRefNo: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "read-agent-subscription"

    http
      .get(url"${config.serviceUrl(serviceName)}/ARN/$agentRefNo")
      .setHeader(extraHeaders(serviceName): _*)
      .execute[HttpResponse]
  }

  def updateSubscription(agentSubscription: AgentSubscriptionEtmpRequest)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val serviceName = "update-agent-subscription"

    http
      .put(url"${config.serviceUrl(serviceName)}")
      .setHeader(extraHeaders(serviceName): _*)
      .withBody(Json.toJson(agentSubscription))
      .execute[HttpResponse]
  }

  private def extraHeaders(serviceName: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Seq()
    .withBearerToken(s"${config.bearerToken(serviceName)}")
    .withXForwardedHost()
    .withDate()
    .withXCorrelationId()
    .withXConversationId()
    .withContentType(Some("application/json"))
    .withAccept(Some("application/json"))
    .withXRegimeType("CBC")
    .withEnvironment(Some(config.environment(serviceName)))
}
