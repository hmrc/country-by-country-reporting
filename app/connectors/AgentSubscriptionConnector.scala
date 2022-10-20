/*
 * Copyright 2022 HM Revenue & Customs
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
import models.agentSubscription.{CreateAgentSubscriptionRequest, DisplayAgentSubscriptionForCBCRequest, UpdateAgentSubscriptionForCBCRequest}
import models.subscription.{DisplaySubscriptionForCBCRequest, UpdateSubscriptionForCBCRequest}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class AgentSubscriptionConnector @Inject()(
  val config: AppConfig,
  val http: HttpClient
) {

  def createSubscription(subscription: CreateAgentSubscriptionRequest)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "create-agent-subscription"

    http.POST[CreateAgentSubscriptionRequest, HttpResponse](
      config.serviceUrl(serviceName),
      subscription,
      headers = extraHeaders(serviceName)
    )(
      wts = CreateAgentSubscriptionRequest.format,
      rds = httpReads,
      hc = hc,
      ec = ec
    )
  }

  def readSubscription(agentSubscription: DisplayAgentSubscriptionForCBCRequest)
                      (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {
    val serviceName = "read-agent-subscription"

    http.POST[DisplayAgentSubscriptionForCBCRequest, HttpResponse](
      config.serviceUrl(serviceName),
      agentSubscription,
      headers = extraHeaders(serviceName)
    )(
      wts = DisplayAgentSubscriptionForCBCRequest.format,
      rds = httpReads,
      hc = hc,
      ec = ec
    )
  }

  def updateSubscription(updateAgentSubscription: UpdateAgentSubscriptionForCBCRequest)
                        (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

    val serviceName = "update-agent-subscription"

    http.POST[UpdateAgentSubscriptionForCBCRequest, HttpResponse](
      config.serviceUrl(serviceName),
      updateAgentSubscription,
      extraHeaders(serviceName)
    )(
      wts = UpdateAgentSubscriptionForCBCRequest.format,
      rds = httpReads,
      hc = hc,
      ec = ec
    )
  }

  private def extraHeaders(serviceName: String)(implicit hc: HeaderCarrier): Seq[(String, String)] = Seq()
    .withBearerToken(s"${config.bearerToken(serviceName)}")
    .withXForwardedHost()
    .withDate()
    .withXCorrelationId()
    .withXConversationId()
    .withContentType(Some("application/json"))
    .withAccept(Some("application/json"))
    .withEnvironment(Some(config.environment(serviceName)))
}
