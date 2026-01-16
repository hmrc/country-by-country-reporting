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
import models.audit.{Audit, AuditDetailForEISResponse, AuditType}
import models.submission.{Accepted as FileStatusAccepted, ConversationId, FileStatus, Rejected}
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
import scala.concurrent.{ExecutionContext, Future}
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
    val found = fileDetailsRepository
      .findByConversationId(ConversationId(request.BREResponse.conversationID))
      .flatMap {
        case Some(fileDetails) =>
          val auditDetails = AuditDetailForEISResponse(request.BREResponse, Some(fileDetails))
          val audit        = Audit(auditDetails, userType = fileDetails.userType)
          auditService.sendAuditEvent(AuditType.eisResponse, Json.toJson(audit)).map(_ => true)
        case _ =>
          logger.warn(s"File details not found for ConversationID: ${request.BREResponse.conversationID}")
          val auditDetails = AuditDetailForEISResponse(request.BREResponse, None)
          auditService.sendAuditEvent(AuditType.eisResponse, Json.toJson(Audit(auditDetails, error = Some("File details not found")))).map(_ => false)
      }
      .recoverWith { case e: Exception =>
        val errorMessage = s"Failed to get file details: ${e.getMessage}"
        logger.error(errorMessage)
        val auditDetails = AuditDetailForEISResponse(request.BREResponse, None)
        auditService.sendAuditEvent(AuditType.eisResponse, Json.toJson(Audit(auditDetails, error = Some(errorMessage)))).map(_ => false)
      }
    val conversationId = request.BREResponse.conversationID
    val fileStatus     = convertToFileStatus(request.BREResponse)

    found.flatMap {
      case true =>
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
                  updatedFileDetails.reportType,
                  updatedFileDetails.reportingPeriodStartDate.toString,
                  updatedFileDetails.reportingPeriodEndDate.toString,
                  updatedFileDetails.reportingEntityName
                )
              case _ =>
                logger.warn(
                  s"Upload file status is rejected on fast journey. No email has been sent: lastUpdated: ${updatedFileDetails.lastUpdated}, submitted: ${updatedFileDetails.submitted}"
                )
            }
            NoContent
          case _ =>
            logger.error("Failed to update the status:mongo error - when trying to update status for ConversationID: " + conversationId + " to " + fileStatus)
            val auditDetails = AuditDetailForEISResponse(request.BREResponse, None)
            auditService.sendAuditEvent(AuditType.eisResponse, Json.toJson(Audit(auditDetails, error = Some("Failed to update the status: mongo error"))))
            NoContent // We want to always respond with Success to EIS
        }
      case false =>
        Future.successful(NoContent)
    }
  }
}
