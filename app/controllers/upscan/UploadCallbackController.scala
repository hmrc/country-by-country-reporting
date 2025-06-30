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

package controllers.upscan

import models.audit.Audit
import models.upscan.CallbackBody
import play.api.i18n.MessagesApi
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.{Action, ControllerComponents}
import services.upscan.UpScanCallbackDispatcher
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadCallbackController @Inject() (
  val upscanCallbackDispatcher: UpScanCallbackDispatcher,
  cc: ControllerComponents,
  implicit override val messagesApi: MessagesApi
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  implicit val writes: Writes[Audit[JsValue]] = (audit: Audit[JsValue]) =>
    Json.obj(
      "details" -> audit.details
    )

  val callback: Action[JsValue] = Action.async(parse.json) { implicit request =>
    val callback = request.body.validate[CallbackBody]
    callback.fold(
      _ => Future.successful(BadRequest("Invalid callback body")),
      validCallback =>
        upscanCallbackDispatcher
          .handleCallback(validCallback)
          .map(_ => Ok)
    )
  }
}
