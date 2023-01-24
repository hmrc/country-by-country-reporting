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

package models.submission

import base.SpecBase
import play.api.libs.json.{JsResultException, Json}

class MessageSpecDataSpec extends SpecBase {

  "MessageSpecDataSpec" - {
    "must serialize MessageSpec for MessageTypeIndic CBC401" in {
      val msd          = MessageSpecData("XDSG111111", CBC401, "Reporting Entity")
      val expectedJson = Json.parse("""{"messageRefId":"XDSG111111","messageTypeIndic":"CBC401","reportingEntityName":"Reporting Entity"}""")
      Json.toJson(msd) mustBe expectedJson
    }
    "must deserialize MessageSpec for MessageTypeIndic CBC401" in {
      val json     = Json.parse("""{"messageRefId":"XDSG333333","messageTypeIndic":"CBC401","reportingEntityName":"Reporting Entity"}""")
      val expected = MessageSpecData("XDSG333333", CBC401, "Reporting Entity")

      json.as[MessageSpecData] mustEqual expected
    }
    "must serialize MessageSpec for MessageTypeIndic CBC402" in {
      val msd = MessageSpecData("XDSG111111", CBC402, "Reporting Entity")
      val expectedJson = Json.parse("""{"messageRefId":"XDSG111111","messageTypeIndic":"CBC402","reportingEntityName":"Reporting Entity"}""")
      Json.toJson(msd) mustBe expectedJson
    }
    "must deserialize MessageSpec for MessageTypeIndic CBC402" in {
      val json = Json.parse("""{"messageRefId":"XDSG333333","messageTypeIndic":"CBC402","reportingEntityName":"Reporting Entity"}""")
      val expected = MessageSpecData("XDSG333333", CBC402, "Reporting Entity")

      json.as[MessageSpecData] mustEqual expected
    }
    "must fail to deserialize for any other MessageTypeIndic value" in {
      val json = Json.parse("""{"messageRefId":"XDSG333333","messageTypeIndic":"CBC123","reportingEntityName":"Reporting Entity"}""")

      a [JsResultException] must be thrownBy(json.as[MessageSpecData])
    }
  }
}
