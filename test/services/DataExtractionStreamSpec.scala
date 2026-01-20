/*
 * Copyright 2026 HM Revenue & Customs
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
import models.submission.*
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}
import scala.xml.Elem
import scala.concurrent.ExecutionContext.Implicits.global

class DataExtractionStreamSpec extends SpecBase {

  val dataExtraction = app.injector.instanceOf[DataExtractionStream]

  def generateValidXml(
    reportingEntityDocTypeIndic: List[Option[String]] = List(None),
    cbcReportDocTypeIndic: List[Option[String]] = List(),
    additionalInfoDocTypeIndic: List[Option[String]] = List()
  ): Elem =
    <CBC_OECD xmlns="urn:oecd:ties:cbc:v2" xmlns:urn1="urn:oecd:ties:cbcstf:v5">
      <MessageSpec>
        <MessageRefId>MessageRefId</MessageRefId>
        <MessageTypeIndic>CBC401</MessageTypeIndic>
      </MessageSpec>{
      reportingEntityDocTypeIndic.map(reportingEntityIndic =>
        <CbcBody>
        <ReportingEntity>
          <Entity>
            <Name>Name</Name>
          </Entity>
          <ReportingRole>CBC704</ReportingRole>
          <ReportingPeriod>
            <StartDate>2012-01-01</StartDate>
            <EndDate>2016-01-01</EndDate>
          </ReportingPeriod>{
          if reportingEntityIndic.nonEmpty then Seq(<DocSpec>
          <urn1:DocTypeIndic>
            {reportingEntityIndic.get}
          </urn1:DocTypeIndic>
        </DocSpec>)
          else Seq.empty
        }
        </ReportingEntity>{
          cbcReportDocTypeIndic.map(cbcReportIndic =>
            <CbcReports>
          {
              if cbcReportIndic.nonEmpty then Seq(<DocSpec>
          <urn1:DocTypeIndic>
            {cbcReportIndic.get}
          </urn1:DocTypeIndic>
        </DocSpec>)
              else Seq.empty
            }
        </CbcReports>
          )
        }{
          additionalInfoDocTypeIndic.map(additionalInfoIndic =>
            <AdditionalInfo>
          {
              if additionalInfoIndic.nonEmpty then Seq(<DocSpec>
          <urn1:DocTypeIndic>
            {additionalInfoIndic.get}
          </urn1:DocTypeIndic>
        </DocSpec>)
              else Seq.empty
            }
        </AdditionalInfo>
          )
        }
      </CbcBody>
      )
    }
    </CBC_OECD>

  "messageSpecData" - {

    "must return Some(MessageSpecData) when given valid xml" in {
      val xml = generateValidXml(reportingEntityDocTypeIndic = List(Some("OECD1")))

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl))

      result mustBe Some(MessageSpecData("MessageRefId", CBC401, NewInformation, startDate, endDate, "Name"))
    }

    "must return None when given invalid xml" in {
      val xml: Elem =
        <file>
          <MessageTypeIndic></MessageTypeIndic>
          <ReportingEntity>
            <Entity>
              <Name>Name</Name>
            </Entity>
          </ReportingEntity>
        </file>

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl))

      result mustBe None
    }

  }

  "getReportType" - {

    "must return TestDataReportType if any DocTypeIndic values are one of the test values" - {
      List("OECD10", "OECD11", "OECD12", "OECD13").foreach { code =>
        s"DocTypeIndic $code in ReportingEntity returns TestDataReportType" in {
          val xml = generateValidXml(
            reportingEntityDocTypeIndic = List(Some(code)),
            cbcReportDocTypeIndic = List(Some("OECD1"), Some("OECD2")),
            additionalInfoDocTypeIndic = List(Some("OECD3"))
          )

          val xmlFile = writeXmlToTempFile(xml)
          val xmlUrl  = xmlFile.toUri.toURL.toString

          val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

          result mustBe TestData
        }
        s"DocTypeIndic $code in CbcReports returns TestDataReportType" in {
          val xml = generateValidXml(
            reportingEntityDocTypeIndic = List(Some("OECD1")),
            cbcReportDocTypeIndic = List(Some(code), Some("OECD2")),
            additionalInfoDocTypeIndic = List(Some("OECD3"))
          )

          val xmlFile = writeXmlToTempFile(xml)
          val xmlUrl  = xmlFile.toUri.toURL.toString

          val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

          result mustBe TestData
        }
        s"DocTypeIndic $code in AdditionalInfo returns TestDataReportType" in {
          val xml = generateValidXml(
            reportingEntityDocTypeIndic = List(Some("OECD1")),
            cbcReportDocTypeIndic = List(Some("OECD2"), Some("OECD3")),
            additionalInfoDocTypeIndic = List(Some(code))
          )

          val xmlFile = writeXmlToTempFile(xml)
          val xmlUrl  = xmlFile.toUri.toURL.toString

          val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

          result mustBe TestData
        }
      }
    }

    "must return NewInformationReportType if the MessageTypeIndic is CBC401 and any ReportingEntity DocTypeIndic is OECD1" in {
      val xml = generateValidXml(reportingEntityDocTypeIndic = List(Some("OECD1")))

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe NewInformation
    }

    "must return NewInformationForExistingReport if the MessageTypeIndic is CBC401 and none of ReportingEntity DocTypeIndic is OECD1" in {
      val xml = generateValidXml(reportingEntityDocTypeIndic = List(Some("OECD2")))

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe NewInformationForExistingReport
    }

    "must return DeletionOfAllPreviousInformationReportType if any ReportingEntity DocTypeIndic is OECD3" in {
      val xml = generateValidXml(
        reportingEntityDocTypeIndic = List(None, Some("Y"), Some("OECD3"))
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe DeletionOfAllInformation
    }

    "must return CorrectionsAndDeletionsForExistingReportType if there is at least one CbcReports element" in {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(None)
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionsAndDeletionsForExistingReportType if there is at least one AdditionalInfo element" in {
      val xml = generateValidXml(
        additionalInfoDocTypeIndic = List(None)
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionsForExistingReportType if all the DocTypeIndic values in CbcReports or AdditionalInfo are OECD2" in {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(Some("OECD2"), Some("OECD2"), Some("OECD2"))
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionForExistingReport
    }

    "must return DeletionForExistingReport if all the DocTypeIndic values in CbcReports or AdditionalInfo are OECD3 and ReportingEntity DocTypeIndicators contains OECD0" in {
      val xml = generateValidXml(
        additionalInfoDocTypeIndic = List(Some("OECD3"), None),
        reportingEntityDocTypeIndic = List(Option("OECD0"))
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe DeletionForExistingReport
    }

    "must return CorrectionAndDeletionForExistingReport if the DocTypeIndic values in CbcReports or AdditionalInfo are mixed" in {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(Some("OECD1"), Some("OECD1"), Some("OECD2")),
        additionalInfoDocTypeIndic = List(Some("OECD1"))
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionAndDeletionForExistingReport if there are no DocTypeIndic values in CbcReports or AdditionalInfo" in {
      val xml = generateValidXml(
        cbcReportDocTypeIndic = List(None, None),
        additionalInfoDocTypeIndic = List(None)
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionAndDeletionForExistingReport if CbcReports or AdditionalInfo contains OECD3 and ReportingEntity DocTypeIndicators contains OECD2" in {
      val xml = generateValidXml(
        additionalInfoDocTypeIndic = List(Some("OECD3"), None),
        reportingEntityDocTypeIndic = List(Option("OECD2"))
      )

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionAndDeletionForExistingReport
    }

    "must return CorrectionForReportingEntity if none of the conditions above are met" in {
      val xml = generateValidXml()

      val xmlFile = writeXmlToTempFile(xml)
      val xmlUrl  = xmlFile.toUri.toURL.toString

      val result = await(dataExtraction.messageSpecData(xmlUrl).map(_.map(_.reportType).getOrElse(fail("Expected Some(MessageSpecData)"))))

      result mustBe CorrectionForReportingEntity
    }
  }

  private def writeXmlToTempFile(xml: Elem): Path = {
    val tmpFile = Files.createTempFile("message-spec-", ".xml")
    Files.write(tmpFile, xml.toString().getBytes(StandardCharsets.UTF_8))
    tmpFile
  }

}
