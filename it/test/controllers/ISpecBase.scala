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

package controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.i18n.Lang.logger
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.{ExecutionContext, Future}

trait ISpecBase extends AnyWordSpec with Matchers with MockitoSugar with BeforeAndAfterAll {

  implicit val system: ActorSystem                = ActorSystem()
  implicit val materializer: Materializer         = Materializer(system)
  implicit val executionContext: ExecutionContext = system.dispatcher

  def unauthorisedApp(): Application = new GuiceApplicationBuilder().build()

  override def afterAll(): Unit = {
    val termination: Future[Unit] = system.terminate().map(_ => ())
    termination.onComplete(_ => super.afterAll())
    termination.failed.foreach { ex =>
      logger.error(s"ActorSystem termination error: ${ex.getMessage}")
    }
  }

}
