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

import base.SpecBase
import models.validation.SaxParseError

import scala.collection.mutable.ListBuffer
import scala.xml.XML

class XmlValidationServiceSpec extends SpecBase {
  val noErrors: ListBuffer[SaxParseError] = ListBuffer()

  val xsdPath = "/xsd/CbcXML_v2.0.xsd"

  "Validation Service" - {
    "must pass back errors if a file is invalid" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val invalid = <this>
      <will>not validate</will>
      </this>

      val result = service.validate(invalid, xsdPath)

      result.isLeft mustBe true
    }

    "must correctly invalidate a submission with a data problem" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val validSubmission = XML.loadFile("test/resources/cbc/fileUpload/invalidcbc.xml")

      val result = service.validate(validSubmission, xsdPath)

      result.isLeft mustBe true
    }

    "must correctly validate a submission" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val validSubmission = XML.loadFile("test/resources/cbc/fileUpload/validcbc.xml")

      val result = service.validate(validSubmission, xsdPath)

      result.isLeft mustBe false
    }

    "must correctly validate a submission with whitespace" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val validSubmission = XML.loadFile("test/resources/cbc/fileUpload/validcbc_whitespace.xml")

      val result = service.validate(validSubmission, xsdPath)

      result.isLeft mustBe false
    }

    "must correctly validate a submission with empty value" in {
      val service = app.injector.instanceOf[XMLValidationService]

      val validSubmission = XML.loadFile("test/resources/cbc/fileUpload/validcbc_empty.xml")

      val result = service.validate(validSubmission, xsdPath)

      result.isLeft mustBe true
    }
  }
}
