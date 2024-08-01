/*
 * Copyright 2024 HM Revenue & Customs
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

package wiremock

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.{MappingBuilder, ResponseDefinitionBuilder, WireMock}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.http.{HttpHeader, HttpHeaders, RequestMethod}
import com.github.tomakehurst.wiremock.{client, WireMockServer}
import WireMockHelper.{wireMockPort, MappingBuilderExt, ResponseDefinitionBuilderExt}

import java.net.ServerSocket
import scala.jdk.CollectionConverters._
import scala.util.Using

trait WireMockHelper {
  val wireMockServer = new WireMockServer(wireMockConfig.port(wireMockPort))

  def startWireMock(): Unit = {
    WireMock.configureFor(wireMockPort)
    wireMockServer.start()
  }

  def stopWireMock(): Unit = wireMockServer.stop()

  def resetWireMock(): Unit = wireMockServer.resetAll()

  def stubResponse(
    url: String,
    statusCode: Int,
    requestMethod: RequestMethod = RequestMethod.POST,
    requestHeaders: Map[String, String] = Map.empty,
    responseBody: String = "",
    responseHeaders: Map[String, String] = Map.empty
  ): Unit =
    stubFor(
      request(requestMethod.getName, urlEqualTo(url))
        .withRequestHeaders(requestHeaders.toSet)
        .willReturn(
          aResponse()
            .withStatus(statusCode)
            .withResponseHeaders(responseHeaders.toSet)
            .withBody(responseBody)
        )
    )

}

object WireMockHelper {

  val wireMockPort: Int = Using(new ServerSocket(0))(_.getLocalPort)
    .getOrElse(throw new Exception("Failed to find random free port"))

  implicit class MappingBuilderExt(builder: client.MappingBuilder) {

    def withRequestHeaders(headers: Set[(String, String)]): MappingBuilder =
      headers.foldLeft(builder) { case (builder, (key, value)) =>
        builder.withHeader(key, equalTo(value))
      }

  }

  implicit class ResponseDefinitionBuilderExt(builder: ResponseDefinitionBuilder) {

    def withResponseHeaders(headers: Set[(String, String)]): ResponseDefinitionBuilder = {
      val responseHeadersWithContentType = Set("Content-Type" -> "application/json; charset=utf-8")
        .union(headers)
        .toList
        .map { case (key, value) =>
          HttpHeader.httpHeader(key, value)
        }
      builder.withHeaders(new HttpHeaders(responseHeadersWithContentType.asJava))
    }

  }

}
