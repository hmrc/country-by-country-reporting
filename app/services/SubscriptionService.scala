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

import connectors.SubscriptionConnector
import models.audit.{Audit, AuditDetailForUpdateOrgSubscriptionRequest}
import models.audit.AuditType.updateContactDetails
import models.error.{BackendError, ReadSubscriptionError, UpdateSubscriptionError}
import models.subscription._
import play.api.Logging
import play.api.http.Status.OK
import play.api.libs.json.Json
import services.audit.AuditService
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionService @Inject() (subscriptionConnector: SubscriptionConnector, auditService: AuditService) extends Logging {

  def getContactInformation(enrolmentId: String)(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Either[BackendError, ResponseDetail]] = {

    val subscriptionRequest: DisplaySubscriptionForCBCRequest =
      DisplaySubscriptionForCBCRequest(
        DisplaySubscriptionDetails(
          RequestCommonForSubscription(),
          ReadSubscriptionRequestDetail(enrolmentId)
        )
      )

    subscriptionConnector.readSubscriptionInformation(subscriptionRequest).map { response =>
      response.status match {
        case OK =>
          val responseDetail = response.json.as[DisplaySubscriptionForCBCResponse].displaySubscriptionForCBCResponse.responseDetail
          Right(responseDetail)
        case status =>
          logger.warn(s"Read subscription Got Status $status")
          Left(ReadSubscriptionError(status))
      }
    }
  }

  def updateSubscription(
    requestDetailForUpdate: RequestDetailForUpdate
  )(implicit hc: HeaderCarrier, ex: ExecutionContext): Future[Either[UpdateSubscriptionError, Unit]] =
    subscriptionConnector.updateSubscription(UpdateSubscriptionForCBCRequest(requestDetailForUpdate)).map { res =>
      res.status match {
        case OK =>
          val audit = Audit(AuditDetailForUpdateOrgSubscriptionRequest(requestDetailForUpdate), Some(AffinityGroup.Organisation))
          auditService.sendAuditEvent(updateContactDetails, Json.toJson(audit))
          Right(())
        case status =>
          logger.warn(s"Update Subscription Got Status $status")
          Left(UpdateSubscriptionError(status))
      }
    }
}
