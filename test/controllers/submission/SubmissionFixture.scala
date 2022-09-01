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

package controllers.submission

import models.subscription.{ContactInformation, OrganisationDetails, ResponseDetail}

object SubmissionFixture {
  val basicXml =
    <submission>
      <fileName>my-file.xml</fileName>
      <enrolmentID>enrolmentID</enrolmentID>
      <file>
        <CBC_OECD xmlns="urn:oecd:ties:cbc:v2" xmlns:stf="urn:oecd:ties:cbcstf:v5">
          <MessageSpec>
            <SendingEntityIN>string</SendingEntityIN>
            <TransmittingCountry>XK</TransmittingCountry>
            <ReceivingCountry>PY</ReceivingCountry>
            <MessageType>CBC</MessageType>
            <Language>KR</Language>
            <Warning>This is a warning</Warning>
            <Contact>This is a Contact</Contact>
            <MessageRefId>GBXACBC1234567</MessageRefId>
            <MessageTypeIndic>CBC401</MessageTypeIndic>
            <CorrMessageRefId>string</CorrMessageRefId>
            <ReportingPeriod>2014-06-09+01:00</ReportingPeriod>
            <Timestamp>2008-11-15T16:52:58</Timestamp>
          </MessageSpec>
          <CbcBody>
            <ReportingEntity>
              <Entity>
                <ResCountryCode>DJ</ResCountryCode>
                <TIN issuedBy="MD">string</TIN>
                <IN issuedBy="VI" INType="string">string</IN>
                <Name>string</Name>
                <Address legalAddressType="OECD303">
                  <CountryCode>YT</CountryCode>
                  <AddressFix>
                    <Street>string</Street>
                    <BuildingIdentifier>string</BuildingIdentifier>
                    <SuiteIdentifier>string</SuiteIdentifier>
                    <FloorIdentifier>string</FloorIdentifier>
                    <DistrictName>string</DistrictName>
                    <POB>string</POB>
                    <PostCode>string</PostCode>
                    <City>string</City>
                    <CountrySubentity>string</CountrySubentity>
                  </AddressFix>
                </Address>
              </Entity>
              <NameMNEGroup>string</NameMNEGroup>
              <ReportingRole>CBC704</ReportingRole>
              <ReportingPeriod>
                <StartDate>2016-01-01</StartDate>
                <EndDate>2012-01-07</EndDate>
              </ReportingPeriod>
              <DocSpec>
                <stf:DocTypeIndic>OECD1</stf:DocTypeIndic>
                <stf:DocRefId>string</stf:DocRefId>
                <stf:CorrMessageRefId>string</stf:CorrMessageRefId>
                <stf:CorrDocRefId>string</stf:CorrDocRefId>
              </DocSpec>
            </ReportingEntity>
            <CbcReports>
              <DocSpec>
                <stf:DocTypeIndic>OECD2</stf:DocTypeIndic>
                <stf:DocRefId>string</stf:DocRefId>
                <stf:CorrMessageRefId>string</stf:CorrMessageRefId>
                <stf:CorrDocRefId>string</stf:CorrDocRefId>
              </DocSpec>
              <ResCountryCode>IM</ResCountryCode>
              <Summary>
                <Revenues>
                  <Unrelated currCode="XAG">100</Unrelated>
                  <Related currCode="MMK">100</Related>
                  <Total currCode="XPF">100</Total>
                </Revenues>
                <ProfitOrLoss currCode="FJD">100</ProfitOrLoss>
                <TaxPaid currCode="MXN">100</TaxPaid>
                <TaxAccrued currCode="PKR">100</TaxAccrued>
                <Capital currCode="MNT">100</Capital>
                <Earnings currCode="EGP">100</Earnings>
                <NbEmployees>100</NbEmployees>
                <Assets currCode="MMK">100</Assets>
              </Summary>
              <ConstEntities>
                <ConstEntity>
                  <ResCountryCode>GD</ResCountryCode>
                  <TIN issuedBy="CN">string</TIN>
                  <IN issuedBy="MG" INType="string">string</IN>
                  <Name>string</Name>
                  <Address legalAddressType="OECD301">
                    <CountryCode>HN</CountryCode>
                    <AddressFix>
                      <Street>string</Street>
                      <BuildingIdentifier>string</BuildingIdentifier>
                      <SuiteIdentifier>string</SuiteIdentifier>
                      <FloorIdentifier>string</FloorIdentifier>
                      <DistrictName>string</DistrictName>
                      <POB>string</POB>
                      <PostCode>string</PostCode>
                      <City>string</City>
                      <CountrySubentity>string</CountrySubentity>
                    </AddressFix>
                    <AddressFree>string</AddressFree>
                  </Address>
                </ConstEntity>
                <Role>CBC802</Role>
                <IncorpCountryCode>IR</IncorpCountryCode>
                <BizActivities>CBC505</BizActivities>
                <OtherEntityInfo>string</OtherEntityInfo>
              </ConstEntities>
            </CbcReports>
            <AdditionalInfo>
              <DocSpec>
                <stf:DocTypeIndic>OECD0</stf:DocTypeIndic>
                <stf:DocRefId>string</stf:DocRefId>
                <stf:CorrMessageRefId>string</stf:CorrMessageRefId>
                <stf:CorrDocRefId>string</stf:CorrDocRefId>
              </DocSpec>
              <OtherInfo language="EN">string</OtherInfo>
              <ResCountryCode>SM</ResCountryCode>
              <SummaryRef>CBC607</SummaryRef>
            </AdditionalInfo>
          </CbcBody>
        </CBC_OECD>
      </file>
    </submission>

  val responseDetail =
    ResponseDetail(
      subscriptionID = "subscriptionID",
      tradingName = Some("tradingName"),
      isGBUser = true,
      primaryContact = ContactInformation(
        email = "aaa@test.com",
        phone = Some("1234567"),
        mobile = None,
        organisationDetails = OrganisationDetails(
          organisationName = "Example"
        )
      ),
      secondaryContact = Some(
        ContactInformation(
          email = "ddd",
          phone = Some("12345678"),
          mobile = Some("1222222"),
          organisationDetails = OrganisationDetails(
            organisationName = "AnotherExample"
          )
        )
      )
    )

}
