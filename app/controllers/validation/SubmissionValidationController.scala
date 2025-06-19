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

package controllers.validation

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

import controllers.auth.IdentifierAuthAction
import models.audit.{AuditType, ValidationAudit}
import models.upscan.UpscanURL
import models.validation.{GenericError, InvalidXmlError, SubmissionValidationFailure, SubmissionValidationSuccess}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.audit.{AuditDetail, AuditService, AuditValidationError}
import services.validation.UploadedXmlValidationEngine
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionValidationController @Inject() (cc: ControllerComponents,
                                                validationEngine: UploadedXmlValidationEngine,
                                                auditService: AuditService,
                                                authenticate: IdentifierAuthAction
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private val ValidationFailureErrorUrl = "country-by-country-reporting/problem/validation-failure"
  private val InvalidXmlErrorUrl        = "country-by-country-reporting/problem/not-xml"
  private val MissingUpscanUrl          = "country-by-country-reporting/problem/missing-url"
  private val InternalServerErrorUrl    = "country-by-country-reporting/problem/internal-error"

  def validateSubmission: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    val conversationId: String              = request.headers.get("X-Conversation-ID").getOrElse("UNKNOWN_CONVERSATION_ID")
    val subscriptionId: String              = request.headers.get("X-Subscription-ID").getOrElse("UNKNOWN_SUBSCRIPTION_ID")
    val reportingEntityName: Option[String] = request.headers.get("X-Reporting-Entity-Name") // Optional header
    val reportType: Option[String]          = Some("NEW_INFORMATION_FOR_EXISTING_REPORT") // todo Or derive this from content

    val messageRefId: Option[String]         = None
    val messageTypeIndicator: Option[String] = None

    request.body.validate[UpscanURL] match {
      case JsSuccess(upscanURL, _) =>
        val fileSize: Long = 0L //todo FIND THIS

        validationEngine.validateUploadSubmission(upscanURL.url) map {
          case SubmissionValidationSuccess(messageSubmissionData) =>

            val detail = AuditDetail(
              fileSize = fileSize,
              conversationId = conversationId,
              subscriptionId = subscriptionId,
              messageRefId = messageRefId,
              messageTypeIndicator = messageTypeIndicator,
              reportingEntityName = reportingEntityName,
              reportType = reportType,
              userType = request.affinityGroup.toString,
              fileError = false
            )
            auditService.sendValidationAuditEvent(ValidationAudit(AuditType.fileValidation, detail))
            Ok(Json.toJsObject(SubmissionValidationSuccess(messageSubmissionData)))

          case SubmissionValidationFailure(validationErrors) =>
            val mappedValidationErrors = validationErrors.errors.map { err: GenericError =>

              AuditValidationError(
                code = err.lineNumber.toString,
                message = err.message.toString
              )
            }
            val detail = AuditDetail(
              fileSize = fileSize,
              conversationId = conversationId,
              subscriptionId = subscriptionId,
              messageRefId = None,
              messageTypeIndicator = None,
              reportingEntityName = reportingEntityName,
              reportType = reportType,
              userType = request.affinityGroup.toString,
              fileError = true,
              errorMessage = Some("Failed to validate XML submission against schema"),
              errorURL = Some(ValidationFailureErrorUrl),
              validationErrors = Some(mappedValidationErrors)
            )
            auditService.sendValidationAuditEvent(ValidationAudit(AuditType.fileValidation, detail))
            logger.warn("Failed to validate XML submission against schema")
            Ok(Json.toJson(SubmissionValidationFailure(validationErrors)))

          case InvalidXmlError(saxException) =>
            val detail = AuditDetail(
              fileSize = fileSize,
              conversationId = conversationId,
              subscriptionId = subscriptionId,
              messageRefId = None,
              messageTypeIndicator = None,
              reportingEntityName = reportingEntityName,
              reportType = reportType,
              userType = request.affinityGroup.toString,
              fileError = true,
              errorMessage = Some(saxException),
              errorURL = Some(InvalidXmlErrorUrl),
              validationErrors = None
            )
            auditService.sendValidationAuditEvent(ValidationAudit(AuditType.fileValidation, detail))
            logger.warn(s"InvalidXmlError: $saxException")
            BadRequest(InvalidXmlError(saxException).toString)

          case _ =>
            val detail = AuditDetail(
              fileSize = fileSize,
              conversationId = conversationId,
              subscriptionId = subscriptionId,
              messageRefId = None,
              messageTypeIndicator = None,
              reportingEntityName = reportingEntityName,
              reportType = reportType,
              userType = request.affinityGroup.toString,
              fileError = true,
              errorMessage = Some("An unexpected error occurred during XML validation"),
              errorURL = Some(InternalServerErrorUrl),
              validationErrors = None
            )
            auditService.sendValidationAuditEvent(ValidationAudit(AuditType.fileValidation, detail))
            logger.warn("Failed to validate submission due to unexpected outcome from validationEngine")
            InternalServerError("Failed to validate submission")
        }
      case JsError(errors) =>
        val detail = AuditDetail(
          fileSize = 0L,
          conversationId = conversationId,
          subscriptionId = subscriptionId,
          messageRefId = None,
          messageTypeIndicator = None,
          reportingEntityName = reportingEntityName,
          reportType = None,
          userType = request.affinityGroup.toString,
          fileError = true,
          errorMessage = Some(s"Missing or invalid Upscan URL: ${JsError.toJson(errors).toString()}"),
          errorURL = Some(MissingUpscanUrl),
          validationErrors = None
        )
        auditService.sendValidationAuditEvent(ValidationAudit(AuditType.fileValidation, detail))
        logger.warn(s"Missing upscan URL: $errors")
        Future.successful(InternalServerError("Missing upscan URL"))
    }
  }
}
