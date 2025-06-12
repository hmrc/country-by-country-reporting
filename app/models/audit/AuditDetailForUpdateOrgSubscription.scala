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

package models.audit

import models.subscription.{ContactInformation, RequestDetailForUpdate}
import play.api.libs.json._

case class AuditDetailForUpdateOrgSubscriptionRequest(subscriptionId: String,
                                                      reportingEntityName: String,
                                                      firstContactName: String,
                                                      firstContactEmail: String,
                                                      firstContactPhoneNumber: Option[String],
                                                      hasSecondContact: Boolean,
                                                      secondContactName: Option[String],
                                                      secondContactEmail: Option[String],
                                                      secondContactPhoneNumber: Option[String]
)

object AuditDetailForUpdateOrgSubscriptionRequest {
  implicit val format: OFormat[AuditDetailForUpdateOrgSubscriptionRequest] = Json.format[AuditDetailForUpdateOrgSubscriptionRequest]

  def apply(requestDetailForUpdate: RequestDetailForUpdate): AuditDetailForUpdateOrgSubscriptionRequest = {
    val primaryContact: ContactInformation           = requestDetailForUpdate.primaryContact
    val secondaryContact: Option[ContactInformation] = requestDetailForUpdate.secondaryContact

    new AuditDetailForUpdateOrgSubscriptionRequest(
      subscriptionId = requestDetailForUpdate.IDNumber,
      reportingEntityName = requestDetailForUpdate.tradingName.getOrElse(""),
      firstContactName = primaryContact.organisationDetails.organisationName,
      firstContactEmail = primaryContact.email,
      firstContactPhoneNumber = primaryContact.phone,
      hasSecondContact = secondaryContact.isDefined,
      secondContactName = if (secondaryContact.isDefined) Some(secondaryContact.get.organisationDetails.organisationName) else None,
      secondContactEmail = if (secondaryContact.isDefined) Some(secondaryContact.get.email) else None,
      secondContactPhoneNumber = if (secondaryContact.isDefined) secondaryContact.get.phone else None
    )
  }

}
