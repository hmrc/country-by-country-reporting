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

package controllers

import config.AppConfig
import controllers.actions.EISResponsePreConditionCheckActionRefiner
import controllers.auth.ValidateAuthTokenAction
import models.audit.{Audit, AuditType}
import models.submission.{Accepted => FileStatusAccepted, ConversationId, FileStatus, Rejected}
import models.xml.{BREResponse, ValidationStatus}
import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import repositories.submission.FileDetailsRepository
import services.EmailService
import services.audit.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.{CustomAlertUtil, DateTimeFormatUtil}

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq

class EISResponseController @Inject() (
  cc: ControllerComponents,
  validateAuth: ValidateAuthTokenAction,
  actionRefiner: EISResponsePreConditionCheckActionRefiner,
  fileDetailsRepository: FileDetailsRepository,
  emailService: EmailService,
  appConfig: AppConfig,
  customAlertUtil: CustomAlertUtil,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  private def convertToFileStatus(breResponse: BREResponse): FileStatus =
    breResponse.genericStatusMessage.status match {
      case ValidationStatus.accepted => FileStatusAccepted
      case _ =>
        customAlertUtil.alertForProblemStatus(breResponse.genericStatusMessage.validationErrors)
        Rejected(breResponse.genericStatusMessage.validationErrors)
    }

  def processEISResponse(): Action[NodeSeq] = (Action(parse.xml) andThen validateAuth andThen actionRefiner).async { implicit request =>
    fileDetailsRepository
      .findByConversationId(ConversationId(request.BREResponse.conversationID))
      .flatMap {
        case Some(fileDetails) =>
          val audit = Audit(request.BREResponse, userType = fileDetails.userType)
          auditService.sendAuditEvent(AuditType.eisResponse, Json.toJson(audit))
        case _ =>
          auditService.sendAuditEvent(AuditType.eisResponseError, Json.toJson(Audit(request.BREResponse, error = Some("File details not found"))))
      }
      .recoverWith { case e: Exception =>
        logger.error(s"Failed to get fileDetails: ${e.getMessage}")
        auditService.sendAuditEvent(AuditType.eisResponseError,
                                    Json.toJson(Audit(request.BREResponse, error = Some(s"Failed to get file details: ${e.getMessage}")))
        )
      }
    val conversationId = request.BREResponse.conversationID
    val fileStatus     = convertToFileStatus(request.BREResponse)

    fileDetailsRepository.updateStatus(conversationId, fileStatus) map {
      case Some(updatedFileDetails) =>
        val fastJourney = updatedFileDetails.lastUpdated.isBefore(updatedFileDetails.submitted.plusSeconds(appConfig.eisResponseWaitTime))

        (fastJourney, updatedFileDetails.status) match {
          case (_, FileStatusAccepted) | (false, Rejected(_)) =>
            emailService.sendAndLogEmail(
              updatedFileDetails.subscriptionId,
              DateTimeFormatUtil.displayFormattedDate(updatedFileDetails.submitted),
              updatedFileDetails.messageRefId,
              updatedFileDetails.agentDetails,
              updatedFileDetails.status == FileStatusAccepted,
              updatedFileDetails.reportType
            )
          case _ =>
            logger.warn("Upload file status is rejected on fast journey. No email has been sent")
        }
        NoContent
      case _ =>
        logger.warn("Failed to update the status:mongo error")
        auditService.sendAuditEvent(AuditType.eisResponseError,
                                    Json.toJson(Audit(request.BREResponse, error = Some("Failed to update the status: mongo error")))
        )
        InternalServerError
    }
  }
}
