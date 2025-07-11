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

package services.upscan

import models.audit.AuditType.fileValidation
import models.audit.{Audit, AuditDetailForSubmissionValidation}
import models.upscan._
import play.api.Logging
import play.api.libs.json.Json
import services.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UpScanCallbackDispatcher @Inject() (sessionStorage: UploadProgressTracker, auditService: AuditService)(implicit
  val ec: ExecutionContext
) extends Logging {

  private val notXmlErrorUrl         = "/problem/not-xml"
  private val virusErrorUrl          = "/problem/virus-found"
  private val internalServerErrorUrl = "/problem/there-is-a-problem"

  def handleCallback(callback: CallbackBody, uploadId: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val uploadStatus = callback match {
      case s: ReadyCallbackBody =>
        UploadedSuccessfully(
          s.uploadDetails.fileName,
          s.uploadDetails.fileMimeType,
          s.downloadUrl,
          Option(s.uploadDetails.size),
          Option(s.uploadDetails.checksum)
        )

      case q: FailedCallbackBody if q.failureDetails.failureReason == "QUARANTINE" =>
        logger.warn(s"FailedCallbackBody, QUARANTINE: ${q.reference.value}")
        triggerAuditEvent(uploadId, q, virusErrorUrl)
        Quarantined

      case r: FailedCallbackBody if r.failureDetails.failureReason == "REJECTED" =>
        logger.warn(s"FailedCallbackBody, REJECTED: ${r.reference.value}")
        triggerAuditEvent(uploadId, r, notXmlErrorUrl)
        UploadRejected(r.failureDetails)

      case f: FailedCallbackBody =>
        logger.warn(s"FailedCallbackBody: ${f.reference.value}")
        triggerAuditEvent(uploadId, f, internalServerErrorUrl)
        Failed
    }

    sessionStorage.registerUploadResult(callback.reference, uploadStatus)
  }

  private def triggerAuditEvent(uploadId: String, callbackBody: FailedCallbackBody, problemUrl: String)(implicit headerCarrier: HeaderCarrier) = {
    val details = AuditDetailForSubmissionValidation(
      conversationId = uploadId,
      subscriptionId = "UNKNOWN ID", // SubscriptionID not available in the callback
      messageRefId = None,
      messageTypeIndicator = None,
      reportingEntityName = None,
      reportType = None,
      userType = "UNKNOWN", // userType is not in the callback
      fileError = true,
      errorMessage = Some(s"${callbackBody.failureDetails.failureReason} : ${callbackBody.failureDetails.message}"),
      errorURL = Some(problemUrl),
      validationErrors = None
    )
    auditService.sendAuditEvent(fileValidation, Json.toJson(Audit(details)))
  }
}
