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
import models.agentSubscription.{AgentContactDetails, AgentResponseDetail}
import models.subscription.{ContactInformation, OrganisationDetails, ResponseDetail}
import org.scalatest.StreamlinedXmlEquality._

class TransformServiceSpec extends SpecBase {
  val pretty = new scala.xml.PrettyPrinter(80, 4)

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

    assert(result === expected)
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
    val x1 = pretty.format(scala.xml.Utility.trim(result))
    val x2 = pretty.format(scala.xml.Utility.trim(expected))
    x1 mustBe x2

  }

  "must transform Subscription Details with AgentContactInformation" in {
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

    val contactInformationForAgent =
      AgentResponseDetail(
        subscriptionID = "subscriptionID",
        tradingName = Some("tradingName"),
        isGBUser = true,
        primaryContact = ContactInformation(
          email = "agent@test.com",
          phone = Some("bbb"),
          mobile = None,
          organisationDetails = OrganisationDetails(
            organisationName = "Example"
          )
        ),
        secondaryContact = Some(
          ContactInformation(
            email = "agent2@test.com",
            phone = Some("eee"),
            mobile = Some("fff"),
            organisationDetails = OrganisationDetails(
              organisationName = "AnotherExample"
            )
          )
        )
      )

    val agentDetails = AgentContactDetails("agent007", contactInformationForAgent)
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
      <agentDetails>
        <agentReferenceNumber>agent007</agentReferenceNumber>
        <agentPrimaryContact>
          <phoneNumber>bbb</phoneNumber>
          <emailAddress>agent@test.com</emailAddress>
          <organisationDetails>
            <organisationName>Example</organisationName>
          </organisationDetails>
        </agentPrimaryContact>
        <agentSecondaryContact>
          <phoneNumber>eee</phoneNumber>
          <mobileNumber>fff</mobileNumber>
          <emailAddress>agent2@test.com</emailAddress>
          <organisationDetails>
            <organisationName>AnotherExample</organisationName>
          </organisationDetails>
        </agentSecondaryContact>
      </agentDetails>
      </subscriptionDetails>
    val result = <subscriptionDetails>
      {service.transformSubscriptionDetails(contactInformation, None, Some(agentDetails))}
    </subscriptionDetails>
    val x1 = pretty.format(scala.xml.Utility.trim(result))
    val x2 = pretty.format(scala.xml.Utility.trim(expected))
    x1 mustBe x2
  }

  "add namespace definitions for CBC-oecd" in {
    val service = app.injector.instanceOf[TransformService]
    val file = <CBC_OECD version="1.0.0">
      <submission>Submitted Data</submission>
    </CBC_OECD>

    val expected = <CBC_OECD version="1.0.0"
                                     xmlns:cbc="urn:oecd:ties:cbc:v2"
                                     xmlns:stf="urn:oecd:ties:cbcstf:v5"
                                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <submission>Submitted Data</submission>
    </CBC_OECD>

    val result = service.addNameSpaceDefinitions(file)

    result.toString mustBe expected.toString
  }

}
