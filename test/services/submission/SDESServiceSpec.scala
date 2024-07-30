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

import base.SpecBase
import connectors.SDESConnector
import generators.Generators
import models.error.SdesSubmissionError
import models.sdes._
import models.submission._
import models.subscription.ResponseDetail
import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.IntegrationPatience
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.inject.bind
import services.SubscriptionService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import scala.concurrent.{ExecutionContext, Future}

class SDESServiceSpec extends SpecBase with IntegrationPatience with Generators with ScalaCheckDrivenPropertyChecks {

  val mockSubscriptionService: SubscriptionService = mock[SubscriptionService]
  val mockSDESConnector: SDESConnector             = mock[SDESConnector]

  override def beforeEach(): Unit =
    reset(mockSDESConnector, mockSubscriptionService)

  override lazy val app: Application = applicationBuilder()
    .overrides(
      bind[SDESConnector].toInstance(mockSDESConnector),
      bind[SubscriptionService].toInstance(mockSubscriptionService),
      bind[Clock].toInstance(fixedClock)
    )
    .build()

  private val sdesService = app.injector.instanceOf[SDESService]

  "SDESService" - {
    "sendFileNotification" - {
      "must send file submission notification to SDES connector and return success status when connector returns a Right" in {
        forAll { (responseDetail: ResponseDetail, submissionDetails: SubmissionDetails) =>
          mockSdesConnectorWithResponse(Future.successful(Right(NO_CONTENT)))
          val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)

          val result = sdesService.sendFileNotification(submissionDetails, responseDetail, conversationId).futureValue

          result.value mustBe conversationId
        }
      }

      "must send file submission to SDES connector and return error status when connector returns a Left" in {
        forAll { (responseDetail: ResponseDetail, submissionDetails: SubmissionDetails) =>
          mockSdesConnectorWithResponse(Future.successful(Left(INTERNAL_SERVER_ERROR)))
          val conversationId = ConversationId.fromUploadId(submissionDetails.uploadId)

          val result = sdesService.sendFileNotification(submissionDetails, responseDetail, conversationId).futureValue

          result.left.value mustBe SdesSubmissionError(INTERNAL_SERVER_ERROR)
        }
      }
    }

  }

  private def mockSdesConnectorWithResponse(response: Future[Either[Int, Int]]): Any =
    when(mockSDESConnector.sendFileNotification(any[FileTransferNotification])(any[HeaderCarrier], any[ExecutionContext]))
      .thenReturn(response)
}
