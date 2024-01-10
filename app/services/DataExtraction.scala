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

import models.submission._

import javax.inject.Inject
import scala.xml.{Elem, NodeSeq}

@Inject
class DataExtraction()() {


  def messageSpecData(xml: Elem): Option[MessageSpecData] =
    for {
      messageID <- (xml \\ "MessageRefId").headOption
      typeIndic <- (xml \\ "MessageTypeIndic").headOption.map(node => MessageTypeIndic.fromString(node.text))
      reportingEntityName <- (xml \\ "ReportingEntity" \\ "Entity" \\ "Name").headOption
    } yield MessageSpecData(messageID.text, typeIndic, reportingEntityName.text, getReportType(typeIndic, xml))

  def getReportType(messageTypeIndicator: MessageTypeIndic, xml: Elem): ReportType = {
    val allDocTypeIndicators: Seq[String] = (xml \\ "DocTypeIndic").map(node => node.text)
    val reportingEntityDocTypeIndicators = (xml \\ "ReportingEntity" \\ "DocTypeIndic").map(node => node.text)
    val cbcReportAndAdditionalInfoSections = xml \\ "CbcReports" ++ xml \\ "AdditionalInfo"

    val testDataIndicators: Seq[String] = List("OECD10", "OECD11", "OECD12", "OECD13")

    if (allDocTypeIndicators.exists(indic => testDataIndicators.contains(indic))) {
      TestData
    }
    else if (messageTypeIndicator == CBC401) {
      NewInformation
    }
    else if (reportingEntityDocTypeIndicators.contains("OECD3")) {
      DeletionOfAllInformation
    }
    else if (cbcReportAndAdditionalInfoSections.nonEmpty) {
      val docTypeIndicators: NodeSeq = cbcReportAndAdditionalInfoSections \\ "DocTypeIndic"
      val uniqueDocTypes = docTypeIndicators.map(node => node.text).distinct

      (uniqueDocTypes.length, uniqueDocTypes.headOption) match {
        case (1, Some("OECD1")) => NewInformationForExistingReport
        case (1, Some("OECD2")) => CorrectionForExistingReport
        case (1, Some("OECD3")) => DeletionForExistingReport
        case _ => CorrectionAndDeletionForExistingReport
      }
    }
    else {
      CorrectionForReportingEntity
    }
  }

}
