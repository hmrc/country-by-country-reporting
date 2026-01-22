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

package services.validation

import config.AppConfig
import helpers.XmlErrorMessageHelper
import models.submission.MessageSpecData
import models.validation.*
import org.xml.sax.SAXParseException
import play.api.Logging
import services.DataExtractionStream

import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class UploadedXmlValidationEngine @Inject() (xmlValidationService: XMLValidationService,
                                             xmlErrorMessageHelper: XmlErrorMessageHelper,
                                             dataExtraction: DataExtractionStream,
                                             appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  def validateUploadSubmission(upScanUrl: String): Future[SubmissionValidationResult] =
    try
      performXmlValidation(upScanUrl) map {
        case Right(messageSpecData) =>
          messageSpecData match {
            case Some(msd) => SubmissionValidationSuccess(msd)
            case None =>
              val errorMessage = "Could not retrieve messageSpec information from the submission"
              logger.warn(errorMessage)
              InvalidXmlError(errorMessage)
          }

        case Left(errors) =>
          SubmissionValidationFailure(ValidationErrors(errors))
      }
    catch {
      case e: SAXParseException =>
        logger.error(s"XML parsing failed. The XML parser has thrown the exception:", e)
        Future.successful(InvalidXmlError(e.getMessage))
    }

  def performXmlValidation(upScanUrl: String): Future[Either[List[GenericError], Option[MessageSpecData]]] =
    xmlValidationService.validateUrlStreamAsync(upScanUrl, appConfig.fileUploadXSDFilePath) flatMap {
      case Right(_)   => dataExtraction.messageSpecData(upScanUrl).map(Right(_))
      case Left(list) => Future.successful(Left(xmlErrorMessageHelper.generateErrorMessages(ListBuffer.from(list))))
    }
}
