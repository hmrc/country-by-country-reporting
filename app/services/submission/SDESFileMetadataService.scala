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

import models.submission.ConversationId
import models.subscription.{ContactInformation, ResponseDetail}
import play.api.Logging

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SDESFileMetadataService extends Logging {

  private val formatter = DateTimeFormatter.ISO_DATE_TIME

  def compileMetadata(
    subscriptionDetails: ResponseDetail,
    conversationID: ConversationId,
    submissionTime: LocalDateTime,
    fileName: String
  ): Map[String, String] = {

    val primaryContact = transformContactInformation(subscriptionDetails.primaryContact, "primaryContact")
    val secondaryContact = subscriptionDetails.secondaryContact
      .map(transformContactInformation(_, "secondaryContact"))
      .getOrElse(Map.empty)
    val tradingName = subscriptionDetails.tradingName
      .map(tradingName => Map("requestAdditionalDetail/tradingName" -> tradingName))
      .getOrElse(Map.empty)

    tradingName ++ Map(
      "requestCommon/conversationID"           -> conversationID.value,
      "requestCommon/receiptDate"              -> submissionTime.format(formatter),
      "requestCommon/regime"                   -> "CBC",
      "requestCommon/schemaVersion"            -> "1.0.0",
      "requestAdditionalDetail/fileName"       -> fileName,
      "requestAdditionalDetail/subscriptionID" -> subscriptionDetails.subscriptionID,
      "requestAdditionalDetail/isGBUser"       -> subscriptionDetails.isGBUser.toString
    ) ++ primaryContact ++ secondaryContact
  }

  private def transformContactInformation(contactInformation: ContactInformation, contactType: String): Map[String, String] = {
    val organisationName = contactInformation.organisationDetails.organisationName
    val contactName      = Map(s"requestAdditionalDetail/$contactType/organisationDetails/organisationName" -> organisationName)
    val phoneNumber      = contactInformation.phone.map(phone => Map(s"requestAdditionalDetail/$contactType/phoneNumber" -> phone))
    val mobileNumber     = contactInformation.mobile.map(mobile => Map(s"requestAdditionalDetail/$contactType/mobileNumber" -> mobile))
    val email            = Map(s"requestAdditionalDetail/$contactType/emailAddress" -> contactInformation.email)

    contactName ++ email ++ phoneNumber.getOrElse(Map.empty) ++ mobileNumber.getOrElse(Map.empty)
  }
}
