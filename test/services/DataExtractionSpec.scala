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

package services

import base.SpecBase
import models.submission.{CBC401, MessageSpecData}

import scala.xml.{Elem, NodeSeq}

class DataExtractionSpec extends SpecBase {

  val dataExtraction: DataExtraction = app.injector.instanceOf[DataExtraction]

  "messageSpecData" - {
    "must return some MessageSpecData when given valid xml" in {

      val xml: Elem = {
        <file>
          <MessageRefId>MessageRefId</MessageRefId>
          <MessageTypeIndic>CBC401</MessageTypeIndic>
          <ReportingEntity>
            <Entity>
              <Name>Name</Name>
            </Entity>
          </ReportingEntity>
        </file>
      }

      dataExtraction.messageSpecData(xml) mustBe Some(MessageSpecData("MessageRefId", CBC401, "Name"))
    }

    "must return None when given invalid xml" in {

      val xml: Elem = {
        <file>
          <MessageTypeIndic>CBC401</MessageTypeIndic>
          <ReportingEntity>
            <Entity>
              <Name>Name</Name>
            </Entity>
          </ReportingEntity>
        </file>
      }
      
      dataExtraction.messageSpecData(xml) mustBe None
    }
  }
}
