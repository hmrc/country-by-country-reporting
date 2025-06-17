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
import models.sdes.NotificationType.{FileProcessed, FileProcessingFailure}
import models.sdes.SdesCallback
import models.submission
import models.submission._
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
              conversationId = Some(sdesCallback.correlationID.value),
              subscriptionId = Some(fileDetails.subscriptionId),
              messageRefId = Some(fileDetails.messageRefId),
              fileError = sdesCallback.failureReason
            )
            auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(auditDetails, userType = fileDetails.userType)))

            sdesCallback.notification match {
              case FileProcessingFailure =>
                val updatedStatus = sdesCallback.failureReason match {
                  case Some(reason) if reason.toLowerCase.contains("virus") =>
                    auditService.sendAuditEvent(
                      sdesResponse,
                      Json.toJson(
                        Audit(
                          SDESAuditResponse(
                            sdesCallback,
                            conversationId = Some(sdesCallback.correlationID.value),
                            subscriptionId = Some(fileDetails.subscriptionId),
                            messageRefId = Some(fileDetails.messageRefId),
                            error = Some(sdesCallback.failureReason.getOrElse("Virus scan failed")),
                            fileError = sdesCallback.failureReason
                          ),
                          userType = fileDetails.userType
                        )
                      )
                    )
                    RejectedSDESVirus

                  case _ => RejectedSDES
                }

                if (fileDetails.status == Pending) {
                  fileDetailsRepository.updateStatus(sdesCallback.correlationID.value, updatedStatus) map { _ =>
                    emailService.sendAndLogEmail(
                      fileDetails.subscriptionId,
                      DateTimeFormatUtil.displayFormattedDate(fileDetails.submitted),
                      fileDetails.messageRefId,
                      fileDetails.agentDetails,
                      isUploadSuccessful = false,
                      fileDetails.reportType
                    )
                    logger.info(s"Updated status for conversationId: ${sdesCallback.correlationID} to $updatedStatus")
                    Ok
                  }
                } else {
                  logger.warn(s"File with conversationId: ${sdesCallback.correlationID} is not in pending state. Current status: ${fileDetails.status}")
                  Future.successful(Ok)
                }

              case FileProcessed =>
                if (fileDetails.status == Pending) {
                  fileDetailsRepository.updateStatus(sdesCallback.correlationID.value, submission.Accepted) map { _ =>
                    emailService.sendAndLogEmail(
                      fileDetails.subscriptionId,
                      DateTimeFormatUtil.displayFormattedDate(fileDetails.submitted),
                      fileDetails.messageRefId,
                      fileDetails.agentDetails,
                      isUploadSuccessful = true,
                      fileDetails.reportType
                    )
                    logger.info(s"Updated status for conversationId: ${sdesCallback.correlationID} to Accepted")
                    Ok
                  }
                } else {
                  logger.info(s"File with conversationId: ${sdesCallback.correlationID} already processed. Current status: ${fileDetails.status}")
                  Future.successful(Ok)
                }

              case _ =>
                val auditDetails = SDESAuditResponse(
                  sdesCallback,
                  conversationId = Some(sdesCallback.correlationID.value),
                  subscriptionId = Some(fileDetails.subscriptionId),
                  messageRefId = Some(fileDetails.messageRefId),
                  error = Some(s"Unsupported notification type: ${sdesCallback.notification.toString}")
                )
                auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(auditDetails, userType = fileDetails.userType)))
                logger.warn(s"Received unsupported SDES notification type: ${sdesCallback.notification} for ${sdesCallback.correlationID}")
                Future.successful(Ok)
            }

          case _ =>
            val auditDetails = SDESAuditResponse(
              sdesCallback,
              error = Some(s"No file details found for conversationId: ${sdesCallback.correlationID.value}")
            )
            auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(auditDetails)))
            logger.error(s"No record found for conversationId: ${sdesCallback.correlationID}")
            Future.successful(Ok)
        }
        .recoverWith { case e: Exception =>
          logger.error(s"SDESCallbackController: Failed to process SDES callback for ${sdesCallback.correlationID}: ${e.getMessage}")
          val auditDetails = SDESAuditResponse(
            sdesCallback,
            error = Some(s"SDESCallbackController processing error: ${e.getMessage}")
          )
          auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(auditDetails)))
          Future.successful(Ok)
        }
    }
  }
}
