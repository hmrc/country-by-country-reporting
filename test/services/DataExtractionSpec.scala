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
import models.submission.{CBC401, CBC402, CorrectionAndDeletionForExistingReport, CorrectionForExistingReport, CorrectionForReportingEntity, DeletionForExistingReport, DeletionOfAllInformation, MessageSpecData, NewInformation, NewInformationForExistingReport, ReportType, TestData}

import scala.xml.Elem

class DataExtractionSpec extends SpecBase {

  val dataExtraction: DataExtraction = app.injector.instanceOf[DataExtraction]

  def generateValidXml(
    reportingEntityDocTypeIndic: List[Option[String]] = List(None),
    cbcReportDocTypeIndic: List[Option[String]] = List(),
    additionalInfoDocTypeIndic: List[Option[String]] = List()
  ):Elem = {
    <CBC_OECD xmlns="urn:oecd:ties:cbc:v2" xmlns:urn1="urn:oecd:ties:cbcstf:v5">
      <MessageSpec>
        <MessageRefId>MessageRefId</MessageRefId>
        <MessageTypeIndic>CBC401</MessageTypeIndic>
      </MessageSpec>
      {reportingEntityDocTypeIndic.map(reportingEntityIndic =>
      <CbcBody>
        <ReportingEntity>
          <Entity>
            <Name>Name</Name>
          </Entity>
          {if (reportingEntityIndic.nonEmpty) {
            <DocSpec>
              <urn1:DocTypeIndic>{reportingEntityIndic.get}</urn1:DocTypeIndic>
            </DocSpec>
          }}
        </ReportingEntity>
        {cbcReportDocTypeIndic.map(cbcReportIndic =>
        <CbcReports>
          {if (cbcReportIndic.nonEmpty) {
            <DocSpec>
              <urn1:DocTypeIndic>{cbcReportIndic.get}</urn1:DocTypeIndic>
            </DocSpec>
          }}
        </CbcReports>
      )}
        {additionalInfoDocTypeIndic.map(additionalInfoIndic =>
        <AdditionalInfo>
          {if (additionalInfoIndic.nonEmpty) {
            <DocSpec>
              <urn1:DocTypeIndic>{additionalInfoIndic.get}</urn1:DocTypeIndic>
            </DocSpec>
          }}
        </AdditionalInfo>
      )}
      </CbcBody>
    )}
    </CBC_OECD>
  }

  "messageSpecData" - {

    "must return Some(MessageSpecData) when given valid xml" in {
      val xml = generateValidXml()

      dataExtraction.messageSpecData(xml) mustBe Some(MessageSpecData("MessageRefId", CBC401, "Name", NewInformation))
    }

    "must return None when given invalid xml" in {
      val xml: Elem = {
        <file>
          <MessageTypeIndic></MessageTypeIndic>
          <ReportingEntity>
            <Entity>
              <Name>Name</Name>
            </Entity>
          </ReportingEntity>
        </file>
      }

      dataExtraction.messageSpecData(xml) mustBe None
    }

  }

  "getReportType" - {

    "must return TestDataReportType if any DocTypeIndic values are one of the test values" - {
      List("OECD10", "OECD11", "OECD12", "OECD13").foreach { code =>
        s"DocTypeIndic ${code} in ReportingEntity returns TestDataReportType" in {
          val xml = generateValidXml(
            reportingEntityDocTypeIndic = List(Some(code)),
            cbcReportDocTypeIndic = List(Some("OECD1"), Some("OECD2")),
            additionalInfoDocTypeIndic = List(Some("OECD3"))
          )

          dataExtraction.getReportType(CBC402, xml) mustBe TestData
        }
        s"DocTypeIndic ${code} in CbcReports returns TestDataReportType" in {
          val xml = generateValidXml(
            reportingEntityDocTypeIndic = List(Some("OECD1")),
            cbcReportDocTypeIndic = List(Some(code), Some("OECD2")),
            additionalInfoDocTypeIndic = List(Some("OECD3"))
          )

          dataExtraction.getReportType(CBC402, xml) mustBe TestData
        }
        s"DocTypeIndic ${code} in AdditionalInfo returns TestDataReportType" in {
          val xml = generateValidXml(
            reportingEntityDocTypeIndic = List(Some("OECD1")),
            cbcReportDocTypeIndic = List(Some("OECD2"), Some("OECD3")),
            additionalInfoDocTypeIndic = List(Some(code))
          )

          dataExtraction.getReportType(CBC402, xml) mustBe TestData
        }
      }
    }

    "must return NewInformationReportType if the MessageTypeIndic is CBC401" in {
      val xml = generateValidXml()

      dataExtraction.getReportType(CBC401, xml) mustBe NewInformation
    }

    "must return DeletionOfAllPreviousInformationReportType if any ReportingEntity DocTypeIndic is OECD3" in {
      val xml = generateValidXml(
        reportingEntityDocTypeIndic = List(None, Some("Y"), Some("OECD3"))
      )

      dataExtraction.getReportType(CBC402, xml) mustBe DeletionOfAllInformation
    }

    "must return CorrectionsAndDeletionsForExistingReportType if there is at least one CbcReports element" in {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(None)
      )

      dataExtraction.getReportType(CBC402, xml) mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionsAndDeletionsForExistingReportType if there is at least one AdditionalInfo element" in {
      val xml = generateValidXml(
        additionalInfoDocTypeIndic = List(None)
      )

      dataExtraction.getReportType(CBC402, xml) mustBe CorrectionAndDeletionForExistingReport
    }

    "must return NewInformationForExistingReport if all the DocTypeIndic values in CbcReports or AdditionalInfo are OECD1" in  {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(Some("OECD1"), None),
        additionalInfoDocTypeIndic = List(Some("OECD1"))
      )

      dataExtraction.getReportType(CBC402, xml) mustBe NewInformationForExistingReport
    }

    "must return CorrectionsForExistingReportType if all the DocTypeIndic values in CbcReports or AdditionalInfo are OECD2" in  {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(Some("OECD2"), Some("OECD2"), Some("OECD2"))
      )

      dataExtraction.getReportType(CBC402, xml) mustBe CorrectionForExistingReport
    }

    "must return DeletionForExistingReport if all the DocTypeIndic values in CbcReports or AdditionalInfo are OECD3" in  {
      val xml = generateValidXml(
        additionalInfoDocTypeIndic = List(Some("OECD3"), None)
      )

      dataExtraction.getReportType(CBC402, xml) mustBe DeletionForExistingReport
    }

    "must return CorrectionAndDeletionForExistingReport if the DocTypeIndic values in CbcReports or AdditionalInfo are mixed" in  {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(Some("OECD1"), Some("OECD1"), Some("OECD2")),
        additionalInfoDocTypeIndic = List(Some("OECD1"))
      )

      dataExtraction.getReportType(CBC402, xml) mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionAndDeletionForExistingReport if there are no DocTypeIndic values in CbcReports or AdditionalInfo" in  {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(None, None),
        additionalInfoDocTypeIndic = List(None)
      )

      dataExtraction.getReportType(CBC402, xml) mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionForReportingEntity if none of the conditions above are met" in {
      val xml = generateValidXml()

      dataExtraction.getReportType(CBC402, xml) mustBe CorrectionForReportingEntity
    }
  }

}