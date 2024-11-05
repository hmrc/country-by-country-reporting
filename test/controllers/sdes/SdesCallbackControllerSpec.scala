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

import base.SpecBase
import generators.Generators
import models.agentSubscription.AgentContactDetails
import models.submission._
import org.mockito.ArgumentMatchers.{any, eq => is}
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.http.Status.{ACCEPTED, OK}
import play.api.inject.bind
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, route, status}
import repositories.submission.FileDetailsRepository
import services.EmailService
import services.audit.AuditService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.HttpVerbs.POST

import scala.concurrent.{ExecutionContext, Future}

class SdesCallbackControllerSpec extends SpecBase with BeforeAndAfterEach with ScalaCheckPropertyChecks with Generators {

  val mockFileDetailsRepository: FileDetailsRepository = mock[FileDetailsRepository]
  val mockEmailService: EmailService                   = mock[EmailService]
  val mockAuditService: AuditService                   = mock[AuditService]

  override def beforeEach(): Unit = {
    reset(mockFileDetailsRepository, mockEmailService)
    super.beforeEach()
  }

  val application: Application = applicationBuilder()
    .overrides(
      bind[FileDetailsRepository].toInstance(mockFileDetailsRepository),
      bind[EmailService].toInstance(mockEmailService),
      bind[AuditService].toInstance(mockAuditService)
    )
    .build()

  "SdesCallbackController" - {
    "must return Ok for success file notification" in {

      forAll(arbitrarySuccessSdesCallback.arbitrary, arbitrary[FileDetails]) { (sdesCallback, fileDetails) =>
        reset(mockAuditService)
        when(mockFileDetailsRepository.findByConversationId(sdesCallback.correlationID)).thenReturn(Future.successful(Some(fileDetails)))
        println(fileDetails)
        val request = FakeRequest(POST, routes.SdesCallbackController.callback.url).withBody(Json.toJson(sdesCallback))
        val result  = route(application, request).value
        status(result) mustEqual OK
        verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])

      }
    }

    "must return Ok for failure notification and update status and send email" in {
      forAll(arbitraryFailureSdesCallback.arbitrary, arbitraryPendingFileDetails.arbitrary) { (sdesCallback, fileDetails) =>
        reset(mockAuditService)
        val updatedStatus = sdesCallback.failureReason match {
          case Some(reason) if reason.toLowerCase.contains("virus") => RejectedSDESVirus
          case _                                                    => RejectedSDES
        }
        when(mockFileDetailsRepository.findByConversationId(sdesCallback.correlationID)).thenReturn(Future.successful(Some(fileDetails)))
        when(mockFileDetailsRepository.updateStatus(sdesCallback.correlationID.value, updatedStatus)).thenReturn(Future.successful(Some(fileDetails)))
        when(
          mockEmailService.sendAndLogEmail(any[String], any[String], any[String], any[Option[AgentContactDetails]], any[Boolean], any[ReportType])(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(Seq(ACCEPTED)))
        val request = FakeRequest(POST, routes.SdesCallbackController.callback.url).withBody(Json.toJson(sdesCallback))
        val result  = route(application, request).value

        status(result) mustEqual OK
        verify(mockFileDetailsRepository).updateStatus(sdesCallback.correlationID.value, updatedStatus)
        verify(mockEmailService, atLeast(1)).sendAndLogEmail(is(fileDetails.subscriptionId),
                                                             any[String],
                                                             is(fileDetails.messageRefId),
                                                             is(fileDetails.agentDetails),
                                                             is(false),
                                                             is(fileDetails.reportType)
        )(
          any[HeaderCarrier]
        )

      }
    }

    "must return Ok for failure notification but do not update status and neither send email if file status not pending" in {
      forAll(arbitraryFailureSdesCallback.arbitrary, arbitraryNonPendingFileDetails.arbitrary) { (sdesCallback, fileDetails) =>
        reset(mockAuditService)
        when(mockFileDetailsRepository.findByConversationId(sdesCallback.correlationID)).thenReturn(Future.successful(Some(fileDetails)))

        val request = FakeRequest(POST, routes.SdesCallbackController.callback.url).withBody(Json.toJson(sdesCallback))
        val result  = route(application, request).value

        status(result) mustEqual OK
        verify(mockAuditService, times(1)).sendAuditEvent(any[String](), any[JsValue]())(any[HeaderCarrier], any[ExecutionContext])
        verify(mockFileDetailsRepository, times(0)).updateStatus(any[String], any[FileStatus])
        verify(mockEmailService, times(0)).sendAndLogEmail(any[String],
                                                           any[String],
                                                           any[String],
                                                           any[Option[AgentContactDetails]],
                                                           any[Boolean],
                                                           any[ReportType]
        )(
          any[HeaderCarrier]
        )

      }
    }
  }
}
