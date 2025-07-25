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

package config

import models.sdes.Algorithm

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.duration.FiniteDuration

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val appName: String = config.get[String]("appName")

  val eisResponseWaitTime: Int = config.get[Int]("microservice.services.eis-response.wait-time")

  def serviceUrl(serviceName: String): String =
    s"${servicesConfig.baseUrl(serviceName)}${servicesConfig.getString(s"microservice.services.$serviceName.context")}"

  val bearerToken: String => String = (serviceName: String) => config.get[String](s"microservice.services.$serviceName.bearer-token")
  val environment: String => String = (serviceName: String) => config.get[String](s"microservice.services.$serviceName.environment")

  val enrolmentKey: String => String = (serviceName: String) => config.get[String](s"enrolmentKeys.$serviceName.key")
  val enrolmentId: String => String  = (serviceName: String) => config.get[String](s"enrolmentKeys.$serviceName.identifier")

  val fileUploadXSDFilePath: String  = config.get[String]("xsd-files.fileUpload_CBC_XSDFile")
  val submissionXSDFilePath: String  = config.get[String]("xsd-files.submission_DCT52c_XSDFile")
  val eisResponseXSDFilePath: String = config.get[String]("xsd-files.eisResponse_DCT52B_XSDFile")

  lazy val sendEmailUrl: String = servicesConfig.baseUrl("email")

  lazy val cacheTtl: Int      = config.get[Int]("mongodb.timeToLiveInSeconds")
  lazy val submissionTtl: Int = config.get[Int]("mongodb.submission.timeToLiveInDays")

  lazy val emailOrganisationSuccessfulTemplate: String   = config.get[String]("emailTemplates.fileOrganisationUploadSuccessful")
  lazy val emailOrganisationUnsuccessfulTemplate: String = config.get[String]("emailTemplates.fileOrganisationUploadUnsuccessful")
  lazy val emailAgentSuccessfulTemplate: String          = config.get[String]("emailTemplates.fileAgentUploadSuccessful")
  lazy val emailAgentUnsuccessfulTemplate: String        = config.get[String]("emailTemplates.fileAgentUploadUnsuccessful")

  lazy val maxNormalFileSizeBytes: Long = config.get[Long]("max-normal-file-size-bytes")
  lazy val maxLargeFileSizeBytes: Long  = config.get[Long]("max-large-file-size-bytes")

  lazy val sdesClientId: String = config.get[String]("sdes.client-id")

  lazy val sdesRecipientOrSender: String   = config.get[String]("sdes.recipient-or-sender")
  lazy val sdesInformationType: String     = config.get[String]("sdes.information-type")
  private val sdesLocation: Option[String] = Option(config.get[String]("sdes.location")).filter(_.nonEmpty)
  lazy val sdesUrl: String = List(Option(servicesConfig.baseUrl("sdes")), sdesLocation, Some("notification"), Some("fileready")).flatten.mkString("/")

  lazy val sdesChecksumAlgorithm: Algorithm = Algorithm(config.get[String]("sdes.checksum-algorithm"))

  val staleTaskEnabled: Boolean           = config.get[Boolean]("tasks.staleFiles.enabled")
  val staleTaskInterval: FiniteDuration   = config.get[FiniteDuration]("tasks.staleFiles.interval")
  val staleTaskAlertAfter: FiniteDuration = config.get[FiniteDuration]("tasks.staleFiles.alertAfter")
}
