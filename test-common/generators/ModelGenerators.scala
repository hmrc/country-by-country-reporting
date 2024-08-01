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
import models.sdes.{Algorithm, Audit, Checksum, FileTransferNotification, Property}
import models.submission.{ConversationId, MessageSpecData, MessageTypeIndic, ReportType, SubmissionDetails}
import models.subscription._
import models.upscan.UploadId
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import java.time.LocalDate

trait ModelGenerators {
  self: Generators =>

  val Regime: String            = "CBC"
  val OriginatingSystem: String = "MDTP"
  val AgentIdType: String       = "ARN"

  implicit lazy val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 1, 1))
  }

  implicit val arbitraryNonEmptyString: Arbitrary[String] = Arbitrary(nonEmptyString)

  implicit val arbitraryShort: Arbitrary[Short] = Arbitrary(Gen.chooseNum(0, Short.MaxValue))

  implicit val arbitraryAlgorithm: Arbitrary[Algorithm] = Arbitrary(Gen.oneOf(Algorithm.values))

  implicit val arbitraryRequestCommonForSubscription: Arbitrary[RequestCommonForSubscription] =
    Arbitrary {
      for {
        receiptDate        <- arbitrary[String]
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
        receiptDate        <- arbitrary[String]
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
      idType   <- arbitrary[String]
      idNumber <- arbitrary[String]
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
      orgName <- arbitrary[String]
    } yield OrganisationDetails(orgName)
  }

  implicit val arbitraryAgentDetails: Arbitrary[AgentDetails] = Arbitrary {
    arbitrary[String].map(AgentDetails.apply)
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
      idType           <- arbitrary[String]
      idNumber         <- arbitrary[String]
      tradingName      <- Gen.option(arbitrary[String])
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[ContactInformation]
      secondaryContact <- Gen.option(arbitrary[ContactInformation])
    } yield RequestDetailForUpdate(idType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryAgentRequestDetailForUpdate: Arbitrary[AgentRequestDetailForUpdate] = Arbitrary {
    for {
      idNumber         <- arbitrary[String]
      tradingName      <- Gen.option(arbitrary[String])
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[AgentContactInformation]
      secondaryContact <- Gen.option(arbitrary[AgentContactInformation])
    } yield AgentRequestDetailForUpdate(AgentIdType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryRequestCommonForUpdateSubscription: Arbitrary[RequestCommonForUpdate] =
    Arbitrary {
      for {
        receiptDate        <- arbitrary[String]
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
        receiptDate        <- arbitrary[String]
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
      idNumber         <- arbitrary[String]
      tradingName      <- Gen.option(arbitrary[String])
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[AgentContactInformation]
      secondaryContact <- Gen.option(arbitrary[AgentContactInformation])
    } yield AgentRequestDetail(AgentIdType, idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryEmailRequest: Arbitrary[EmailRequest] = Arbitrary {
    for {
      to     <- Gen.listOf(validEmailAddress)
      id     <- arbitrary[String]
      params <- arbitrary[Map[String, String]]
    } yield EmailRequest(to, id, params)
  }

  implicit val arbitraryIndividualContact: Arbitrary[Individual] =
    Arbitrary {
      for {
        firstName  <- arbitrary[String]
        lastName   <- arbitrary[String]
        middleName <- Gen.option(arbitrary[String])
      } yield Individual(firstName, lastName, middleName)
    }

  implicit val arbitraryOrganisationContact: Arbitrary[Organisation] =
    Arbitrary(arbitrary[String].map(Organisation(_)))

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
        idNumber         <- arbitrary[String]
        tradingName      <- Gen.option(arbitrary[String])
        isGBUser         <- arbitrary[Boolean]
        primaryContact   <- arbitrary[Contact]
        secondaryContact <- Gen.option(arbitrary[Contact])
      } yield AgentSubscriptionEtmpRequest(AgentIdType, idNumber, isGBUser, primaryContact, tradingName, secondaryContact)
    }

  implicit val arbitraryProperty: Arbitrary[Property] = Arbitrary {
    for {
      name  <- arbitrary[String]
      value <- arbitrary[String]
    } yield Property(name, value)
  }

  implicit val arbitraryCheckSum: Arbitrary[Checksum] = Arbitrary {
    for {
      algorithm <- arbitrary[Algorithm]
      value     <- arbitrary[String]
    } yield Checksum(algorithm, value)
  }

  implicit val arbitraryFile: Arbitrary[models.sdes.File] = Arbitrary {
    for {
      recipientOrSender <- arbitrary[Option[String]]
      name              <- arbitrary[String]
      location          <- arbitrary[Option[String]]
      checksum          <- arbitrary[Checksum]
      size              <- arbitrary[Int]
      properties        <- arbitrary[List[Property]]
    } yield models.sdes.File(recipientOrSender, name, location, checksum, size, properties)
  }

  implicit val arbitraryAudit: Arbitrary[Audit] = Arbitrary {
    arbitrary[String].map(Audit.apply)
  }

  implicit val arbitraryFileTransferNotification: Arbitrary[FileTransferNotification] = Arbitrary {
    for {
      informationType <- arbitrary[String]
      file            <- arbitrary[models.sdes.File]
      audit           <- arbitrary[Audit]
    } yield FileTransferNotification(informationType, file, audit)
  }

  implicit val arbitraryResponseDetail: Arbitrary[ResponseDetail] = Arbitrary {
    for {
      subscriptionId   <- validSubscriptionID
      tradingName      <- Gen.option(arbitrary[String])
      isGBUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[ContactInformation]
      secondaryContact <- Gen.option(arbitrary[ContactInformation])
    } yield ResponseDetail(subscriptionId, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryMessageSpecData: Arbitrary[MessageSpecData] = Arbitrary {
    for {
      messageRefId        <- arbitrary[String]
      messageTypeIndic    <- Gen.oneOf(MessageTypeIndic.values)
      reportingEntityName <- arbitrary[String]
      reporterType        <- Gen.oneOf(ReportType.values)
    } yield MessageSpecData(messageRefId, messageTypeIndic, reportingEntityName, reporterType)
  }

  implicit val arbitrarySubmissionDetails: Arbitrary[SubmissionDetails] = Arbitrary {
    for {
      file            <- arbitrary[models.sdes.File]
      uploadId        <- arbitrary[String]
      enrolmentId     <- arbitrary[String]
      documentUrl     <- arbitrary[String]
      messageSpecData <- arbitrary[MessageSpecData]
    } yield SubmissionDetails(file.name, UploadId(uploadId), enrolmentId, file.size, documentUrl, file.checksum.value, messageSpecData)
  }

  implicit val arbitraryAgentResponseDetail: Arbitrary[AgentResponseDetail] = Arbitrary {
    for {
      subscriptionId   <- validSubscriptionID
      tradingName      <- Gen.option(arbitrary[String])
      isGbUser         <- arbitrary[Boolean]
      primaryContact   <- arbitrary[ContactInformation]
      secondaryContact <- Gen.option(arbitrary[ContactInformation])
    } yield AgentResponseDetail(subscriptionId, tradingName, isGbUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryConversationId: Arbitrary[ConversationId] = Arbitrary {
    Gen.uuid.map(uuid => ConversationId.fromUploadId(UploadId.apply(uuid.toString)))
  }
}
