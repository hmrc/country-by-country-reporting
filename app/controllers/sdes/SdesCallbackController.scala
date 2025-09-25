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

package controllers.sdes

import models.audit.AuditType.sdesResponse
import models.audit.{Audit, SDESAuditResponse}
import models.sdes.NotificationType.FileProcessingFailure
import models.sdes.SdesCallback
import models.submission.{Pending, RejectedSDES, RejectedSDESVirus}
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import repositories.submission.FileDetailsRepository
import services.EmailService
import services.audit.AuditService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import utils.DateTimeFormatUtil

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SdesCallbackController @Inject() (
  cc: ControllerComponents,
  fileDetailsRepository: FileDetailsRepository,
  emailService: EmailService,
  auditService: AuditService
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  val callback: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[SdesCallback] { sdesCallback: SdesCallback =>
      logger.info(
        s"SDESCallbackController: Received SDES ${sdesCallback.notification} callback for file: ${sdesCallback.filename} (${sdesCallback.correlationID})"
      )
      fileDetailsRepository
        .findByConversationId(sdesCallback.correlationID)
        .flatMap {
          case Some(fileDetails) =>
            val auditDetails = SDESAuditResponse(
              sdesCallback,
              Some(fileDetails)
            )
            auditService
              .sendAuditEvent(sdesResponse, Json.toJson(Audit(auditDetails, userType = fileDetails.userType)))
          case None =>
            auditService.sendAuditEvent(sdesResponse,
                                        Json.toJson(
                                          Audit(SDESAuditResponse(
                                                  sdesCallback,
                                                  None
                                                ),
                                                error = Some("No file details found")
                                          )
                                        )
            )
        }
        .recoverWith { case e: Exception =>
          logger.error(s"Failed to get fileDetails: ${e.getMessage}")
          auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(sdesCallback, error = Some("Exception occurred while retrieving file details"))))
        }
      sdesCallback match {
        case SdesCallback(FileProcessingFailure, _, _, _, correlationID, _, failureReason) =>
          fileDetailsRepository.findByConversationId(correlationID) flatMap {
            case Some(fileDetails) if fileDetails.status == Pending =>
              val updatedStatus = failureReason match {
                case Some(reason) if reason.toLowerCase.contains("virus") => RejectedSDESVirus
                case _                                                    => RejectedSDES
              }
              fileDetailsRepository.updateStatus(correlationID.value, updatedStatus) map { _ =>
                emailService.sendAndLogEmail(
                  fileDetails.subscriptionId,
                  DateTimeFormatUtil.displayFormattedDate(fileDetails.submitted),
                  fileDetails.messageRefId,
                  fileDetails.agentDetails,
                  isUploadSuccessful = false,
                  fileDetails.reportType,
                  DateTimeFormatUtil.displayFormattedDate(fileDetails.reportingPeriodStartDate),
                  DateTimeFormatUtil.displayFormattedDate(fileDetails.reportingPeriodEndDate),
                  fileDetails.reportingEntityName
                )
                logger.info(s"Updated status for conversationId: $correlationID to $updatedStatus")
                Ok
              }
            case Some(fileDetails) if fileDetails.status != Pending =>
              val auditDetails = SDESAuditResponse(
                sdesCallback,
                Some(fileDetails)
              )
              logger.warn(s"File with conversationId: $correlationID is not in pending state")
              auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(auditDetails, error = Some("File is not in pending state"))))
              Future.successful(Ok)
            case _ =>
              logger.error(s"No record found for the conversationId: $correlationID")
              auditService.sendAuditEvent(
                sdesResponse,
                Json.toJson(Audit(SDESAuditResponse(sdesCallback, None), error = Some(s"No file details found for the conversationId: $correlationID")))
              )
              Future.successful(Ok)
          }
        case _ =>
          Future.successful(Ok)
      }
    }
  }
}
