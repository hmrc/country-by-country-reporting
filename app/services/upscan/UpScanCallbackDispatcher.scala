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

import models.audit.Audit
import models.audit.AuditType.{sdesResponse, sdesResponseError}

import javax.inject.Inject
import models.upscan._
import play.api.Logging
import play.api.libs.json.Json
import services.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class UpScanCallbackDispatcher @Inject() (sessionStorage: UploadProgressTracker, auditService: AuditService)(implicit
  val ec: ExecutionContext
) extends Logging {

  def handleCallback(callback: CallbackBody)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val uploadStatus = callback match {
      case s: ReadyCallbackBody =>
        auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(s)))
        UploadedSuccessfully(
          s.uploadDetails.fileName,
          s.uploadDetails.fileMimeType,
          s.downloadUrl,
          Option(s.uploadDetails.size),
          Option(s.uploadDetails.checksum)
        )
      case s: FailedCallbackBody if s.failureDetails.failureReason == "QUARANTINE" =>
        logger.warn(s"FailedCallbackBody, QUARANTINE: $s")
        auditService.sendAuditEvent(sdesResponseError, Json.toJson(Audit(s)))
        Quarantined
      case s: FailedCallbackBody if s.failureDetails.failureReason == "REJECTED" =>
        logger.warn(s"FailedCallbackBody, REJECTED: $s")
        auditService.sendAuditEvent(sdesResponseError, Json.toJson(Audit(s)))
        UploadRejected(s.failureDetails)
      case f: FailedCallbackBody =>
        logger.warn(s"FailedCallbackBody: $f")
        auditService.sendAuditEvent(sdesResponse, Json.toJson(Audit(f)))
        Failed
    }
    sessionStorage.registerUploadResult(callback.reference, uploadStatus)
  }

}
