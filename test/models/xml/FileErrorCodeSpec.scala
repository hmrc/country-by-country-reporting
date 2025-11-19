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

import base.SpecBase
import models.xml.FileErrorCode.UnknownFileErrorCode
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import models.xml.FileErrorCode.given

class FileErrorCodeSpec extends SpecBase with ScalaCheckPropertyChecks {

  "FileErrorCode" - {
    "read errorCode" in {
      for (errorCode <- FileErrorCode.values) {
        val xml = <Code>{errorCode.code}</Code>
        fromXml(xml) mustBe errorCode
      }
    }

    "read unknown errorCode" in {
      val xml = <Code>{50000}</Code>
      fromXml(xml) mustBe UnknownFileErrorCode("50000")
    }

    "read CBC Error Codes" in {
      val xml = <Code>CBC Error Code 39a</Code>
      fromXml(xml) mustBe UnknownFileErrorCode("CBC Error Code 39a")
    }
  }
}
