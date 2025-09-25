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
import com.lucidchart.open.xtract.XmlReader
import models.submission.{Accepted, ConversationId, FileDetails, NewInformation}
import models.xml.FileErrorCode.FailedSchemaValidation
import models.xml.RecordErrorCode.MessageTypeIndic
import models.xml.{BREResponse, FileErrors, GenericStatusMessage, RecordError, ValidationErrors, ValidationStatus}
import play.api.libs.json.Json

import java.time.LocalDateTime
import scala.xml.XML

class AuditDetailForEISResponseSpec extends SpecBase {

  "AuditDetailForEISResponse" - {
    "marshal without fileDetails" in {

      val ganericStatusMessage = GenericStatusMessage(
        ValidationErrors(
          None,
          None
        ),
        ValidationStatus.accepted
      )

      val breResponse = BREResponse("CBC", "conv-789", ganericStatusMessage)

      val auditDetail = Json.parse("""
          |{
          |    "regime" : "CBC",
          |    "conversationId" : "conv-789",
          |    "status" : "Accepted"
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForEISResponse]

      val jsonObj = AuditDetailForEISResponse(breResponse = breResponse, fileDetails = None)
      jsonObj mustBe auditDetailRequest
    }

    "marshal with accepted" in {

      val ganericStatusMessage = GenericStatusMessage(
        ValidationErrors(
          None,
          None
        ),
        ValidationStatus.accepted
      )

      val breResponse = BREResponse("CBC", "conv-789", ganericStatusMessage)

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
        None,
        None,
        LocalDateTime.now(),
        LocalDateTime.now()
      )

      val auditDetail = Json.parse("""
          |{
          |    "regime" : "CBC",
          |    "conversationId" : "conv-789",
          |    "subscriptionId" : "sub-123",
          |    "messageRefId" : "msg-456",
          |    "status" : "Accepted"
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForEISResponse]

      val jsonObj = AuditDetailForEISResponse(breResponse = breResponse, fileDetails = Some(fileDetails))
      jsonObj mustBe auditDetailRequest
    }

    "marshal with rejected and all errors" in {

      val validationErrorsXML                = XML.loadFile("test/resources/cbc/fileUpload/fileErrors.xml")
      val validationErrors: ValidationErrors = XmlReader.of[ValidationErrors].read(validationErrorsXML).getOrElse(ValidationErrors(None, None))
      val ganericStatusMessage               = GenericStatusMessage(validationErrors, ValidationStatus.rejected)

      val breResponse = BREResponse("CBC", "conv-789", ganericStatusMessage)

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
        None,
        None,
        LocalDateTime.now(),
        LocalDateTime.now()
      )

      noException should be thrownBy AuditDetailForEISResponse(breResponse = breResponse, fileDetails = Some(fileDetails))
    }

    "marshal with rejected and both errors" in {

      val ganericStatusMessage = GenericStatusMessage(
        ValidationErrors(
          Some(List(FileErrors(FailedSchemaValidation, Some("MessageRefID has already been used")))),
          Some(
            List(
              RecordError(
                MessageTypeIndic,
                Some("DocRefID already used"),
                Some(List("failed"))
              )
            )
          )
        ),
        ValidationStatus.rejected
      )

      val breResponse = BREResponse("CBC", "conv-789", ganericStatusMessage)

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
        None,
        None,
        LocalDateTime.now(),
        LocalDateTime.now()
      )

      val auditDetail = Json.parse("""
          |{
          |    "regime" : "CBC",
          |    "conversationId" : "conv-789",
          |    "subscriptionId" : "sub-123",
          |    "messageRefId" : "msg-456",
          |    "status" : "Rejected",
          |    "validationErrors" : {
          |      "fileError" : [ {
          |        "code" : "50007",
          |        "details" : "MessageRefID has already been used"
          |      } ],
          |      "recordError" : [ {
          |        "code" : "80010",
          |        "details" : "DocRefID already used",
          |        "docRefIDInError" : ["failed"]
          |      } ]
          |    }
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForEISResponse]

      val jsonObj = AuditDetailForEISResponse(breResponse = breResponse, fileDetails = Some(fileDetails))
      jsonObj mustBe auditDetailRequest
    }

    "marshal with rejected and file errors only" in {

      val ganericStatusMessage = GenericStatusMessage(
        ValidationErrors(
          Some(
            List(
              FileErrors(FailedSchemaValidation, Some("MessageRefID has already been used")),
              FileErrors(FailedSchemaValidation, Some("MessageRefID has already been used"))
            )
          ),
          None
        ),
        ValidationStatus.rejected
      )

      val breResponse = BREResponse("CBC", "conv-789", ganericStatusMessage)

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
        None,
        None,
        LocalDateTime.now(),
        LocalDateTime.now()
      )

      val auditDetail = Json.parse("""
          |{
          |    "regime" : "CBC",
          |    "conversationId" : "conv-789",
          |    "subscriptionId" : "sub-123",
          |    "messageRefId" : "msg-456",
          |    "status" : "Rejected",
          |    "validationErrors" : {
          |      "fileError" : [ {
          |        "code" : "50007",
          |        "details" : "MessageRefID has already been used"
          |      },
          |       {
          |        "code" : "50007",
          |        "details" : "MessageRefID has already been used"
          |      }]
          |      }
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForEISResponse]

      val jsonObj = AuditDetailForEISResponse(breResponse = breResponse, fileDetails = Some(fileDetails))
      jsonObj mustBe auditDetailRequest
    }

    "marshal with rejected and record errors only" in {

      val ganericStatusMessage = GenericStatusMessage(
        ValidationErrors(
          None,
          Some(
            List(
              RecordError(
                MessageTypeIndic,
                Some("DocRefID already used"),
                Some(List("failed"))
              ),
              RecordError(
                MessageTypeIndic,
                Some("DocRefID already used"),
                Some(List("failed"))
              )
            )
          )
        ),
        ValidationStatus.rejected
      )

      val breResponse = BREResponse("CBC", "conv-789", ganericStatusMessage)

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
        None,
        None,
        LocalDateTime.now(),
        LocalDateTime.now()
      )

      val auditDetail = Json.parse("""
          |{
          |    "regime" : "CBC",
          |    "conversationId" : "conv-789",
          |    "subscriptionId" : "sub-123",
          |    "messageRefId" : "msg-456",
          |    "status" : "Rejected",
          |    "validationErrors" : {
          |      "recordError" : [ {
          |        "code" : "80010",
          |        "details" : "DocRefID already used",
          |        "docRefIDInError" : ["failed"]
          |      },
          |       {
          |        "code" : "80010",
          |        "details" : "DocRefID already used",
          |        "docRefIDInError" : ["failed"]
          |      }]
          |    }
          |}
          |""".stripMargin)
      val auditDetailRequest = auditDetail.as[AuditDetailForEISResponse]

      val jsonObj = AuditDetailForEISResponse(breResponse = breResponse, fileDetails = Some(fileDetails))
      jsonObj mustBe auditDetailRequest
    }

  }
}
