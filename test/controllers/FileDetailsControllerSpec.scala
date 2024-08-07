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

package controllers

import base.SpecBase
import controllers.auth.{FakeIdentifierAuthAction, IdentifierAuthAction}
import models.submission._
import models.upscan.UploadId
import models.xml.ValidationErrors
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, route, status, writeableOf_AnyContentAsEmpty, GET}
import repositories.submission.FileDetailsRepository

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future

class FileDetailsControllerSpec extends SpecBase with BeforeAndAfterEach {

  val mockFileDetailsRepository: FileDetailsRepository = mock[FileDetailsRepository]

  override def beforeEach(): Unit = reset(mockFileDetailsRepository)

  private val conversationId = ConversationId.fromUploadId(UploadId(UUID.randomUUID().toString))

  val application: Application = applicationBuilder()
    .overrides(
      bind[FileDetailsRepository].toInstance(mockFileDetailsRepository),
      bind[IdentifierAuthAction].to[FakeIdentifierAuthAction]
    )
    .build()

  private val submissionTime1 = LocalDateTime.now()
  private val fileDetails1 =
    FileDetails(conversationId, "subscriptionId1", "messageRefId1", "Reporting Entity", TestData, Pending, "fileName1", submissionTime1, submissionTime1)
  private val submissionTime2 = LocalDateTime.now()
  private val fileDetails2 =
    FileDetails(conversationId, "subscriptionId1", "messageRefId1", "Reporting Entity", TestData, Accepted, "fileName2", submissionTime2, submissionTime2)
  private val files = Seq(fileDetails1, fileDetails2)

  "FileDetailsController" - {
    "must return FileDetails for the input 'conversationId'" in {
      val fileDetails = FileDetails(
        conversationId,
        subscriptionId = "subscriptionId",
        messageRefId = "messageRefId",
        reportingEntityName = "Reporting Entity",
        reportType = TestData,
        status = Pending,
        name = "test.xml",
        submitted = LocalDateTime.now(),
        lastUpdated = LocalDateTime.now()
      )

      when(mockFileDetailsRepository.findByConversationId(any[ConversationId]())).thenReturn(Future.successful(Some(fileDetails)))

      val request =
        FakeRequest(GET, routes.FileDetailsController.getFileDetails(conversationId).url)

      val result = route(application, request).value
      status(result) mustBe OK
    }

    "must return NotFound status when no record found for the input 'conversationId'" in {

      when(mockFileDetailsRepository.findByConversationId(any[ConversationId]())).thenReturn(Future.successful(None))

      val request =
        FakeRequest(GET, routes.FileDetailsController.getFileDetails(conversationId).url)

      val result = route(application, request).value
      status(result) mustBe NOT_FOUND
    }

    "Get All files should return OK when files exist" in {
      when(
        mockFileDetailsRepository
          .findBySubscriptionId(any[String]())
      ).thenReturn(
        Future.successful(files)
      )

      val request =
        FakeRequest(
          GET,
          routes.FileDetailsController.getAllFileDetails("XACBC0009234568").url
        )

      val result = route(application, request).value
      status(result) mustEqual OK

    }

    "Get All files should return NotFound when files doesn't exist" in {
      when(
        mockFileDetailsRepository
          .findBySubscriptionId(any[String]())
      ).thenReturn(
        Future.successful(Nil)
      )

      val request =
        FakeRequest(
          GET,
          routes.FileDetailsController.getAllFileDetails("XACBC0009234568").url
        )

      val result = route(application, request).value
      status(result) mustEqual NOT_FOUND

    }

    "must return Pending FileStatus for the given 'conversationId'" in {

      when(mockFileDetailsRepository.findStatusByConversationId(any[ConversationId]())).thenReturn(Future.successful(Some(Pending)))

      val request =
        FakeRequest(GET, routes.FileDetailsController.getStatus(conversationId).url)

      val result = route(application, request).value
      status(result) mustBe OK
      contentAsJson(result).as[FileStatus] mustBe Pending
    }

    "must return Accepted FileStatus for the given 'conversationId'" in {

      when(mockFileDetailsRepository.findStatusByConversationId(any[ConversationId]())).thenReturn(Future.successful(Some(Accepted)))

      val request =
        FakeRequest(GET, routes.FileDetailsController.getStatus(conversationId).url)

      val result = route(application, request).value
      status(result) mustBe OK
      contentAsJson(result).as[FileStatus] mustBe Accepted
    }

    "must return Rejected FileStatus for the given 'conversationId'" in {
      val validationErrors = ValidationErrors(None, None)
      when(mockFileDetailsRepository.findStatusByConversationId(any[ConversationId]()))
        .thenReturn(Future.successful(Some(Rejected(validationErrors))))

      val request =
        FakeRequest(GET, routes.FileDetailsController.getStatus(conversationId).url)

      val result = route(application, request).value
      status(result) mustBe OK
      contentAsJson(result).as[FileStatus] mustBe Rejected(validationErrors)
    }
  }
}
