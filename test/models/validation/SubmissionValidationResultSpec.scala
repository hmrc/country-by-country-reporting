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

package models.validation

import generators.Generators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.Json

class SubmissionValidationResultSpec extends AnyFreeSpec with Generators with OptionValues with Matchers with ScalaCheckPropertyChecks {

  "SubmissionValidationResult" - {

    "must serialise and de-serialise SubmissionValidationSuccess as expected" in {
      val requestModel: SubmissionValidationSuccess = arbitrary[SubmissionValidationSuccess].sample.value
      Json
        .toJson(requestModel)
        .as[SubmissionValidationSuccess] mustBe requestModel
    }

    "must serialise and de-serialise SubmissionValidationFailure as expected" in {
      val requestModel: SubmissionValidationFailure = arbitrary[SubmissionValidationFailure].sample.value
      Json
        .toJson(requestModel)
        .as[SubmissionValidationFailure] mustBe requestModel
    }

    "must serialise and de-serialise InvalidXmlError as expected" in {
      val requestModel: InvalidXmlError = arbitrary[InvalidXmlError].sample.value
      Json
        .toJson(requestModel)
        .as[InvalidXmlError] mustBe requestModel
    }

    "must serialise and de-serialise as expected" in {
      forAll(arbitrary[SubmissionValidationResult]) { requestModel =>
        Json
          .toJson(requestModel)
          .as[SubmissionValidationResult] mustBe requestModel
      }
    }

  }
}
