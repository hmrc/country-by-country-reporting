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

package generators

import models.agentSubscription._
import models.email.EmailRequest
import models.subscription._
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.{Arbitrary, Gen}

import java.time.LocalDate

trait ModelGenerators {
  self: Generators =>

  implicit lazy val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary {
    datesBetween(LocalDate.of(1900, 1, 1), LocalDate.of(2100, 1, 1))
  }

  implicit val arbitraryRequestCommonForSubscription: Arbitrary[RequestCommonForSubscription] =
    Arbitrary {
      for {
        receiptDate        <- arbitrary[String]
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield RequestCommonForSubscription(
        regime = "CBC",
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = "MDTP",
        None
      )
    }

  implicit val arbitraryAgentRequestCommonForSubscription: Arbitrary[AgentRequestCommonForSubscription] =
    Arbitrary {
      for {
        receiptDate <- arbitrary[String]
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield AgentRequestCommonForSubscription(
        regime = "CBC",
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = "MDTP",
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
    for {
      idNumber <- arbitrary[String]
    } yield AgentReadSubscriptionRequestDetail(
      IDType = "ARN",
      IDNumber = idNumber
    )
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

  implicit val arbitraryReadAgentSubscriptionForCBCRequest: Arbitrary[DisplayAgentSubscriptionForCBCRequest] =
    Arbitrary {
      for {
        requestCommon <- arbitrary[AgentRequestCommonForSubscription]
        requestDetail <- arbitrary[AgentReadSubscriptionRequestDetail]
      } yield DisplayAgentSubscriptionForCBCRequest(
        DisplayAgentSubscriptionDetails(requestCommon, requestDetail)
      )
    }

  implicit val arbitraryOrganisationDetails: Arbitrary[OrganisationDetails] = Arbitrary {
    for {
      orgName <- arbitrary[String]
    } yield OrganisationDetails(orgName)
  }

  implicit val arbitraryAgentDetails: Arbitrary[AgentDetails] = Arbitrary {
    for {
      orgName <- arbitrary[String]
    } yield AgentDetails(orgName)
  }

  implicit val arbitraryContactInformation: Arbitrary[ContactInformation] = Arbitrary {
    for {
      contactType <- arbitrary[OrganisationDetails]
      email       <- arbitrary[String]
      phone       <- Gen.option(arbitrary[String])
      mobile      <- Gen.option(arbitrary[String])
    } yield ContactInformation(contactType, email, phone, mobile)
  }

  implicit val arbitraryAgentContactInformation: Arbitrary[AgentContactInformation] = Arbitrary {
    for {
      contactType <- arbitrary[AgentDetails]
      email <- arbitrary[String]
      phone <- Gen.option(arbitrary[String])
      mobile <- Gen.option(arbitrary[String])
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
      idNumber <- arbitrary[String]
      tradingName <- Gen.option(arbitrary[String])
      isGBUser <- arbitrary[Boolean]
      primaryContact <- arbitrary[AgentContactInformation]
      secondaryContact <- Gen.option(arbitrary[AgentContactInformation])
    } yield AgentRequestDetailForUpdate("ARN", idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryRequestCommonForUpdateSubscription: Arbitrary[RequestCommonForUpdate] =
    Arbitrary {
      for {
        receiptDate        <- arbitrary[String]
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield RequestCommonForUpdate(
        regime = "CBC",
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = "MDTP",
        None
      )
    }

  implicit val arbitraryAgentRequestCommonForUpdate: Arbitrary[AgentRequestCommonForUpdate] =
    Arbitrary {
      for {
        receiptDate <- arbitrary[String]
        acknowledgementRef <- stringsWithMaxLength(32)
      } yield AgentRequestCommonForUpdate(
        regime = "CBC",
        receiptDate = receiptDate,
        acknowledgementReference = acknowledgementRef,
        originatingSystem = "MDTP",
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
      reqCommonForUpdate <- arbitrary[AgentRequestCommonForUpdate]
      reqRequestDetailForUpdate <- arbitrary[AgentRequestDetailForUpdate]
    } yield UpdateAgentSubscriptionDetails(
      reqCommonForUpdate,
      reqRequestDetailForUpdate
    )
  }

  implicit val arbitraryUpdateSubscriptionForCBCRequest: Arbitrary[UpdateSubscriptionForCBCRequest] =
    Arbitrary {
      for {
        request <- arbitrary[UpdateSubscriptionDetails]
      } yield UpdateSubscriptionForCBCRequest(
        request
      )
    }

  implicit val arbitraryAgentUpdateSubscriptionForCBCRequest: Arbitrary[UpdateAgentSubscriptionForCBCRequest] =
    Arbitrary {
      for {
        request <- arbitrary[UpdateAgentSubscriptionDetails]
      } yield UpdateAgentSubscriptionForCBCRequest(
        request
      )
    }

  implicit val arbitraryCreateAgentSubscriptionRequest
  : Arbitrary[CreateAgentSubscriptionRequest] =
    Arbitrary {
      for {
        requestCommon <- arbitrary[AgentRequestCommonForSubscription]
        requestDetail <- arbitrary[AgentRequestDetail]
      } yield CreateAgentSubscriptionRequest(
        AgentCreateSubscriptionRequest(requestCommon, requestDetail)
      )
    }

  implicit val arbitraryAgentRequestDetail: Arbitrary[AgentRequestDetail] = Arbitrary {
    for {
      idNumber <- arbitrary[String]
      tradingName <- Gen.option(arbitrary[String])
      isGBUser <- arbitrary[Boolean]
      primaryContact <- arbitrary[AgentContactInformation]
      secondaryContact <- Gen.option(arbitrary[AgentContactInformation])
    } yield AgentRequestDetail("ARN", idNumber, tradingName, isGBUser, primaryContact, secondaryContact)
  }

  implicit val arbitraryEmailRequest: Arbitrary[EmailRequest] = Arbitrary {
    for {
      to <- arbitrary[List[String]]
      id <- arbitrary[String]
      params <- arbitrary[Map[String, String]]

    } yield EmailRequest(to, id, params)
  }

  implicit val arbitraryIndividualContact: Arbitrary[Individual] = {
    Arbitrary {
      for {
        firstName <- arbitrary[String]
        lastName <- arbitrary[String]
        middleName <- Gen.option(arbitrary[String])
      } yield Individual(firstName, lastName, middleName)
    }
  }

  implicit val arbitraryOrganisationContact: Arbitrary[Organisation] =
    Arbitrary { arbitrary[String].map(Organisation(_)) }

  implicit val arbitraryContact: Arbitrary[Contact] =
    Arbitrary {
      for {
        email <- arbitrary[String]
        individualContact <- Gen.option(arbitrary[Individual])
        organisationContact <- Gen.option(arbitrary[Organisation])
        phone <- Gen.option(arbitrary[String])
        mobile <- Gen.option(arbitrary[String])
      } yield Contact(s"$email@domain.com", individualContact, organisationContact, phone, mobile)
    }

  implicit val arbitraryCreateAgentSubscriptionEtmpRequest
  : Arbitrary[AgentSubscriptionEtmpRequest] =
    Arbitrary {
      for {
        idNumber <- arbitrary[String]
        tradingName <- Gen.option(arbitrary[String])
        isGBUser <- arbitrary[Boolean]
        primaryContact <- arbitrary[Contact]
        secondaryContact <- Gen.option(arbitrary[Contact])
      } yield AgentSubscriptionEtmpRequest("ARN", idNumber, isGBUser, primaryContact, tradingName, secondaryContact)
    }
}
