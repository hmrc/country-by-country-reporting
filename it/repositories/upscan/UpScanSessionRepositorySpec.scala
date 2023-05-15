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

package repositories.upscan

import base.SpecBase
import config.AppConfig
import models.upscan._
import org.bson.types.ObjectId
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext.Implicits.global

class UpScanSessionRepositorySpec extends SpecBase with DefaultPlayMongoRepositorySupport[UploadSessionDetails] {


  private val uploadId = UploadId(UUID.randomUUID().toString)
  private val instant = Instant.now.truncatedTo(java.time.temporal.ChronoUnit.MILLIS)
  private val stubClock: Clock = Clock.fixed(instant, ZoneId.systemDefault)

  private val mockAppConfig = mock[AppConfig]
  when(mockAppConfig.cacheTtl) thenReturn 1

  override protected val repository = new UpScanSessionRepository(
    mongoComponent = mongoComponent,
    appConfig = mockAppConfig,
    clock = stubClock
  )

  val uploadDetails = UploadSessionDetails(
    ObjectId.get(),
    uploadId,
    Reference("xxxx"),
    Quarantined
  )

  "Insert" - {
    "must insert UploadStatus" in {
      val uploadDetails = UploadSessionDetails(
        ObjectId.get(),
        uploadId,
        Reference("xxxx"),
        Quarantined
      )
      val res = repository.insert(uploadDetails)
      whenReady(res) { result =>
        result mustBe true
      }
    }
    "must read UploadStatus" in {
      val res = repository.findByUploadId(uploadId)
      whenReady(res) {
        case Some(result) =>
          result.uploadId mustBe (uploadDetails.uploadId)
          result.reference mustBe (uploadDetails.reference)
          result.status mustBe (uploadDetails.status)
        case _ => true
      }
    }
  }
  "Update" - {
    "must update a status" in {
      val result1: Boolean = Await.result(repository.updateStatus(Reference("xxxx"), Failed), 5.seconds)
      result1 mustBe true
    }
  }
}
