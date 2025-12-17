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
import models.xml.FileErrorCode.{FileContainsTestDataForProductionEnvironment, MessageRefIDHasAlreadyBeenUsed}
import models.xml.RecordErrorCode.{CorrDocRefIDTwiceInSameMessage, MessageTypeIndic}
import play.api.libs.json.{JsValue, Json}

import scala.xml.Elem

class GenericStatusMessageSpec extends SpecBase {

  "GenericStatusMessage" - {
    "must read xml as GenericStatusMessage when status is 'Accepted'" in {
      val xml: Elem = <gsm:GenericStatusMessage>
        <gsm:ValidationErrors></gsm:ValidationErrors>
        <gsm:ValidationResult>
          <gsm:Status>Accepted</gsm:Status>
        </gsm:ValidationResult>
      </gsm:GenericStatusMessage>

      val result = fromXml[GenericStatusMessage](xml)

      result mustBe GenericStatusMessage(ValidationErrors(None, None), ValidationStatus.accepted)
      Json.toJson(result) mustBe Json.parse("""{"validationErrors":{},"status":"Accepted"}""")

    }

    "must read xml as GenericStatusMessage when status is 'Rejected'" in {
      val xml: Elem = <gsm:GenericStatusMessage>
        <gsm:ValidationErrors>
          <gsm:FileError>
            <gsm:Code>50009</gsm:Code>
            <gsm:Details Language="EN">Duplicate message ref ID</gsm:Details>
          </gsm:FileError>
          <gsm:FileError>
            <gsm:Code>50010</gsm:Code>
            <gsm:Details Language="EN">test file error</gsm:Details>
          </gsm:FileError>
          <gsm:RecordError>
            <gsm:Code>80010</gsm:Code>
            <gsm:Details Language="EN">A message can contain either new records (OECD1) or corrections/deletions (OECD2 and OECD3), but cannot contain a mixture of both</gsm:Details>
            <gsm:DocRefIDInError>asjdhjjhjssjhdjshdAJGSJJS</gsm:DocRefIDInError>
          </gsm:RecordError>
          <gsm:RecordError>
            <gsm:Code>80011</gsm:Code>
            <gsm:Details Language="EN">Example Message</gsm:Details>
            <gsm:DocRefIDInError>test record</gsm:DocRefIDInError>
          </gsm:RecordError>
        </gsm:ValidationErrors>
        <gsm:ValidationResult>
          <gsm:Status>Rejected</gsm:Status>
        </gsm:ValidationResult>
      </gsm:GenericStatusMessage>

      val result = fromXml[GenericStatusMessage](xml)

      result mustBe GenericStatusMessage(
        ValidationErrors(
          Some(
            List(
              FileErrors(MessageRefIDHasAlreadyBeenUsed, Some("Duplicate message ref ID")),
              FileErrors(FileContainsTestDataForProductionEnvironment, Some("test file error"))
            )
          ),
          Some(
            List(
              RecordError(
                MessageTypeIndic,
                Some("A message can contain either new records (OECD1) or corrections/deletions (OECD2 and OECD3), but cannot contain a mixture of both"),
                Some(List("asjdhjjhjssjhdjshdAJGSJJS"))
              ),
              RecordError(CorrDocRefIDTwiceInSameMessage, Some("Example Message"), Some(List("test record")))
            )
          )
        ),
        ValidationStatus.rejected
      )

      val expectedJson: JsValue = Json.parse("""
                                               |{"validationErrors":
                                               |{"fileError":[{"code":"50009","details":"Duplicate message ref ID"}, {"code":"50010","details":"test file error"}],
                                               |"recordError":[
                                               |{"code":"80010","details":"A message can contain either new records (OECD1) or corrections/deletions (OECD2 and OECD3), but cannot contain a mixture of both","docRefIDInError":["asjdhjjhjssjhdjshdAJGSJJS"]},
                                               |{"code":"80011","details":"Example Message","docRefIDInError":["test record"]}
                                               |]},
                                               |"status":"Rejected"}""".stripMargin)
      Json.toJson(result) mustBe expectedJson

    }

    "must fail for an invalid xml" in {
      val xml: Elem = <test>random XML</test>

      assertThrows[RuntimeException] {
        fromXml[GenericStatusMessage](xml)
      }
    }
  }
}
