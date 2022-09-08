/*
 * Copyright 2022 HM Revenue & Customs
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

import connectors.EmailConnector
import models.email.{EmailRequest, EmailTemplate}
import models.error.ReadSubscriptionError
import models.subscription.OrganisationDetails
import play.api.Logging
import play.api.http.Status.{ACCEPTED, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailService @Inject()(emailConnector: EmailConnector, emailTemplate: EmailTemplate, subscriptionService: SubscriptionService)(implicit
                                                                                                                                     executionContext: ExecutionContext
) extends Logging {

  def sendAndLogEmail(subscriptionId: String, submissionTime: String, messageRefId: String, isUploadSuccessful: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[Int] = ???


  def sendEmail(subscriptionId: String, submissionTime: String, messageRefId: String, isUploadSuccessful: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[Option[HttpResponse]] = ???
}
