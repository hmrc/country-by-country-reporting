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

package models.audit

import base.SpecBase
import models.sdes.SdesCallback
import models.submission.{Accepted, ConversationId, FileDetails, NewInformation}
import play.api.libs.json.Json

import java.time.LocalDateTime

class SDESAuditResponseSpec extends SpecBase {

  "SDESAuditResponse" - {
    "marshal with mandatory values" in {
      val sdesCallbackAsJson = Json.parse("""
                                    |{
                                    |  "notification": "FileProcessed",
                                    |  "filename": "test file.xml",
                                    |  "checksumAlgorithm": "SHA-256",
                                    |  "checksum": "1222374536363abef3633",
                                    |  "correlationID": "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                    |  "dateTime": "2024-08-07T10:13:09.429Z",
                                    |  "failureReason": null
                                    |}
                                    |""".stripMargin)
      val sdesCallBack = sdesCallbackAsJson.as[SdesCallback]
      val auditDetail = Json.parse("""
                                     |{
                                     |    "notification" : "FileProcessed",
                                     |    "conversationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "subscriptionId" : "sub-123",
                                     |    "messageRefId" : "msg-456",
                                     |    "fileName" : "test file.xml",
                                     |    "correlationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "checkSumAlgorithm" : "SHA256",
                                     |    "checksum" : "1222374536363abef3633",
                                     |    "dateTime" : "2024-08-07T10:13:09.429Z",
                                     |    "fileError" : false
                                     |}
                                     |""".stripMargin)
      val auditDetailRequest = auditDetail.as[SDESAuditResponse]
      val fileDetails = FileDetails(
        ConversationId("7e67633b-596b-454d-b7b1-c85fe3fdf994"),
        "sub-123",
        "msg-456",
        "",
        NewInformation,
        Accepted,
        "",
        LocalDateTime.now(),
        LocalDateTime.now(),
        None,
        None
      )
      val jsonObj = SDESAuditResponse(sdesCallBack, Some(fileDetails))
      auditDetailRequest mustBe jsonObj
    }

    "marshal with all values" in {
      val sdesCallbackAsJson = Json.parse("""
                                    |{
                                    |  "notification": "FileProcessed",
                                    |  "filename": "test file.xml",
                                    |  "checksumAlgorithm": "SHA-256",
                                    |  "checksum": "1222374536363abef3633",
                                    |  "correlationID": "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                    |  "failureReason": "virus found"
                                    |}
                                    |""".stripMargin)
      val sdesCallBack = sdesCallbackAsJson.as[SdesCallback]
      val auditDetail = Json.parse("""
                                     |{
                                     |    "notification" : "FileProcessed",
                                     |    "conversationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "subscriptionId" : "sub-123",
                                     |    "messageRefId" : "msg-456",
                                     |    "fileName" : "test file.xml",
                                     |    "correlationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "checkSumAlgorithm" : "SHA256",
                                     |    "checksum" : "1222374536363abef3633",
                                     |    "fileError" : true,
                                     |    "errorMessage" : "virus found"
                                     |}
                                     |""".stripMargin)
      val auditDetailRequest = auditDetail.as[SDESAuditResponse]
      val fileDetails = FileDetails(
        ConversationId("7e67633b-596b-454d-b7b1-c85fe3fdf994"),
        "sub-123",
        "msg-456",
        "",
        NewInformation,
        Accepted,
        "",
        LocalDateTime.now(),
        LocalDateTime.now(),
        None,
        None
      )
      val jsonObj = SDESAuditResponse(sdesCallBack, Some(fileDetails))
      auditDetailRequest mustBe jsonObj
    }
    "marshal without file detail values values" in {
      val sdesCallbackAsJson = Json.parse("""
                                            |{
                                            |  "notification": "FileProcessed",
                                            |  "filename": "test file.xml",
                                            |  "checksumAlgorithm": "SHA-256",
                                            |  "checksum": "1222374536363abef3633",
                                            |  "correlationID": "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                            |  "dateTime": "2024-08-07T10:13:09.429Z",
                                            |  "failureReason": null
                                            |}
                                            |""".stripMargin)
      val sdesCallBack = sdesCallbackAsJson.as[SdesCallback]
      val auditDetail = Json.parse("""
                                     |{
                                     |    "notification" : "FileProcessed",
                                     |    "conversationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "fileName" : "test file.xml",
                                     |    "correlationId" : "7e67633b-596b-454d-b7b1-c85fe3fdf994",
                                     |    "checkSumAlgorithm" : "SHA256",
                                     |    "checksum" : "1222374536363abef3633",
                                     |    "dateTime" : "2024-08-07T10:13:09.429Z",
                                     |    "fileError" : false
                                     |}
                                     |""".stripMargin)
      val auditDetailRequest = auditDetail.as[SDESAuditResponse]
      val jsonObj            = SDESAuditResponse(sdesCallBack, None)
      auditDetailRequest mustBe jsonObj
    }
  }
}
