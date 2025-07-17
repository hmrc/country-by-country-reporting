/*
 * Copyright 2025 HM Revenue & Customs
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
import models.submission.{CBC401, TestData}
import models.validation.{GenericError, Message}
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.Organisation

class AuditDetailForSubmissionValidationSpec extends SpecBase {

  "Audit Detail for File Validation" - {
    val userType        = Organisation.toString
    val conversationId  = "conv-id"
    val subscriptionId  = "sub-id"
    val fileReferenceId = "file-reference-id"
    val errorURL        = "validation-failure-url"

    "formats for a successful file validation (fileError = false)" in {
      val messageRefId        = "mref-id"
      val messageTypeIndic    = CBC401.toString
      val reportingEntityName = "Test Company"
      val reportType          = TestData.toString

      val auditDetailObject = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = Some(messageRefId),
        messageTypeIndicator = Some(messageTypeIndic),
        reportingEntityName = Some(reportingEntityName),
        reportType = Some(reportType),
        userType = userType,
        fileError = false
      )

      val expectedJson = Json.parse(s"""
                                       |{
                                       |    "conversationId" : "$conversationId",
                                       |    "subscriptionId" : "$subscriptionId",
                                       |    "fileReferenceId" : "$fileReferenceId",
                                       |    "messageRefId" : "$messageRefId",
                                       |    "messageTypeIndicator" : "$messageTypeIndic",
                                       |    "reportingEntityName" : "$reportingEntityName",
                                       |    "reportType" : "$reportType",
                                       |    "userType" : "$userType",
                                       |    "fileError" : false
                                       |}
                                       |""".stripMargin)

      Json.toJson(auditDetailObject) mustBe expectedJson
      expectedJson.as[AuditDetailForSubmissionValidation] mustBe auditDetailObject
    }

    "formats for a file validation failure (schema errors)" in {
      val errorMessage = "Failed to validate XML submission against schema"

      val testErrors =
        Seq(
          GenericError(176, Message("xml.empty.field", List("Entity"))),
          GenericError(258, Message("xml.add.a.element", List("Summary")))
        )

      val auditDetailObject = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = userType,
        fileError = true,
        errorMessage = Some(errorMessage),
        errorURL = Some(errorURL),
        validationErrors = Some(testErrors)
      )

      val expectedJson = Json.parse(s"""
             |{"conversationId":"conv-id",
             |"subscriptionId":"sub-id",
             |"fileReferenceId":"file-reference-id",
             |"userType":"Organisation",
             |"fileError":true,
             |"errorMessage":"Failed to validate XML submission against schema",
             |"errorURL":"validation-failure-url",
             |"validationErrors":"176 -> xml.empty.field (Entity); 258 -> xml.add.a.element (Summary)"}
             |""".stripMargin)

      Json.toJson(auditDetailObject) mustBe expectedJson

    }
    "formats for an InvalidXmlError" in {
      val errorMessage = "SAX parse error: Premature end of file."

      val auditDetailObject = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = userType,
        fileError = true,
        errorMessage = Some(errorMessage),
        errorURL = Some(errorURL)
      )

      val expectedJson = Json.parse(s"""
                                       |{
                                       |    "conversationId" : "$conversationId",
                                       |    "subscriptionId" : "$subscriptionId",
                                       |    "fileReferenceId" : "$fileReferenceId",
                                       |    "userType" : "$userType",
                                       |    "fileError" : true,
                                       |    "errorMessage" : "$errorMessage",
                                       |    "errorURL" : "$errorURL"
                                       |}
                                       |""".stripMargin)

      Json.toJson(auditDetailObject) mustBe expectedJson
      expectedJson.as[AuditDetailForSubmissionValidation] mustBe auditDetailObject
    }

    "formats for invalid Upscan URL" in {
      val errorMessage = "Missing or invalid Upscan URL"

      val auditDetailObject = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = userType,
        fileError = true,
        errorMessage = Some(errorMessage),
        errorURL = Some(errorURL),
        validationErrors = None
      )

      val expectedJson = Json.parse(s"""
                                       |{
                                       |    "conversationId" : "$conversationId",
                                       |    "subscriptionId" : "$subscriptionId",
                                       |    "fileReferenceId" : "$fileReferenceId",
                                       |    "userType" : "$userType",
                                       |    "fileError" : true,
                                       |    "errorMessage" : "$errorMessage",
                                       |    "errorURL" : "$errorURL"
                                       |}
                                       |""".stripMargin)

      Json.toJson(auditDetailObject) mustBe expectedJson
      expectedJson.as[AuditDetailForSubmissionValidation] mustBe auditDetailObject
    }

    "formats for an unexpected internal server error during validation" in {
      val errorMessage = "An unexpected error occurred during XML validation"

      val auditDetailObject = AuditDetailForSubmissionValidation(
        conversationId = conversationId,
        subscriptionId = subscriptionId,
        fileReferenceId = fileReferenceId,
        messageRefId = None,
        messageTypeIndicator = None,
        reportingEntityName = None,
        reportType = None,
        userType = userType,
        fileError = true,
        errorMessage = Some(errorMessage),
        errorURL = Some(errorURL)
      )

      val expectedJson = Json.parse(s"""
                                       |{
                                       |    "conversationId" : "$conversationId",
                                       |    "subscriptionId" : "$subscriptionId",
                                       |    "fileReferenceId" : "$fileReferenceId",
                                       |    "userType" : "$userType",
                                       |    "fileError" : true,
                                       |    "errorMessage" : "$errorMessage",
                                       |    "errorURL" : "$errorURL"
                                       |}
                                       |""".stripMargin)

      Json.toJson(auditDetailObject) mustBe expectedJson
      expectedJson.as[AuditDetailForSubmissionValidation] mustBe auditDetailObject
    }
  }
}
