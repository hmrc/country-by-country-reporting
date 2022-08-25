/*
 * Copyright 2022 HM Revenue & Customs
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
import models.subscription.{ContactInformation, OrganisationDetails, ResponseDetail}
import org.scalatest.StreamlinedXmlEquality

class TransformServiceSpec extends SpecBase with StreamlinedXmlEquality {

  "must transform ContactInformation with organisation" in {
    val service = app.injector.instanceOf[TransformService]

    val contactInformation = ContactInformation(
      email = "aaa",
      phone = Some("bbb"),
      mobile = None,
      organisationDetails = OrganisationDetails(
        organisationName = "Example"
      )
    )

    val expected =
      <contactDetails>
        <phoneNumber>bbb</phoneNumber>
        <emailAddress>aaa</emailAddress>
        <organisationDetails>
          <organisationName>Example</organisationName>
        </organisationDetails>
      </contactDetails>

    val result = <contactDetails>
      {service.transformContactInformation(contactInformation)}
    </contactDetails>

    result mustEqual expected
  }

  "must transform Subscription Details" in {
    val service = app.injector.instanceOf[TransformService]

    val contactInformation =
      ResponseDetail(
        subscriptionID = "subscriptionID",
        tradingName = Some("tradingName"),
        isGBUser = true,
        primaryContact = ContactInformation(
          email = "aaa",
          phone = Some("bbb"),
          mobile = None,
          organisationDetails = OrganisationDetails(
            organisationName = "Example"
          )
        ),
        secondaryContact = Some(
          ContactInformation(
            email = "ddd",
            phone = Some("eee"),
            mobile = Some("fff"),
            organisationDetails = OrganisationDetails(
              organisationName = "AnotherExample"
            )
          )
        )
      )

    val expected =
      <subscriptionDetails>
        <subscriptionID>subscriptionID</subscriptionID>
        <tradingName>tradingName</tradingName>
        <isGBUser>true</isGBUser>
        <primaryContact>
          <phoneNumber>bbb</phoneNumber>
          <emailAddress>aaa</emailAddress>
          <organisationDetails>
            <organisationName>Example</organisationName>
          </organisationDetails>
        </primaryContact>
        <secondaryContact>
          <phoneNumber>eee</phoneNumber>
          <mobileNumber>fff</mobileNumber>
          <emailAddress>ddd</emailAddress>
          <organisationDetails>
            <organisationName>AnotherExample</organisationName>
          </organisationDetails>
        </secondaryContact>
      </subscriptionDetails>

    val result = <subscriptionDetails>
      {service.transformSubscriptionDetails(contactInformation, None)}
    </subscriptionDetails>
    expected == result
  }

  "add namespace definitions for CBC-oecd" in {
    val service = app.injector.instanceOf[TransformService]
    val file = <CBC_OECD version="1.0.0">
      <submission>Submitted Data</submission>
    </CBC_OECD>

    val expected = <CBC_OECD version="1.0.0"
                                     xmlns:cbc="urn:oecd:ties:cbc:v1"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <submission>Submitted Data</submission>
    </CBC_OECD>

    val result = service.addNameSpaceDefinitions(file)

    result.toString mustBe expected.toString
  }

}
