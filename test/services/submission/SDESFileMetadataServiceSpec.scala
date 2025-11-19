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
import generators.Generators
import models.submission.ConversationId
import models.subscription.ResponseDetail
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SDESFileMetadataServiceSpec extends SpecBase with MockitoSugar with ScalaCheckDrivenPropertyChecks with Generators {

  private val sdesFileMetaDataService = app.injector.instanceOf[SDESFileMetadataService]
  private val dateTime                = LocalDateTime.now(fixedClock)

  "SDESMetaDataServiceSpec" - {
    "compileMetaData" - {

      "must compile metadata for an organisation" in {
        forAll { (responseDetail: ResponseDetail, conversationId: ConversationId, fileName: String) =>
          val result = sdesFileMetaDataService.compileMetadata(responseDetail, conversationId, dateTime, fileName)

          val primaryContact        = responseDetail.primaryContact
          val maybeSecondaryContact = responseDetail.secondaryContact

          result.get("requestCommon/conversationID").value mustBe conversationId.value
          result.get("requestCommon/receiptDate").value mustBe dateTime.format(DateTimeFormatter.ISO_DATE_TIME)
          result.get("requestCommon/schemaVersion").value mustBe "1.0.0"
          result.get("requestCommon/regime").value mustBe Regime
          result.get("requestAdditionalDetail/primaryContact/emailAddress").value mustBe primaryContact.email
          result
            .get("requestAdditionalDetail/primaryContact/organisationDetails/organisationName")
            .value mustBe primaryContact.organisationDetails.organisationName
          result.get("requestAdditionalDetail/fileName").value mustBe fileName
          result.get("requestAdditionalDetail/subscriptionID").value mustBe responseDetail.subscriptionID
          result.get("requestAdditionalDetail/isGBUser").value mustBe responseDetail.isGBUser.toString

          result.get("requestAdditionalDetail/tradingName") mustBe responseDetail.tradingName
          result.get("requestAdditionalDetail/primaryContact/mobileNumber") mustBe primaryContact.mobile
          result.get("requestAdditionalDetail/primaryContact/phoneNumber") mustBe primaryContact.phone

          result.get("requestAdditionalDetail/secondaryContact/phoneNumber") mustBe maybeSecondaryContact.flatMap(_.phone)
          result.get("requestAdditionalDetail/secondaryContact/emailAddress") mustBe maybeSecondaryContact.map(_.email)
          result.get("requestAdditionalDetail/secondaryContact/mobileNumber") mustBe maybeSecondaryContact.flatMap(_.mobile)
          result.get("requestAdditionalDetail/secondaryContact/organisationDetails/organisationName") mustBe maybeSecondaryContact.map(
            _.organisationDetails.organisationName
          )
        }
      }
    }
  }
}
