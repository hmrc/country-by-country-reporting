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

package config

import base.SpecBase
import models.sdes.Algorithm

class AppConfigSpec extends SpecBase {
  val appConfig = app.injector.instanceOf[AppConfig]

  "AppConfig" - {
    "should load configuration values" in {

      appConfig.emailOrganisationSuccessfulTemplate mustBe "cbc_file_upload_successful"
      appConfig.emailOrganisationUnsuccessfulTemplate mustBe "cbc_file_upload_unsuccessful"
      appConfig.emailAgentSuccessfulTemplate mustBe "cbc_agent_file_upload_successful"
      appConfig.emailAgentUnsuccessfulTemplate mustBe "cbc_agent_file_upload_unsuccessful"

      appConfig.maxNormalFileSizeBytes mustBe 3145728L
      appConfig.maxLargeFileSizeBytes mustBe 104857600L

      appConfig.sdesRecipientOrSender mustBe "cbc-reporting"
      appConfig.sdesInformationType mustBe "cbc-report"

      appConfig.sdesChecksumAlgorithm mustBe Algorithm("SHA-256")

    }

  }
}
