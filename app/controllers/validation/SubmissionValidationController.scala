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
import models.audit.{Audit, AuditType}
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
            auditService.sendAuditEvent(AuditType.fileValidation, Json.toJson(msd))
            Ok(Json.toJsObject(SubmissionValidationSuccess(msd)))

          case SubmissionValidationFailure(errors) =>
            val audit = Audit(SubmissionValidationFailure(errors), userType = Some(request.affinityGroup), error = Some("Failed to validate xml submission"))
            auditService.sendAuditEvent(AuditType.fileValidationError, Json.toJson(audit))
            logger.warn("Failed to validate xml submission")
            Ok(Json.toJson(SubmissionValidationFailure(errors)))

          case InvalidXmlError(saxException) =>
            val audit = Audit(InvalidXmlError(saxException), userType = Some(request.affinityGroup), error = Some("Invalid XML"))
            auditService.sendAuditEvent(AuditType.fileValidationError, Json.toJson(audit))
            logger.warn("InvalidXmlError: Failed to validate xml submission")
            BadRequest(InvalidXmlError(saxException).toString)

          case _ =>
            val audit = Audit(
              InvalidXmlError("Failed to validate xml submission"),
              userType = Some(request.affinityGroup),
              error = Some("Failed to validate xml submission")
            )
            auditService.sendAuditEvent(AuditType.fileValidationError, Json.toJson(audit))
            logger.warn("Failed to validate xml submission")
            InternalServerError("failed to validateSubmission")
        }
      case JsError(errors) =>
        logger.warn(s"Missing upscan URL: $errors")
        val audit = Audit(InvalidXmlError(s"Missing upscan URL: $errors"), userType = Some(request.affinityGroup), error = Some("Missing upscan URL"))
        auditService.sendAuditEvent(AuditType.fileValidationError, Json.toJson(audit))
        logger.warn("Missing upscan URL")
        Future.successful(InternalServerError("Missing upscan URL"))
    }
  }
}
