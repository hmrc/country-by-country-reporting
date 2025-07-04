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

package services

import base.SpecBase
import connectors.SubscriptionConnector
import models.audit.AuditType.updateContactDetails
import models.audit.{Audit, AuditDetailForUpdateOrgSubscriptionRequest}
import models.error.{ReadSubscriptionError, UpdateSubscriptionError}
import models.subscription.{DisplaySubscriptionForCBCRequest, RequestDetailForUpdate, UpdateSubscriptionForCBCRequest}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status._
import play.api.inject.bind
import play.api.libs.json.Json
import services.audit.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult.{Failure, Success}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionServiceSpec extends SpecBase with BeforeAndAfterEach {

  val mockSubscriptionConnector = mock[SubscriptionConnector]

  val mockAuditService            = mock[AuditService]
  override def beforeEach(): Unit = reset(mockSubscriptionConnector, mockAuditService)

  "SubscriptionService" - {
    val application = applicationBuilder()
      .overrides(
        bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
        bind[AuditService].toInstance(mockAuditService)
      )
      .build()

    val requestDetailJson = Json.parse("""
        |{
        |      "IDType": "SAFE",
        |      "IDNumber": "IDNumber",
        |      "tradingName": "Trading Name",
        |      "isGBUser": true,
        |      "primaryContact":
        |        {
        |          "organisation": {
        |            "organisationName": "orgName1"
        |          },
        |          "email": "test@email.com",
        |          "phone": "+4411223344"
        |        },
        |      "secondaryContact":
        |        {
        |          "organisation": {
        |            "organisationName": "orgName2"
        |          },
        |          "email": "test@email.com",
        |          "phone": "+4411223344"
        |        }
        |}
        |""".stripMargin)
    val requestDetailForUpdate = requestDetailJson.as[RequestDetailForUpdate]

    "must correctly retrieve subscription from connector" in {
      val service = application.injector.instanceOf[SubscriptionService]
      val subscriptionResponseJson: String =
        """
          |{
          |"displaySubscriptionForCBCResponse": {
          |"responseCommon": {
          |"status": "OK",
          |"processingDate": "2020-08-09T11:23:45Z"
          |},
          |"responseDetail": {
          |"subscriptionID": "111111111",
          |"tradingName": "",
          |"isGBUser": true,
          |"primaryContact": [
          |{
          |"email": "",
          |"phone": "",
          |"mobile": "",
          |"organisation": {
          |"organisationName": "orgName"
          |}
          |}
          |],
          |"secondaryContact": [
          |{
          |"email": "",
          |"organisation": {
          |"organisationName": ""
          |}
          |}
          |]
          |}
          |}
          |}""".stripMargin

      when(mockSubscriptionConnector.readSubscriptionInformation(any[DisplaySubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(OK, subscriptionResponseJson)))

      val result = service.getContactInformation("111111111")

      whenReady(result) { _ =>
        verify(mockSubscriptionConnector, times(1)).readSubscriptionInformation(any[DisplaySubscriptionForCBCRequest]())(any[HeaderCarrier](),
                                                                                                                         any[ExecutionContext]()
        )
      }
    }

    "must retrieve ReadSubscriptionError from connector when not ok status" in {
      val service = application.injector.instanceOf[SubscriptionService]

      when(mockSubscriptionConnector.readSubscriptionInformation(any[DisplaySubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val result = service.getContactInformation("111111111")

      whenReady(result) { sub =>
        sub mustBe Left(ReadSubscriptionError(500))
        verify(mockSubscriptionConnector, times(1)).readSubscriptionInformation(any[DisplaySubscriptionForCBCRequest]())(any[HeaderCarrier](),
                                                                                                                         any[ExecutionContext]()
        )
      }
    }

    "must  return UpdateSubscription with OK status when connector & audit service response with ok status" in {
      val auditDetail = Json.parse("""
                                     |{
                                     |      "subscriptionId": "IDNumber",
                                     |      "reportingEntityName": "Trading Name",
                                     |      "firstContactName": "orgName1",
                                     |      "firstContactEmail": "test@email.com",
                                     |      "firstContactPhoneNumber": "+4411223344",
                                     |      "hasSecondContact": true,
                                     |      "secondContactName": "orgName2",
                                     |      "secondContactEmail": "test@email.com",
                                     |      "secondContactPhoneNumber": "+4411223344"
                                     |}
                                     |""".stripMargin)
      val auditDetailRequest   = auditDetail.as[AuditDetailForUpdateOrgSubscriptionRequest]
      val expectedAuditRequest = Json.toJson(Audit(auditDetailRequest, Some(AffinityGroup.Organisation)))

      val service = application.injector.instanceOf[SubscriptionService]

      when(mockSubscriptionConnector.updateSubscription(any[UpdateSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(OK, "Good Response")))
      when(mockAuditService.sendAuditEvent(any(), any())(any(), any())).thenReturn(Future.successful(Success))

      val result = service.updateSubscription(requestDetailForUpdate)

      whenReady(result) { sub =>
        verify(mockSubscriptionConnector, times(1)).updateSubscription(any[UpdateSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
        verify(mockAuditService, times(1)).sendAuditEvent(ArgumentMatchers.eq(updateContactDetails), ArgumentMatchers.eq(expectedAuditRequest))(any(), any())
        sub mustBe Right(())
      }
    }

    "must  return UpdateSubscription with OK status when connector response with ok and audit service returns failure" in {
      val service = application.injector.instanceOf[SubscriptionService]

      when(mockSubscriptionConnector.updateSubscription(any[UpdateSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(OK, "Good Response")))
      when(mockAuditService.sendAuditEvent(any(), any())(any(), any())).thenReturn(Future.successful(Failure("Failed")))

      val result = service.updateSubscription(requestDetailForUpdate)

      whenReady(result) { sub =>
        verify(mockSubscriptionConnector, times(1)).updateSubscription(any[UpdateSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
        verify(mockAuditService, times(1)).sendAuditEvent(any(), any())(any(), any())
        sub mustBe Right(())
      }
    }

    "must have UpdateSubscriptionError when connector response with not ok status" in {
      val service = application.injector.instanceOf[SubscriptionService]

      when(mockSubscriptionConnector.updateSubscription(any[UpdateSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]()))
        .thenReturn(Future.successful(HttpResponse(INTERNAL_SERVER_ERROR, "")))

      val result = service.updateSubscription(requestDetailForUpdate)

      whenReady(result) { sub =>
        verify(mockSubscriptionConnector, times(1)).updateSubscription(any[UpdateSubscriptionForCBCRequest]())(any[HeaderCarrier](), any[ExecutionContext]())
        sub mustBe Left(UpdateSubscriptionError(500))
      }
    }
  }

}
