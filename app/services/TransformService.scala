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

import models.submission.{NamespaceForNode, SubmissionMetaData}
import models.subscription._

import javax.inject.Inject
import scala.xml._

class TransformService @Inject() () {

  //TODO update cadx  schemaLocation when recievd the spec DCT72a_CBCSubmissionRequest_v0.1.xsd
  def addSubscriptionDetailsToSubmission(
    uploadedFile: NodeSeq,
    subscriptionDetails: ResponseDetail,
    metaData: SubmissionMetaData
  ): NodeSeq =
    <cadx:CBCSubmissionRequest xmlns:cbc="urn:oecd:ties:cbc:v1"
                          xmlns:cadx="http://www.hmrc.gsi.gov.uk/cbc/cadx"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="http://www.hmrc.gsi.gov.uk/cbc/cadx DCT72a_CBCSubmissionRequest_v0.1.xsd"> 

      <requestCommon>
        <receiptDate>
          {metaData.submissionTime}
        </receiptDate>
        <regime>CBC</regime>
        <conversationID>{metaData.conversationId.value}</conversationID>
        <schemaVersion>1.0.0</schemaVersion>
      </requestCommon>
      <requestDetail>
        {addNameSpaces(addNameSpaceDefinitions(uploadedFile), Seq(NamespaceForNode("CBC_OECD", "cbc")))}
      </requestDetail>
      <requestAdditionalDetail>
        {transformSubscriptionDetails(subscriptionDetails, metaData.fileName)}
      </requestAdditionalDetail>
    </cadx:CBCSubmissionRequest>

  def addNameSpaceDefinitions(submissionFile: NodeSeq): NodeSeq =
    for (node <- submissionFile) yield node match {
      case elem: Elem =>
        elem.copy(scope = NamespaceBinding("xsi", "http://www.w3.org/2001/XMLSchema-instance", NamespaceBinding("cbc", "urn:oecd:ties:cbc:v1", TopScope)))
    }

  def transformSubscriptionDetails(
    subscriptionDetails: ResponseDetail,
    fileName: Option[String]
  ): NodeSeq =
    Seq(
      fileName.map(name => <fileName>
        {name}
      </fileName>),
      Some(<subscriptionID>{subscriptionDetails.subscriptionID}</subscriptionID>),
      subscriptionDetails.tradingName.filter(_.trim.nonEmpty).map(tradingName => <tradingName>{tradingName}</tradingName>),
      Some(<isGBUser>
        {subscriptionDetails.isGBUser}
      </isGBUser>),
      Some(<primaryContact>
        {transformContactInformation(subscriptionDetails.primaryContact)}
      </primaryContact>),
      subscriptionDetails.secondaryContact.map(sc => <secondaryContact>
        {transformContactInformation(sc)}
      </secondaryContact>)
    ).filter(_.isDefined).map(_.get)

  def transformContactInformation(
    contactInformation: ContactInformation
  ): NodeSeq = {

    val organisationDetails =
      Some(<organisationDetails>
        <organisationName>{contactInformation.organisationDetails.organisationName}</organisationName>
      </organisationDetails>)

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
