/*
 * Copyright 2024 HM Revenue & Customs
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

package generators

import models.agentSubscription._
import models.email.EmailRequest
import models.sdes.NotificationType.{FileProcessingFailure, NotificationType}
import models.sdes._
import models.submission._
import models.subscription._
import models.upscan.UploadId
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset, ZonedDateTime}

trait ModelGenerators {
  self: Generators =>

  val Regime: String            = "CBC"
  val OriginatingSystem: String = "MDTP"
  val AgentIdType: String       = "ARN"

  implicit lazy val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 1, 1))
  }

  implicit val arbitraryShort: Arbitrary[Short] = Arbitrary(Gen.chooseNum(0, Short.MaxValue))

  implicit val arbitraryAlgorithm: Arbitrary[Algorithm] = Arbitrary(Gen.oneOf(Algorithm.values))

  implicit val arbitraryRequestCommonForSubscription: Arbitrary[RequestCommonForSubscription] =
    Arbitrary {
      for {
        receiptDate        <- nonEmptyString
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield RequestCommonForSubscription(
        regime = Regime,
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = OriginatingSystem,
        None
      )
    }

  implicit val arbitraryAgentRequestCommonForSubscription: Arbitrary[AgentRequestCommonForSubscription] =
    Arbitrary {
      for {
        receiptDate        <- nonEmptyString
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield AgentRequestCommonForSubscription(
        regime = Regime,
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = OriginatingSystem,
        None
      )
    }

  implicit val arbitraryReadSubscriptionRequestDetail: Arbitrary[ReadSubscriptionRequestDetail] = Arbitrary {
    for {
      idType   <- nonEmptyString
      idNumber <- nonEmptyString
    } yield ReadSubscriptionRequestDetail(
      IDType = idType,
      IDNumber = idNumber
    )
  }

  implicit val arbitraryAgentReadSubscriptionRequestDetail: Arbitrary[AgentReadSubscriptionRequestDetail] = Arbitrary {
    Gen.alphaNumStr.map(idNumber => AgentReadSubscriptionRequestDetail(IDType = AgentIdType, IDNumber = idNumber))
  }

  implicit val arbitraryReadSubscriptionForCBCRequest: Arbitrary[DisplaySubscriptionForCBCRequest] =
    Arbitrary {
      for {
        requestCommon <- arbitrary[RequestCommonForSubscription]
        requestDetail <- arbitrary[ReadSubscriptionRequestDetail]
      } yield DisplaySubscriptionForCBCRequest(
        DisplaySubscriptionDetails(requestCommon, requestDetail)
      )
    }

  implicit val arbitraryOrganisationDetails: Arbitrary[OrganisationDetails] = Arbitrary {
    for {
      orgName <- nonEmptyString
    } yield OrganisationDetails(orgName)
  }

  implicit val arbitraryAgentDetails: Arbitrary[AgentDetails] = Arbitrary {
    nonEmptyString.map(AgentDetails.apply)
  }

  implicit val arbitraryContactInformation: Arbitrary[ContactInformation] = Arbitrary {
    for {
      contactType <- arbitrary[OrganisationDetails]
      email       <- validEmailAddress
      phone       <- Gen.option(validContactNumber)
      mobile      <- Gen.option(validContactNumber)
    } yield ContactInformation(contactType, email, phone, mobile)
  }

  implicit val arbitraryAgentContactInformation: Arbitrary[AgentContactInformation] = Arbitrary {
    for {
      contactType <- arbitrary[AgentDetails]
      email       <- validEmailAddress
      phone       <- Gen.option(validContactNumber)
      mobile      <- Gen.option(validContactNumber)
    } yield AgentContactInformation(contactType, email, phone, mobile)
  }

  implicit val arbitraryRequestDetail: Arbitrary[RequestDetailForUpdate] = Arbitrary {
    for {
      idType           <- nonEmptyString
      idNumber         <- nonEmptyString
      tradingName      <- Gen.option(nonEmptyString)
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[ContactInformation]
      secondaryContact <- Gen.option(arbitrary[ContactInformation])
    } yield RequestDetailForUpdate(idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryAgentRequestDetailForUpdate: Arbitrary[AgentRequestDetailForUpdate] = Arbitrary {
    for {
      idNumber         <- nonEmptyString
      tradingName      <- Gen.option(nonEmptyString)
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[AgentContactInformation]
      secondaryContact <- Gen.option(arbitrary[AgentContactInformation])
    } yield AgentRequestDetailForUpdate(AgentIdType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryRequestCommonForUpdateSubscription: Arbitrary[RequestCommonForUpdate] =
    Arbitrary {
      for {
        receiptDate        <- nonEmptyString
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield RequestCommonForUpdate(
        regime = Regime,
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = OriginatingSystem,
        None
      )
    }

  implicit val arbitraryAgentRequestCommonForUpdate: Arbitrary[AgentRequestCommonForUpdate] =
    Arbitrary {
      for {
        receiptDate        <- nonEmptyString
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield AgentRequestCommonForUpdate(
        regime = Regime,
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = OriginatingSystem,
        None
      )
    }

  implicit val arbitraryUpdateSubscriptionRequestDetail: Arbitrary[UpdateSubscriptionDetails] = Arbitrary {
    for {
      reqCommonForUpdate        <- arbitrary[RequestCommonForUpdate]
      reqRequestDetailForUpdate <- arbitrary[RequestDetailForUpdate]
    } yield UpdateSubscriptionDetails(
      reqCommonForUpdate,
      reqRequestDetailForUpdate
    )
  }

  implicit val arbitraryUpdateAgentSubscriptionDetails: Arbitrary[UpdateAgentSubscriptionDetails] = Arbitrary {
    for {
      reqCommonForUpdate        <- arbitrary[AgentRequestCommonForUpdate]
      reqRequestDetailForUpdate <- arbitrary[AgentRequestDetailForUpdate]
    } yield UpdateAgentSubscriptionDetails(
      reqCommonForUpdate,
      reqRequestDetailForUpdate
    )
  }

  implicit val arbitraryUpdateSubscriptionForCBCRequest: Arbitrary[UpdateSubscriptionForCBCRequest] =
    Arbitrary {
      arbitrary[UpdateSubscriptionDetails].map(UpdateSubscriptionForCBCRequest.apply)
    }

  implicit val arbitraryAgentUpdateSubscriptionForCBCRequest: Arbitrary[UpdateAgentSubscriptionForCBCRequest] =
    Arbitrary {
      arbitrary[UpdateAgentSubscriptionDetails].map(UpdateAgentSubscriptionForCBCRequest.apply)
    }

  implicit val arbitraryCreateAgentSubscriptionRequest: Arbitrary[CreateAgentSubscriptionRequest] =
    Arbitrary {
      for {
        requestCommon <- arbitrary[AgentRequestCommonForSubscription]
        requestDetail <- arbitrary[AgentRequestDetail]
      } yield CreateAgentSubscriptionRequest(AgentCreateSubscriptionRequest(requestCommon, requestDetail))
    }

  implicit val arbitraryAgentRequestDetail: Arbitrary[AgentRequestDetail] = Arbitrary {
    for {
      idNumber         <- nonEmptyString
      tradingName      <- Gen.option(nonEmptyString)
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[AgentContactInformation]
      secondaryContact <- Gen.option(arbitrary[AgentContactInformation])
    } yield AgentRequestDetail(AgentIdType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryEmailRequest: Arbitrary[EmailRequest] = Arbitrary {
    for {
      to     <- Gen.listOf(validEmailAddress)
      id     <- nonEmptyString
      params <- arbitrary[Map[String, String]]
    } yield EmailRequest(to, id, params)
  }

  implicit val arbitraryIndividualContact: Arbitrary[Individual] =
    Arbitrary {
      for {
        firstName  <- nonEmptyString
        lastName   <- nonEmptyString
        middleName <- Gen.option(nonEmptyString)
      } yield Individual(firstName, lastName, middleName)
    }

  implicit val arbitraryOrganisationContact: Arbitrary[Organisation] =
    Arbitrary {
      for {
        name <- nonEmptyString
      } yield Organisation(name)
    }

  implicit val arbitraryContact: Arbitrary[Contact] =
    Arbitrary {
      for {
        email               <- validEmailAddress
        individualContact   <- Gen.option(arbitrary[Individual])
        organisationContact <- Gen.option(arbitrary[Organisation])
        phone               <- Gen.option(validContactNumber)
        mobile              <- Gen.option(validContactNumber)
      } yield Contact(email, individualContact, organisationContact, phone, mobile)
    }

  implicit val arbitraryCreateAgentSubscriptionEtmpRequest: Arbitrary[AgentSubscriptionEtmpRequest] =
    Arbitrary {
      for {
        idNumber         <- nonEmptyString
        tradingName      <- Gen.option(nonEmptyString)
        isGBUser         <- arbitrary[Boolean]
        primaryContact   <- arbitrary[Contact]
        secondaryContact <- Gen.option(arbitrary[Contact])
      } yield AgentSubscriptionEtmpRequest(AgentIdType, idNumber, isGBUser, primaryContact, tradingName, secondaryContact)
    }

  implicit val arbitraryProperty: Arbitrary[Property] = Arbitrary {
    for {
      name  <- nonEmptyString
      value <- nonEmptyString
    } yield Property(name, value)
  }

  implicit val arbitraryCheckSum: Arbitrary[Checksum] = Arbitrary {
    for {
      algorithm <- arbitrary[Algorithm]
      value     <- nonEmptyString
    } yield Checksum(algorithm, value)
  }

  implicit val arbitraryFile: Arbitrary[models.sdes.File] = Arbitrary {
    for {
      recipientOrSender <- arbitrary[Option[String]]
      name              <- nonEmptyString
      location          <- arbitrary[Option[String]]
      checksum          <- arbitrary[Checksum]
      size              <- arbitrary[Int]
      properties        <- arbitrary[List[Property]]
    } yield models.sdes.File(recipientOrSender, name, location, checksum, size, properties)
  }

  implicit val arbitraryReportType: Arbitrary[ReportType] = Arbitrary {
    Gen.oneOf(ReportType.values)
  }

  implicit val arbitraryFileStatus: Arbitrary[FileStatus] = Arbitrary {
    Gen.oneOf(FileStatus.values)
  }

  implicit val arbitraryFileDetails: Arbitrary[models.submission.FileDetails] = Arbitrary {
    for {
      _id                 <- arbitrary[ConversationId]
      subscriptionId      <- nonEmptyString
      messageRefId        <- nonEmptyString
      reportingEntityName <- nonEmptyString
      reportType          <- arbitrary[ReportType]
      status              <- arbitrary[FileStatus]
      name                <- nonEmptyString
      submitted           <- arbitrary[LocalDateTime]
      lastUpdated         <- arbitrary[LocalDateTime]
    } yield models.submission.FileDetails(
      _id,
      subscriptionId,
      messageRefId,
      reportingEntityName,
      reportType,
      status,
      name,
      submitted,
      lastUpdated
    )
  }

  val arbitraryPendingFileDetails: Arbitrary[models.submission.FileDetails] = Arbitrary {
    for {
      _id                 <- arbitrary[ConversationId]
      subscriptionId      <- nonEmptyString
      messageRefId        <- nonEmptyString
      reportingEntityName <- nonEmptyString
      reportType          <- arbitrary[ReportType]
      name                <- nonEmptyString
      submitted           <- arbitrary[LocalDateTime]
      lastUpdated         <- arbitrary[LocalDateTime]
    } yield models.submission.FileDetails(
      _id,
      subscriptionId,
      messageRefId,
      reportingEntityName,
      reportType,
      Pending,
      name,
      submitted,
      lastUpdated
    )
  }

  val arbitraryNonPendingFileDetails: Arbitrary[models.submission.FileDetails] = Arbitrary {
    for {
      _id                 <- arbitrary[ConversationId]
      subscriptionId      <- nonEmptyString
      messageRefId        <- nonEmptyString
      reportingEntityName <- nonEmptyString
      reportType          <- arbitrary[ReportType]
      status              <- arbitrary[FileStatus].suchThat(_ != Pending)
      name                <- nonEmptyString
      submitted           <- arbitrary[LocalDateTime]
      lastUpdated         <- arbitrary[LocalDateTime]
    } yield models.submission.FileDetails(
      _id,
      subscriptionId,
      messageRefId,
      reportingEntityName,
      reportType,
      status,
      name,
      submitted,
      lastUpdated
    )
  }

  implicit val arbitraryAudit: Arbitrary[Audit] = Arbitrary {
    nonEmptyString.map(Audit.apply)
  }

  implicit val arbitraryFileTransferNotification: Arbitrary[FileTransferNotification] = Arbitrary {
    for {
      informationType <- nonEmptyString
      file            <- arbitrary[models.sdes.File]
      audit           <- arbitrary[Audit]
    } yield FileTransferNotification(informationType, file, audit)
  }

  implicit val arbitraryResponseDetail: Arbitrary[ResponseDetail] = Arbitrary {
    for {
      subscriptionId   <- validSubscriptionID
      tradingName      <- Gen.option(nonEmptyString)
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[ContactInformation]
      secondaryContact <- Gen.option(arbitrary[ContactInformation])
    } yield ResponseDetail(subscriptionId, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryMessageSpecData: Arbitrary[MessageSpecData] = Arbitrary {
    for {
      messageRefId        <- nonEmptyString
      messageTypeIndic    <- Gen.oneOf(MessageTypeIndic.values)
      reportingEntityName <- nonEmptyString
      reporterType        <- Gen.oneOf(ReportType.values)
    } yield MessageSpecData(messageRefId, messageTypeIndic, reportingEntityName, reporterType)
  }

  implicit val arbitrarySubmissionDetails: Arbitrary[SubmissionDetails] = Arbitrary {
    for {
      file            <- arbitrary[models.sdes.File]
      uploadId        <- nonEmptyString
      enrolmentId     <- nonEmptyString
      documentUrl     <- nonEmptyString
      messageSpecData <- arbitrary[MessageSpecData]
    } yield SubmissionDetails(file.name, UploadId(uploadId), enrolmentId, file.size, documentUrl, file.checksum.value, messageSpecData)
  }

  implicit val arbitraryAgentResponseDetail: Arbitrary[AgentResponseDetail] = Arbitrary {
    for {
      subscriptionId   <- validSubscriptionID
      tradingName      <- Gen.option(nonEmptyString)
      isGbUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[ContactInformation]
      secondaryContact <- Gen.option(arbitrary[ContactInformation])
    } yield AgentResponseDetail(subscriptionId, tradingName, isGbUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryConversationId: Arbitrary[ConversationId] = Arbitrary {
    Gen.uuid.map(uuid => ConversationId.fromUploadId(UploadId.apply(uuid.toString)))
  }

  implicit val arbitraryNotification: Arbitrary[NotificationType] = Arbitrary {
    Gen.oneOf(NotificationType.values)
  }

  private val minEpochSecond = ZonedDateTime.of(1900, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC).toEpochSecond
  private val maxEpochSecond = ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 999999999, ZoneOffset.UTC).toEpochSecond
  implicit val zonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary {
    for {
      epochSecond <- Gen.choose(minEpochSecond, maxEpochSecond)
      zoneOffset  <- Gen.const(ZoneOffset.UTC)
    } yield ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), zoneOffset)
  }

  implicit val arbitrarySdesCallback: Arbitrary[SdesCallback] = Arbitrary {
    for {
      notification  <- arbitrary[NotificationType]
      filename      <- nonEmptyString
      algorithm     <- arbitrary[Algorithm]
      checksum      <- nonEmptyString
      correlationID <- arbitrary[ConversationId]
      dateTime      <- Gen.option(zonedDateTime.arbitrary)
      failureReason <- Gen.option(nonEmptyString)
    } yield SdesCallback(notification, filename, algorithm, checksum, correlationID, dateTime, failureReason)
  }

  val arbitraryFailureSdesCallback: Arbitrary[SdesCallback] = Arbitrary {
    for {
      notification  <- Gen.const(FileProcessingFailure)
      filename      <- nonEmptyString
      virus         <- Gen.const("virus")
      otherFailure  <- nonEmptyString
      algorithm     <- arbitrary[Algorithm]
      checksum      <- nonEmptyString
      correlationID <- arbitrary[ConversationId]
      dateTime      <- Gen.option(zonedDateTime.arbitrary)
      failureReason <- Gen.oneOf(Some(virus), Some(otherFailure), None)
    } yield SdesCallback(notification, filename, algorithm, checksum, correlationID, dateTime, failureReason)
  }

  val arbitrarySuccessSdesCallback: Arbitrary[SdesCallback] = Arbitrary {
    for {
      notification  <- Gen.const(NotificationType.FileProcessed)
      filename      <- nonEmptyString
      algorithm     <- arbitrary[Algorithm]
      checksum      <- nonEmptyString
      correlationID <- arbitrary[ConversationId]
      dateTime      <- Gen.option(zonedDateTime.arbitrary)
    } yield SdesCallback(notification, filename, algorithm, checksum, correlationID, dateTime)
  }
}
