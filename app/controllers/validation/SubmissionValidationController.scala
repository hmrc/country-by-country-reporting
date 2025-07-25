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

package controllers.validation

import controllers.auth.{IdentifierAuthAction, IdentifierRequest}
import models.audit.{Audit, AuditDetailForSubmissionValidation, AuditType}
import models.submission.MessageSpecData
import models.upscan.ValidateRequest
import models.validation._
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.audit.AuditService
import services.validation.UploadedXmlValidationEngine
import uk.gov.hmrc.http.HeaderCarrier
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

  private val dataErrorsUrl          = "/problem/data-errors"
  private val invalidXmlErrorUrl     = "/problem/invalid-xml"
  private val internalServerErrorUrl = "/problem/there-is-a-problem"

  def validateSubmission: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    request.body.validate[ValidateRequest] match {
      case JsSuccess(validateRequest, _) =>
        validationEngine.validateUploadSubmission(validateRequest.url) map {
          case SubmissionValidationSuccess(messageSubmissionData) =>
            sendSuccessfulAuditEvent(validateRequest, messageSubmissionData)
            Ok(Json.toJsObject(SubmissionValidationSuccess(messageSubmissionData)))

          case SubmissionValidationFailure(validationErrors) =>
            logger.warn("Failed to validate XML submission against schema")
            sendAuditEventForValidationErrors(validateRequest, validationErrors.errors)
            Ok(Json.toJson(SubmissionValidationFailure(validationErrors)))

          case InvalidXmlError(saxException) =>
            logger.warn(s"InvalidXmlError: $saxException")
            sendAuditEventForExceptions(validateRequest, invalidXmlErrorUrl, saxException)
            BadRequest(InvalidXmlError(saxException).toString)

          case _ =>
            logger.warn("Failed to validate submission due to unexpected outcome from validationEngine")
            val errorMessage = "An unexpected error occurred during XML validation"
            sendAuditEventForExceptions(validateRequest, internalServerErrorUrl, errorMessage)
            InternalServerError("Failed to validate submission")
        }
      case JsError(errors) =>
        logger.warn(s"Missing upscan URL: $errors")
        logger.warn(s"No Audit event triggered for request ${request.body}")
        Future.successful(InternalServerError("Missing upscan URL"))
    }
  }

  private def sendSuccessfulAuditEvent(validateRequest: ValidateRequest,
                                       messageSpecData: MessageSpecData
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: IdentifierRequest[JsValue]) = {
    val detail = AuditDetailForSubmissionValidation(
      conversationId = validateRequest.conversationId,
      subscriptionId = validateRequest.subscriptionId,
      fileReferenceId = validateRequest.fileReferenceId,
      messageRefId = Some(messageSpecData.messageRefId),
      messageTypeIndicator = Some(messageSpecData.messageTypeIndic.toString),
      reportingEntityName = Some(messageSpecData.reportingEntityName),
      reportType = Some(messageSpecData.reportType.toString),
      userType = request.affinityGroup.toString,
      fileError = false
    )
    sendAuditEvent(detail)
  }

  private def sendAuditEvent(detail: AuditDetailForSubmissionValidation)(implicit hc: HeaderCarrier, ec: ExecutionContext) =
    auditService.sendAuditEvent(AuditType.fileValidation, Json.toJson(Audit(detail)))

  private def sendAuditEventForValidationErrors(validateRequest: ValidateRequest,
                                                mappedValidationErrors: Seq[GenericError]
  )(implicit hc: HeaderCarrier, ec: ExecutionContext, request: IdentifierRequest[JsValue]) = {

    val detail = AuditDetailForSubmissionValidation(
      conversationId = validateRequest.conversationId,
      subscriptionId = validateRequest.subscriptionId,
      fileReferenceId = validateRequest.fileReferenceId,
      messageRefId = None,
      messageTypeIndicator = None,
      reportingEntityName = None,
      reportType = None,
      userType = request.affinityGroup.toString,
      fileError = true,
      errorMessage = Some("Failed to validate XML submission against schema"),
      errorURL = Some(dataErrorsUrl),
      validationErrors = Some(mappedValidationErrors)
    )
    sendAuditEvent(detail)
  }

  private def sendAuditEventForExceptions(validateRequest: ValidateRequest, errorUrl: String, errorMessage: String)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext,
    request: IdentifierRequest[JsValue]
  ) = {

    val detail = AuditDetailForSubmissionValidation(
      conversationId = validateRequest.conversationId,
      subscriptionId = validateRequest.subscriptionId,
      fileReferenceId = validateRequest.fileReferenceId,
      messageRefId = None,
      messageTypeIndicator = None,
      reportingEntityName = None,
      reportType = None,
      userType = request.affinityGroup.toString,
      fileError = true,
      errorMessage = Some(errorMessage),
      errorURL = Some(errorUrl),
      validationErrors = None
    )
    sendAuditEvent(detail)
  }
}
