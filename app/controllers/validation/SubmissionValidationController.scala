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

import controllers.auth.IdentifierAuthAction
import models.audit.{AuditType, AuditWithUserType}
import models.upscan.UpscanURL
import models.validation.{InvalidXmlError, SubmissionValidationFailure, SubmissionValidationSuccess}
import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import services.audit.AuditService
import services.validation.UploadedXmlValidationEngine
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionValidationController @Inject() (
  authenticate: IdentifierAuthAction,
  cc: ControllerComponents,
  validationEngine: UploadedXmlValidationEngine,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def validateSubmission: Action[JsValue] = authenticate(parse.json).async { implicit request =>
    request.body.validate[UpscanURL] match {
      case JsSuccess(upscanURL, _) =>
        validationEngine.validateUploadSubmission(upscanURL.url) map {
          case SubmissionValidationSuccess(msd) =>
            Ok(Json.toJsObject(SubmissionValidationSuccess(msd)))

          case SubmissionValidationFailure(errors) =>
            val audit = AuditWithUserType(SubmissionValidationFailure(errors), Some(request.affinityGroup))
            auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(audit))
            Ok(Json.toJson(SubmissionValidationFailure(errors)))

          case InvalidXmlError(saxException) =>
            val audit = AuditWithUserType(InvalidXmlError(saxException), Some(request.affinityGroup))
            auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(audit))
            logger.warn("InvalidXmlError: Failed to validate xml submission")
            BadRequest(InvalidXmlError(saxException).toString)

          case _ =>
            val audit = AuditWithUserType(InvalidXmlError("Failed to validate xml submission"), Some(request.affinityGroup))
            auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(audit))
            logger.warn("Failed to validate xml submission")
            InternalServerError("failed to validateSubmission")
        }
      case JsError(errors) =>
        logger.warn(s"Missing upscan URL: $errors")
        val audit = AuditWithUserType(InvalidXmlError(s"Missing upscan URL: $errors"), Some(request.affinityGroup))
        auditService.sendAuditEvent(AuditType.fileSubmission, Json.toJson(audit))
        Future.successful(InternalServerError("Missing upscan URL"))
    }
  }
}
