/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

import scala.util.{Failure, Success}
import scala.xml.Elem

class XmlHandlerSpec extends AnyFreeSpec with Matchers {

  "XmlHandler.load" - {

    "should successfully load valid XML from a URL" in {

      val resourceUrl = getClass.getResource("/sample.xml")
      resourceUrl must not be null

      val handler = new XmlHandler
      val result  = handler.load(resourceUrl.toString)

      result mustBe a[Success[_]]
      result.get mustBe a[Elem]
      (result.get \\ "note").nonEmpty mustBe true
    }

    "should fail to load when the URL is invalid" in {
      val handler = new XmlHandler
      val result  = handler.load("invalid://url")

      result mustBe a[Failure[_]]
      result.failed.get mustBe a[java.net.MalformedURLException]
    }
  }
}
