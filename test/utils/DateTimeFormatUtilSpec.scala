/*
 * Copyright 2026 HM Revenue & Customs
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

package utils

import base.SpecBase

class DateTimeFormatUtilSpec extends SpecBase {

  "DateTimeFormatUtil" - {
    "should convert date string to display format" in {
      val dateString = "2023-12-25"
      val expected   = "25 December 2023"

      DateTimeFormatUtil.convertStringToDisplayDate(dateString) mustEqual expected
    }

    "should return original string if conversion fails" in {
      val invalidDateString = "invalid-date"

      DateTimeFormatUtil.convertStringToDisplayDate(invalidDateString) mustEqual invalidDateString
    }

    "should convert optional date string to display format" in {
      val optionalDateString = Some("2023-12-25")
      val expected           = Some("25 December 2023")

      DateTimeFormatUtil.convertOptionalStringToDisplayDate(optionalDateString) mustEqual expected
    }

    "should return None if optional date string is None" in {
      val optionalDateString: Option[String] = None

      DateTimeFormatUtil.convertOptionalStringToDisplayDate(optionalDateString) mustEqual None
    }
  }

}
