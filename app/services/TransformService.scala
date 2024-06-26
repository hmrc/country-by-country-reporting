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

package services

import models.agentSubscription.{AgentContactDetails, AgentResponseDetail}
import models.submission.{NamespaceForNode, SubmissionMetaData}
import models.subscription._

import javax.inject.Inject
import scala.xml._

class TransformService @Inject() () {

  def addSubscriptionDetailsToSubmission(
    uploadedFile: NodeSeq,
    subscriptionDetails: ResponseDetail,
    metaData: SubmissionMetaData,
    agentDetails: Option[AgentContactDetails] = None
  ): NodeSeq =
    <cadx:CBCSubmissionRequest xmlns:cbc="urn:oecd:ties:cbc:v2" xmlns:cadx="http://www.hmrc.gsi.gov.uk/cbc/cadx" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.hmrc.gsi.gov.uk/cbc/cadx DCT52c_CustomerFileSubmissionMDTPToCADX_v0.1.xsd">
        <requestCommon>
          <receiptDate>{metaData.submissionTime}</receiptDate>
          <regime>CBC</regime>
          <conversationID>{metaData.conversationId.value}</conversationID>
          <schemaVersion>1.0.0</schemaVersion>
        </requestCommon>
        <requestDetail>{addNameSpaces(addNameSpaceDefinitions(uploadedFile), Seq(NamespaceForNode("CBC_OECD", "cbc"), NamespaceForNode("DocSpec", "stf")))}</requestDetail>
        <requestAdditionalDetail>{transformSubscriptionDetails(subscriptionDetails, metaData.fileName, agentDetails)}</requestAdditionalDetail>
      </cadx:CBCSubmissionRequest>

  def addNameSpaceDefinitions(submissionFile: NodeSeq): NodeSeq =
    submissionFile.collect { case elem: Elem =>
      elem.copy(scope =
        NamespaceBinding(
          "xsi",
          "http://www.w3.org/2001/XMLSchema-instance",
          NamespaceBinding("stf", "urn:oecd:ties:cbcstf:v5", NamespaceBinding("cbc", "urn:oecd:ties:cbc:v2", TopScope))
        )
      )
    }

  def transformSubscriptionDetails(
    subscriptionDetails: ResponseDetail,
    fileName: Option[String],
    agentDetails: Option[AgentContactDetails] = None
  ): NodeSeq =
    Seq(
      fileName.map(name => <fileName>{name}</fileName>),
      Some(<subscriptionID>{subscriptionDetails.subscriptionID}</subscriptionID>),
      subscriptionDetails.tradingName.filter(_.trim.nonEmpty).map(tradingName => <tradingName>{tradingName}</tradingName>),
      Some(<isGBUser>{subscriptionDetails.isGBUser}</isGBUser>),
      Some(<primaryContact>{transformContactInformation(subscriptionDetails.primaryContact)}</primaryContact>),
      subscriptionDetails.secondaryContact.map(sc => <secondaryContact>{transformContactInformation(sc)}</secondaryContact>),
      agentDetails.map { agentDetail =>
        <agentDetails>{transformAgentSubscriptionDetails(agentDetail.agentReferenceNumber, agentDetail.subscriptionDetails)}</agentDetails>
      }
    ).filter(_.isDefined).map(_.get)
  def transformAgentSubscriptionDetails(
    agentReferenceNumber: String,
    subscriptionDetails: AgentResponseDetail
  ): NodeSeq =
    Seq(
      Some(<agentReferenceNumber>{agentReferenceNumber}</agentReferenceNumber>),
      Some(<agentPrimaryContact>{transformContactInformation(subscriptionDetails.primaryContact)}</agentPrimaryContact>),
      subscriptionDetails.secondaryContact.map(sc => <agentSecondaryContact>{transformContactInformation(sc)}</agentSecondaryContact>)
    ).filter(_.isDefined).map(_.get)

  def transformContactInformation(
    contactInformation: ContactInformation
  ): NodeSeq = {

    val organisationDetails =
      Some(<organisationDetails><organisationName>{contactInformation.organisationDetails.organisationName}</organisationName></organisationDetails>)

    val nodes =
      Seq(
        contactInformation.phone.map(phone => <phoneNumber>{phone}</phoneNumber>),
        contactInformation.mobile.map(mobile => <mobileNumber>{mobile}</mobileNumber>),
        Some(<emailAddress>{contactInformation.email}</emailAddress>),
        organisationDetails
      )

    nodes.filter(_.isDefined).map(_.get)
  }

  def addNameSpaces(file: NodeSeq, namespaces: Seq[NamespaceForNode]): NodeSeq = {

    def changeNS(el: NodeSeq): NodeSeq = {
      def fixSeq(ns: Seq[Node], currentPrefix: Option[String]): Seq[Node] = for (node <- ns) yield node match {
        case elem: Elem if elem.label == "DocSpec" =>
          namespaces
            .find(n => n.nodeName == elem.label)
            .map { n =>
              elem.copy(
                prefix = "cbc",
                child = fixSeq(elem.child, Some(n.prefix))
              )
            }
            .getOrElse(
              elem.copy(
                prefix = currentPrefix.orNull,
                child = fixSeq(elem.child, currentPrefix)
              )
            )
        case elem: Elem =>
          namespaces
            .find(n => n.nodeName == elem.label)
            .map { n =>
              elem.copy(
                prefix = n.prefix,
                child = fixSeq(elem.child, Some(n.prefix))
              )
            }
            .getOrElse(
              elem.copy(
                prefix = currentPrefix.orNull,
                child = fixSeq(elem.child, currentPrefix)
              )
            )
        case other => other
      }

      fixSeq(el, None).head
    }

    changeNS(file)
  }

}
