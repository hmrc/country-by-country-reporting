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

import models.submission.*
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.Attributes

import javax.xml.parsers.SAXParserFactory
import java.net.URL
import java.time.LocalDate
import javax.inject.Inject
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class DataExtractionStream @Inject() (implicit ec: ExecutionContext) {

  def messageSpecData(xmlUrl: String): Future[Option[MessageSpecData]] =
    Future {
      val acc = extractStreaming(xmlUrl)

      for {
        messageId  <- acc.messageRefId
        typeIndic  <- acc.messageTypeIndic
        startDate  <- acc.startDate
        endDate    <- acc.endDate
        entityName <- acc.reportingEntityName
      } yield MessageSpecData(
        messageId,
        typeIndic,
        getReportType(typeIndic, acc),
        startDate,
        endDate,
        entityName
      )
    }

  private def extractStreaming(xmlUrl: String): Accumulator = {
    val factory = SAXParserFactory.newInstance()
    factory.setNamespaceAware(true)

    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

    val parser  = factory.newSAXParser()
    val handler = new MessageSpecHandler

    val is = new URL(xmlUrl).openStream()
    try parser.parse(is, handler)
    finally is.close()

    handler.result
  }

  final private case class Accumulator(
    var messageRefId: Option[String] = None,
    var messageTypeIndic: Option[MessageTypeIndic] = None,
    var reportingEntityName: Option[String] = None,
    var startDate: Option[LocalDate] = None,
    var endDate: Option[LocalDate] = None,
    allDocTypeIndicators: mutable.ListBuffer[String] = mutable.ListBuffer.empty,
    reportingEntityDocTypeIndicators: mutable.ListBuffer[String] = mutable.ListBuffer.empty,
    var seenCbcReports: Boolean = false,
    var seenAdditionalInfo: Boolean = false,
    var sectionDocTypeIndicators: mutable.ListBuffer[String] = mutable.ListBuffer.empty
  )

  final private class MessageSpecHandler extends DefaultHandler {
    private val acc                = Accumulator()
    private var path: List[String] = Nil
    private val text               = new StringBuilder

    def result: Accumulator = acc

    override def startElement(
      uri: String,
      localName: String,
      qName: String,
      attributes: Attributes
    ): Unit = {
      val name = if (localName.nonEmpty) localName else qName
      path = name :: path
      text.clear()

      name match {
        case "CbcReports"     => acc.seenCbcReports = true
        case "AdditionalInfo" => acc.seenAdditionalInfo = true
        case _                =>
      }
    }

    override def characters(ch: Array[Char], start: Int, length: Int): Unit =
      text.appendAll(ch, start, length)

    override def endElement(
      uri: String,
      localName: String,
      qName: String
    ): Unit = {
      val value = text.toString.trim

      path match {
        case "MessageRefId" :: _ if value.nonEmpty =>
          acc.messageRefId = Some(value)

        case "MessageTypeIndic" :: _ if value.nonEmpty =>
          acc.messageTypeIndic = Some(MessageTypeIndic.fromString(value))

        case "Name" :: "Entity" :: "ReportingEntity" :: _ if value.nonEmpty =>
          acc.reportingEntityName = Some(value)

        case "StartDate" :: "ReportingPeriod" :: _ if value.nonEmpty =>
          acc.startDate = Some(LocalDate.parse(value))

        case "EndDate" :: "ReportingPeriod" :: _ if value.nonEmpty =>
          acc.endDate = Some(LocalDate.parse(value))

        case "DocTypeIndic" :: "DocSpec" :: "ReportingEntity" :: _ if value.nonEmpty =>
          acc.reportingEntityDocTypeIndicators += value
          acc.allDocTypeIndicators += value

        case "DocTypeIndic" :: "DocSpec" :: "AdditionalInfo" :: _ if value.nonEmpty =>
          acc.sectionDocTypeIndicators += value
          acc.allDocTypeIndicators += value

        case "DocTypeIndic" :: "DocSpec" :: "CbcReports" :: _ if value.nonEmpty =>
          acc.sectionDocTypeIndicators += value
          acc.allDocTypeIndicators += value

        case "DocTypeIndic" :: _ if value.nonEmpty =>
          acc.allDocTypeIndicators += value

        case _ =>
      }

      path = path.tail
      text.clear()
    }

  }

  private def getReportType(
    messageTypeIndicator: MessageTypeIndic,
    acc: Accumulator
  ): ReportType = {

    val testDataIndicators = Set("OECD10", "OECD11", "OECD12", "OECD13")

    // 1. Test data
    if (acc.allDocTypeIndicators.exists(testDataIndicators.contains)) {
      TestData
    }

    // 2. Deletion dominates everything
    else if (acc.reportingEntityDocTypeIndicators.contains("OECD3")) {
      DeletionOfAllInformation
    }

    // 3. Correction / deletion context if sections exist
    else if (acc.seenCbcReports || acc.seenAdditionalInfo) {
      val unique = acc.sectionDocTypeIndicators.distinct

      (unique.length, unique.headOption) match {
        case (1, Some("OECD2")) =>
          CorrectionForExistingReport

        case (1, Some("OECD3")) if acc.reportingEntityDocTypeIndicators.contains("OECD0") =>
          DeletionForExistingReport

        case _ =>
          CorrectionAndDeletionForExistingReport
      }
    }

    // 4. CBC401 new-information logic (only if no correction sections)
    else if (messageTypeIndicator == CBC401 && acc.reportingEntityDocTypeIndicators.nonEmpty) {
      if (acc.reportingEntityDocTypeIndicators.contains("OECD1")) {
        NewInformation
      } else {
        NewInformationForExistingReport
      }
    }

    // 5. Fallback
    else {
      CorrectionForReportingEntity
    }
  }

}
