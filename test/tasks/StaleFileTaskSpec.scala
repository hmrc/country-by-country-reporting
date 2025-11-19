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

package tasks

import base.SpecBase
import config.AppConfig
import org.apache.pekko.actor.{ActorSystem, Cancellable, Scheduler}
import org.mockito.ArgumentMatchers.{any, argThat}
import org.mockito._
import play.api.Configuration
import play.api.inject.ApplicationLifecycle
import repositories.submission.FileDetailsRepository
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.MongoLockRepository

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class StaleFileTaskSpec extends SpecBase {

  lazy val config = app.injector.instanceOf[Configuration]

  private val mockAppConfig             = mock[AppConfig]
  private val mockFileDetailsRepository = mock[FileDetailsRepository]
  private val mockMongoLockRepository   = mock[MongoLockRepository]
  private val mockTimestampSupport      = mock[TimestampSupport]
  private val mockActorSystem           = mock[ActorSystem]
  private val mockScheduler             = mock[Scheduler]
  private val mockCancellable           = mock[Cancellable]
  private val applicatinLifecycle       = app.injector.instanceOf[ApplicationLifecycle]
  when(mockAppConfig.staleTaskInterval) thenReturn 2.hours

  "StaleFileTask" - {
    "should not be scheduled when staleTaskEnabled feature flag is false" in {
      when(mockAppConfig.staleTaskEnabled) thenReturn false

      val staleFileTask = new StaleFileTask(
        mockActorSystem,
        mockFileDetailsRepository,
        applicatinLifecycle,
        mockAppConfig,
        mockMongoLockRepository,
        mockTimestampSupport
      )

      verify(mockActorSystem, never()).scheduler
    }

    "should be scheduled when staleTaskEnabled feature flag is true" in {
      when(mockAppConfig.staleTaskEnabled) thenReturn true
      when(mockActorSystem.scheduler).thenReturn(mockScheduler)

      when(
        mockScheduler.scheduleWithFixedDelay(
          any[FiniteDuration],
          any[FiniteDuration]
        )(any[Runnable])(any[ExecutionContext])
      ).thenReturn(mockCancellable)

      val staleFileTask = new StaleFileTask(
        mockActorSystem,
        mockFileDetailsRepository,
        applicatinLifecycle,
        mockAppConfig,
        mockMongoLockRepository,
        mockTimestampSupport
      )

      verify(mockScheduler, times(1)).scheduleWithFixedDelay(
        argThat((delay: FiniteDuration) => delay == 1.second),
        argThat((interval: FiniteDuration) => interval == 2.hours)
      )(
        argThat((runnable: Runnable) => runnable != null)
      )(
        argThat((ec: ExecutionContext) => ec != null)
      )
    }
  }

}
