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

package services.submission

import config.AppConfig
import connectors.SDESConnector
import models.error.{BackendError, SdesSubmissionError}
import models.sdes._
import models.submission.{ConversationId, SubmissionDetails}
import models.subscription.ResponseDetail
import play.api.Logging
import repositories.submission.FileDetailsRepository
import services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.DateTimeFormatUtil

import java.time.Clock
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SDESService @Inject() (
  sdesConnector: SDESConnector,
  readSubscriptionService: SubscriptionService,
  fileDetailsRepository: FileDetailsRepository,
  appConfig: AppConfig,
  metaDataService: SDESFileMetadataService
)(implicit ec: ExecutionContext, clock: Clock)
    extends Logging {

  def sendFileNotification(
    submissionDetails: SubmissionDetails,
    subscriptionDetails: ResponseDetail,
    conversationId: ConversationId
  )(implicit hc: HeaderCarrier): Future[Either[BackendError, ConversationId]] = {

    logger.info(s"Sending large file with conversation Id [${conversationId.value}] to SDES")
    val submissionTime = DateTimeFormatUtil.zonedDateTimeNow.toLocalDateTime
    val metaData       = metaDataService.compileMetadata(subscriptionDetails, conversationId, submissionTime, submissionDetails.fileName)

    val fileNotificationRequest = createFileNotificationRequest(submissionDetails, conversationId, metaData)

    sdesConnector.sendFileNotification(fileNotificationRequest).map {
      case Right(_) => Right(conversationId)
      case Left(errorStatus) =>
        logger.warn(s"Failed to send file with conversation Id [${conversationId.value}] to SDES. Got error status: $errorStatus")
        Left(SdesSubmissionError(errorStatus))
    }
  }

  def createFileNotificationRequest(
    submissionDetails: SubmissionDetails,
    correlationId: ConversationId,
    metaData: Map[String, String]
  ): FileTransferNotification =
    FileTransferNotification(
      appConfig.sdesInformationType,
      File(
        Option(appConfig.sdesRecipientOrSender),
        submissionDetails.fileName,
        Option(submissionDetails.documentUrl),
        Checksum(appConfig.sdesChecksumAlgorithm, submissionDetails.checksum),
        submissionDetails.fileSize.toInt,
        mapToProperty(metaData)
      ),
      Audit(correlationId.value)
    )

  private def mapToProperty(metaData: Map[String, String]): List[Property] =
    metaData.toList.map { case (name, value) => Property(name, value) }
}
