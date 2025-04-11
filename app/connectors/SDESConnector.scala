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
import models.sdes._
import play.api.Logging
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SDESConnector @Inject() (
  val config: AppConfig,
  val http: HttpClientV2
) extends Logging {

  private val clientIdHeader: Seq[(String, String)] = Seq("x-client-id" -> config.sdesClientId)

  def sendFileNotification(
    request: FileTransferNotification
  )(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Int, Int]] =
    http
      .post(url"${config.sdesUrl}") //, request, clientIdHeader)
      .setHeader(clientIdHeader: _*)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case NO_CONTENT => Right(response.status)
          case _ =>
            logger.warn(s"Received status [${response.status}] from SDES for correlationId [${request.audit.correlationID}]")
            Left(response.status)
        }
      }
}
