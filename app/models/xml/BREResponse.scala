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

package models.xml

import play.api.libs.json.{Json, OWrites}

import scala.xml.NodeSeq

case class BREResponse(regime: String, conversationID: String, genericStatusMessage: GenericStatusMessage)

object BREResponse {

  given XmlReads[BREResponse] with
    def read(xml: NodeSeq): BREResponse =
      val regime               = (xml \# "requestCommon" \# "regime").text
      val conversationID       = (xml \# "requestCommon" \# "conversationID").text
      val genericStatusMessage = fromXml[GenericStatusMessage](xml \# "requestDetail" \# "GenericStatusMessage")
      BREResponse(regime, conversationID, genericStatusMessage)

  given writes: OWrites[BREResponse] = Json.writes[BREResponse]
}
