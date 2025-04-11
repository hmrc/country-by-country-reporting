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

import models.upscan.CallbackBody
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.json.JsValue
import play.api.mvc.{Action, ControllerComponents}
import services.upscan.UpScanCallbackDispatcher
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.controller.WithJsonBody

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UploadCallbackController @Inject() (
  val cc: ControllerComponents,
  val upscanCallbackDispatcher: UpScanCallbackDispatcher,
  implicit override val messagesApi: MessagesApi
)(implicit val ec: ExecutionContext)
    extends BackendController(cc)
    with WithJsonBody
    with I18nSupport {

  val callback: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[CallbackBody] { feedback: CallbackBody =>
      upscanCallbackDispatcher
        .handleCallback(feedback)
        .map(_ => Ok)
    }
  }
}
