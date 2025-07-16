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

package controllers

import config.AppConfig
import controllers.auth.IdentifierAuthAction
import models.error.BackendError
import models.submission._
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, ControllerComponents, Result}
import services.submission.SubmissionService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubmissionController @Inject() (
  authenticate: IdentifierAuthAction,
  cc: ControllerComponents,
  submissionService: SubmissionService,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def submitDisclosure: Action[JsValue] = authenticate.async(parse.json) { implicit request =>
    implicit val fileReferenceId: String =
      request.headers.get("x-file-reference-id").getOrElse("FileReferenceId header not found") //this id originates from upscan
    request.body
      .validate[SubmissionDetails]
      .fold(
        invalid = _ => Future.successful(InternalServerError),
        valid = submission => {
          val fileSize = submission.fileSize
          if (fileSize > appConfig.maxLargeFileSizeBytes) {
            logger.warn(s"File with uploadId: [${submission.uploadId.value}] has size above max limit [${appConfig.maxLargeFileSizeBytes}]")
            Future.successful(InternalServerError)
          } else if (fileSize <= appConfig.maxNormalFileSizeBytes) {
            handleResult(submissionService.submitNormalFile(submission))
          } else {
            handleResult(submissionService.submitLargeFile(submission))
          }
        }
      )
  }

  private def handleResult(submissionResult: Future[Either[BackendError, ConversationId]]): Future[Result] =
    submissionResult.map {
      case Right(conversationId) =>
        Ok(Json.toJson(conversationId))
      case Left(error) =>
        logger.warn(error.detail)
        InternalServerError
    }
}
