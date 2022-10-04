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

package helpers

import base.SpecBase
import models.validation.{GenericError, Message, SaxParseError}

import scala.collection.mutable.ListBuffer

class XmlErrorMessageHelperSpec extends SpecBase {

  val lineNumber = 20
  val over400    = "s" * 401
  val over200    = "s" * 201
  val over4000   = "s" * 2000 + "\n" + "s" * 2001

  val helper = new XmlErrorMessageHelper
  "ErrorMessageHelper" - {

    "generateErrorMessages" - {

      "when element contains 'cbc:' prefix" - {

        "must return correct error for missing element declaration'" in {
          val missingDeclaration = SaxParseError(lineNumber, "cvc-elt.1.a: Cannot find the declaration of element 'cbc:CBC_OECD'.")
          val result             = helper.generateErrorMessages(ListBuffer(missingDeclaration))
          result mustBe List(GenericError(lineNumber, Message("xml.must.have.element.declaration", List("CBC_OECD", "urn:oecd:ties:cbc:v2"))))
        }

        "must return correct error for missing attribute error'" in {
          val missingAttributeError = SaxParseError(lineNumber, "cvc-complex-type.4: Attribute 'currCode' must appear on element 'cbc:Amount'.")
          val result                = helper.generateErrorMessages(ListBuffer(missingAttributeError))
          result mustBe List(GenericError(lineNumber, Message("xml.add.attribute", List("Amount currCode"))))
        }

        "must return correct error for too long field attribute error for xnlNameType" in {
          val tooLongValue = "1" * 200
          val invalidEnumError1 =
            SaxParseError(
              lineNumber,
              s"cvc-maxLength-valid: Value '$tooLongValue' with length = '210' is not facet-valid with respect to maxLength '200' for type 'xnlNameType'."
            )
          val invalidEnumError2 =
            SaxParseError(
              lineNumber,
              s"cvc-attribute.3: The value '$tooLongValue' of attribute 'xnlNameType' on element 'cbc:FirstName' is not valid with respect to its type, 'StringMin1Max200_Type'."
            )
          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("FirstName xnlNameType", "200"))))
        }

        "must return correct error for too long field attribute error for INType" in {
          val tooLongValue = "1" * 200
          val invalidEnumError1 =
            SaxParseError(
              lineNumber,
              s"cvc-maxLength-valid: Value '$tooLongValue' with length = '210' is not facet-valid with respect to maxLength '200' for type 'INType'."
            )
          val invalidEnumError2 =
            SaxParseError(
              lineNumber,
              s"cvc-attribute.3: The value '$tooLongValue' of attribute 'INType' on element 'cbc:IN' is not valid with respect to its type, 'StringMin1Max200_Type'."
            )
          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("INType", "200"))))
        }

        "must return correct error for invalid enum error for attribute" in {
          val invalidEnumError1 =
            SaxParseError(
              lineNumber,
              "cvc-enumeration-valid: Value 'GBf' is not facet-valid with respect to enumeration '[AF, AX]'. It must be a value from the enumeration."
            )
          val invalidEnumError2 =
            SaxParseError(
              lineNumber,
              "cvc-attribute.3: The value 'GBf' of attribute 'issuedBy' on element 'cbc:TIN' is not valid with respect to its type, 'CountryCode_Type'."
            )
          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.ISO.code", List("TIN issuedBy"))))
        }

        "must return correct error for missing element error'" in {

          val missingElementError1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max200_Type'."
            )

          val missingElementError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'cbc:ResCountryCode' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(missingElementError1, missingElementError2))
          result mustBe List(GenericError(lineNumber, Message("xml.add.a.element", List("ResCountryCode"))))
        }

        "must return correct error for missing optional element error when tags exist but are empty'" in {

          val missingElementError1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."
            )

          val missingElementError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'cbc:Street' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(missingElementError1, missingElementError2))
          result mustBe List(GenericError(lineNumber, Message("xml.optional.field.empty", List("Street"))))
        }

        "must return correct error for empty TransmittingCountry'" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-enumeration-valid: Value '' is not facet-valid with respect to enumeration '[AT, BE, BG, CY, CZ, DK, EE, FI, FR, DE, GR, HU, HR, IE, IT, LV, LT, LU, MT, NL, PL, PT, RO, SK, SI, ES, SE, GB]'. It must be a value from the enumeration."
          )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'cbc:TransmittingCountry' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.field", List("TransmittingCountry"))))
        }

        "must return correct error for missing birthplace'" in {

          val error1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max200_Type'."
            )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'cbc:BirthPlace' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.add.a.element", List("BirthPlace"))))
        }

        "must return correct error when length exceed for TIN'" in {

          val error1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over200' with length = '201' is not facet-valid with respect to maxLength '200' for type 'StringMin1Max200_Type'."
          )
          val error2 = SaxParseError(lineNumber, "cvc-complex-type.2.2: Element 'cbc:TIN' must have no element [children], and the value must be valid.")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("TIN", "200"))))
        }

        "must return correct error for missing org name'" in {

          val error1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max200_Type'."
            )
          val error2 =
            SaxParseError(lineNumber, "cvc-complex-type.2.2: Element 'cbc:OrganisationName' must have no element [children], and the value must be valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.add.an.element", List("OrganisationName"))))
        }

        "must return correct error when allowed length exceeded 400" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over400' with length = '401' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'cbc:BuildingIdentifier' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("BuildingIdentifier", "400"))))
        }

        "must return unique error when MessageRefId allowed length exceeds 200 but user is told must be 85 characters or less" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over400' with length = '201' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max200_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'cbc:MessageRefId' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("MessageRefId", "85"))))
        }

        "must return unique error when DocRefId allowed length exceeds 200 but user is told must be 100 characters or less" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over400' with length = '201' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max200_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'cbc:DocRefId' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("DocRefId", "100"))))
        }

        "must return correct error when allowed length exceeded 4000 and the number is formatted correctly" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over4000' with length = '4001' is not facet-valid with respect to maxLength '4000' for type 'StringMin1Max4000_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'cbc:Warning' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("Warning", "4,000"))))
        }

        "must return correct error when invalid enum given for CBC" in {

          val invalidEnumError1 = SaxParseError(
            lineNumber,
            "cvc-enumeration-valid: Value 'ABC' is not facet-valid with respect to enumeration '[CBC]'. It must be a value from the enumeration."
          )
          val invalidEnumError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value 'ABC' of element 'cbc:MessageType' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.add.cbc")))
        }

        "must return correct error when invalid enum given for element" in {

          val invalidEnumError1 = SaxParseError(
            lineNumber,
            "cvc-enumeration-valid: Value 'Invalid code' is not facet-valid with respect to enumeration '[AF, AX, AL, DZ]'. It must be a value from the enumeration."
          )
          val invalidEnumError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value 'Raneevev' of element 'cbc:ResCountryCode' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.ISO.code", List("ResCountryCode"))))
        }

        "must return correct error when decimal is included on whole number field" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '4000.02' is not a valid value for 'integer'.")
          val error2 = SaxParseError(lineNumber, "cvc-complex-type.2.2: Element 'cbc:Amount' must have no element [children], and the value must be valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.must.be.whole.number", List("Amount"))))
        }

        "must return correct error when an int is not a whole number" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '90.1' is not a valid value for 'integer'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '90.1' of element 'cbc:NbEmployees' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.must.be.whole.number", List("NbEmployees"))))
        }

        "must return correct error for invalid date format" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '14-01-2007' is not a valid value for 'date'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '14-01-2007' of element 'cbc:BirthDate' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.date.format", List("BirthDate"))))
        }

        "must return correct error for invalid dateTime format" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '14-01-2007' is not a valid value for 'dateTime'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '14-01-2007' of element 'cbc:BirthDate' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.dateTime.format", List("BirthDate"))))
        }

        "must return correct error for invalid date format (leap year)" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '2021-02-31' is not a valid value for 'date'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '2021-02-31' of element 'cbc:BirthDate' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.date.format.real", List("BirthDate"))))
        }

        "must return correct error for invalid date format (ImplementingDate)" in {
          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '2020-05-oo' is not a valid value for 'dateTime'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '2020-05-oo' of element 'cbc:ImplementingDate' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.dateTime.format", List("ImplementingDate"))))
        }

        "must return correct error for line (value and tags)" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-complex-type.2.4.a: Invalid content was found starting with element '{\"urn:ukdac6:v0.1\":cbc:BuildingIdentifier}'. One of '{\"urn:ukdac6:v0.1\":Street}' is expected."
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.defaultMessage")))
        }

        "must return correct error when 3 errors present for same line" in {

          val error1 = SaxParseError(lineNumber, "random error 1")
          val error2 = SaxParseError(lineNumber, "random error 2")
          val error3 = SaxParseError(lineNumber, "random error 3")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2, error3))
          result mustBe List(GenericError(20, Message("xml.defaultMessage")))
        }

        "must return correct error for unrecognised errors" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-pattern-valid: Value 'rbrbrbrbrb' is not facet-valid with respect to pattern '[A-Z]{2}[D]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})' for type '#AnonType_DisclosureIDDAC6Disclosure_Type'."
          )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value 'rbrbrbrbrb' of element 'cbc:DisclosureID' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.defaultMessage")))
        }

        "must return correct error for missing MessageRefId" in {

          val error1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '2' for type 'StringMin2Max200_Type'."
            )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'cbc:MessageRefId' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.field", List("MessageRefId"))))
        }

        "must return 'Add a line for' message for relevant missing tag values" in {
          val elements = List(
            "Jurisdictions",
            "TransmittingCountry",
            "MessageRefId",
            "MessageTypeIndic",
            "Timestamp",
            "Capacity",
            "Nexus",
            "DocTypeIndic",
            "DocRefId",
            "Role",
            "Arrangement",
            "DisclosureDate",
            "Type",
            "Narrative",
            "Summary",
            "ResCountryCode",
            "TIN",
            "Name",
            "FirstName",
            "LastName",
            "CountryCode",
            "City"
          )

          elements.map { element =>
            val error1 = SaxParseError(
              lineNumber,
              s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'cbc:ABC'. One of '{\"urn:oecd:ties:cbc:v2\":cbc:$element}' is expected."""
            )
            val result = helper.generateErrorMessages(ListBuffer(error1))
            result mustBe List(GenericError(lineNumber, Message("xml.add.line", Seq(element))))
          }
        }

        "must return 'Add a' message for relevant missing tag values" in {
          val elements = List(
            "DocSpec",
            "MessageSpec"
          )

          elements.map { element =>
            val error1 = SaxParseError(
              lineNumber,
              s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'cbc:ABC'. One of '{\"urn:oecd:ties:cbc:v2\":cbc:$element}' is expected."""
            )
            val result = helper.generateErrorMessages(ListBuffer(error1))
            result mustBe List(GenericError(lineNumber, Message("xml.add.a.element", Seq(element))))
          }
        }

        "must return 'Add an' message for relevant missing tag values" in {
          val elements = List(
            "Address"
          )

          elements.map { element =>
            val error1 = SaxParseError(
              lineNumber,
              s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'cbc:ABC'. One of '{\"urn:oecd:ties:cbc:v2\":cbc:$element}' is expected."""
            )
            val result = helper.generateErrorMessages(ListBuffer(error1))
            result mustBe List(GenericError(lineNumber, Message("xml.add.an.element", Seq(element))))
          }
        }

        "must return 'Add' message for relevant missing tag values" in {

          val error1 = SaxParseError(
            lineNumber,
            s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'cbc:ABC'. One of '{\"urn:oecd:ties:cbc:v2\":cbc:Entity}' is expected."""
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.add.element", Seq("Entity"))))
        }

        "must return 'X is missing one or more fields, including Y' message for relevant missing tag values" in {

          val error1 = SaxParseError(
            lineNumber,
            s"""cvc-complex-type.2.4.b: The content of element 'cbc:ParentElement' is not complete. One of '{\"urn:oecd:ties:cbc:v2\":cbc:ChildElement}' is expected."""
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.tag", Seq("ParentElement", "ChildElement"))))
        }

        "must return correct error for AddressFix tag being after AddressFree" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-complex-type.2.4.d: Invalid content was found starting with element 'cbc:AddressFix'. No child element is expected at this point."
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.addressFix.error")))
        }
      }

      "when element does not contain 'cbc:' prefix" - {

        "must return correct error for missing element declaration'" in {
          val missingDeclaration = SaxParseError(lineNumber, "cvc-elt.1.a: Cannot find the declaration of element 'CBC_OECD'.")
          val result             = helper.generateErrorMessages(ListBuffer(missingDeclaration))
          result mustBe List(GenericError(lineNumber, Message("xml.must.have.element.declaration", List("CBC_OECD", "urn:oecd:ties:cbc:v2"))))
        }

        "must return correct error for missing attribute error'" in {
          val missingAttributeError = SaxParseError(lineNumber, "cvc-complex-type.4: Attribute 'currCode' must appear on element 'Amount'.")
          val result                = helper.generateErrorMessages(ListBuffer(missingAttributeError))
          result mustBe List(GenericError(lineNumber, Message("xml.add.attribute", List("Amount currCode"))))
        }

        "must return correct error for unexpected error message'" in {
          val randomError = SaxParseError(lineNumber, "random error.")
          val result      = helper.generateErrorMessages(ListBuffer(randomError))
          result mustBe List(GenericError(lineNumber, Message("xml.defaultMessage")))
        }

        "must return correct error for too long field attribute error for xnlNameType" in {
          val tooLongValue = "1" * 200
          val invalidEnumError1 =
            SaxParseError(
              lineNumber,
              s"cvc-maxLength-valid: Value '$tooLongValue' with length = '210' is not facet-valid with respect to maxLength '200' for type 'xnlNameType'."
            )
          val invalidEnumError2 =
            SaxParseError(
              lineNumber,
              s"cvc-attribute.3: The value '$tooLongValue' of attribute 'xnlNameType' on element 'FirstName' is not valid with respect to its type, 'StringMin1Max200_Type'."
            )
          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("FirstName xnlNameType", "200"))))
        }

        "must return correct error for too long field attribute error for INType" in {
          val tooLongValue = "1" * 200
          val invalidEnumError1 =
            SaxParseError(
              lineNumber,
              s"cvc-maxLength-valid: Value '$tooLongValue' with length = '210' is not facet-valid with respect to maxLength '200' for type 'INType'."
            )
          val invalidEnumError2 =
            SaxParseError(
              lineNumber,
              s"cvc-attribute.3: The value '$tooLongValue' of attribute 'INType' on element 'IN' is not valid with respect to its type, 'StringMin1Max200_Type'."
            )
          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("INType", "200"))))
        }

        "must return correct error for invalid enum error for attribute" in {
          val invalidEnumError1 =
            SaxParseError(
              lineNumber,
              "cvc-enumeration-valid: Value 'GBf' is not facet-valid with respect to enumeration '[AF, AX]'. It must be a value from the enumeration."
            )
          val invalidEnumError2 =
            SaxParseError(lineNumber,
                          "cvc-attribute.3: The value 'GBf' of attribute 'issuedBy' on element 'TIN' is not valid with respect to its type, 'CountryCode_Type'."
            )
          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.ISO.code", List("TIN issuedBy"))))
        }

        "must return correct error for empty element error'" in {

          val missingElementError1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max200_Type'."
            )

          val missingElementError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'TIN' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(missingElementError1, missingElementError2))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.field", List("TIN"))))
        }

        "must return correct error for missing optional element error when tags exist but are empty'" in {

          val missingElementError1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max400_Type'."
            )

          val missingElementError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'Street' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(missingElementError1, missingElementError2))
          result mustBe List(GenericError(lineNumber, Message("xml.optional.field.empty", List("Street"))))
        }

        "must return correct error for empty ResCountryCode'" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-enumeration-valid: Value '' is not facet-valid with respect to enumeration '[AT, BE, BG, CY, CZ, DK, EE, FI, FR, DE, GR, HU, HR, IE, IT, LV, LT, LU, MT, NL, PL, PT, RO, SK, SI, ES, SE, GB]'. It must be a value from the enumeration."
          )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'ResCountryCode' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.field", List("ResCountryCode"))))
        }

        "must return correct error for empty DocRefId'" in {

          val error1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max200_Type'."
            )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'DocRefId' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.field", List("DocRefId"))))
        }

        "must return correct error when length exceed for TIN'" in {

          val error1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over200' with length = '201' is not facet-valid with respect to maxLength '200' for type 'StringMin1Max200_Type'."
          )
          val error2 = SaxParseError(lineNumber, "cvc-complex-type.2.2: Element 'TIN' must have no element [children], and the value must be valid.")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("TIN", "200"))))
        }

        "must return correct error for missing org name'" in {

          val error1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '1' for type 'StringMin1Max200_Type'."
            )
          val error2 =
            SaxParseError(lineNumber, "cvc-complex-type.2.2: Element 'OrganisationName' must have no element [children], and the value must be valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.add.an.element", List("OrganisationName"))))
        }

        "must return correct error when allowed length exceeded 400" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over400' with length = '401' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max400_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'BuildingIdentifier' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("BuildingIdentifier", "400"))))
        }

        "must return unique error when MessageRefId allowed length exceeds 200 but user is told must be 85 characters or less" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over400' with length = '201' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max200_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'MessageRefId' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("MessageRefId", "85"))))
        }

        "must return unique error when DocRefId allowed length exceeds 200 but user is told must be 100 characters or less" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over400' with length = '201' is not facet-valid with respect to maxLength '400' for type 'StringMin1Max200_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'DocRefId' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("DocRefId", "100"))))
        }

        "must return correct error when allowed length exceeded 4000 and the number is formatted corectly" in {

          val maxLengthError1 = SaxParseError(
            lineNumber,
            s"cvc-maxLength-valid: Value '$over4000' with length = '4001' is not facet-valid with respect to maxLength '4000' for type 'StringMin1Max4000_Type'."
          )
          val maxlengthError2 = SaxParseError(lineNumber, s"cvc-type.3.1.3: The value '$over400' of element 'Warning' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(maxLengthError1, maxlengthError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.allowed.length", List("Warning", "4,000"))))
        }

        "must return correct error when invalid enum given for CBC" in {

          val invalidEnumError1 = SaxParseError(
            lineNumber,
            "cvc-enumeration-valid: Value 'ABC' is not facet-valid with respect to enumeration '[CBC]'. It must be a value from the enumeration."
          )
          val invalidEnumError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value 'ABC' of element 'MessageType' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.add.cbc")))
        }

        "must return correct error when invalid enum given for element" in {

          val invalidEnumError1 = SaxParseError(
            lineNumber,
            "cvc-enumeration-valid: Value 'Invalid code' is not facet-valid with respect to enumeration '[AF, AX, AL, DZ]'. It must be a value from the enumeration."
          )
          val invalidEnumError2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value 'Raneevev' of element 'IncorpCountryCode' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(invalidEnumError1, invalidEnumError2))
          result mustBe List(GenericError(lineNumber, Message("xml.not.ISO.code", List("IncorpCountryCode"))))
        }

        "must return correct error when decimal is included on whole number field" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '4000.02' is not a valid value for 'integer'.")
          val error2 = SaxParseError(lineNumber, "cvc-complex-type.2.2: Element 'Amount' must have no element [children], and the value must be valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.must.be.whole.number", List("Amount"))))
        }

        "must return correct error when an int is not a whole number" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '90.1' is not a valid value for 'integer'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '90.1' of element 'NbEmployees' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.must.be.whole.number", List("NbEmployees"))))
        }

        "must return correct error for invalid date format" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '14-01-2007' is not a valid value for 'date'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '14-01-2007' of element 'BirthDate' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.date.format", List("BirthDate"))))
        }

        "must return correct error for invalid dateTime format" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '14-01-2007' is not a valid value for 'dateTime'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '14-01-2007' of element 'BirthDate' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.dateTime.format", List("BirthDate"))))
        }

        "must return correct error for invalid date format (leap year)" in {

          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '2021-02-31' is not a valid value for 'date'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '2021-02-31' of element 'BirthDate' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.date.format.real", List("BirthDate"))))
        }

        "must return correct error for invalid date format (ImplementingDate)" in {
          val error1 = SaxParseError(lineNumber, "cvc-datatype-valid.1.2.1: '2020-05-oo' is not a valid value for 'dateTime'.")
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '2020-05-oo' of element 'ImplementingDate' is not valid.")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.dateTime.format", List("ImplementingDate"))))
        }

        "must return correct error for line (value and tags)" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-complex-type.2.4.a: Invalid content was found starting with element '{\"urn:ukdac6:v0.1\":BuildingIdentifier}'. One of '{\"urn:ukdac6:v0.1\":Street}' is expected."
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.defaultMessage")))
        }

        "must return correct error when 3 errors present for same line" in {

          val error1 = SaxParseError(lineNumber, "random error 1")
          val error2 = SaxParseError(lineNumber, "random error 2")
          val error3 = SaxParseError(lineNumber, "random error 3")

          val result = helper.generateErrorMessages(ListBuffer(error1, error2, error3))
          result mustBe List(GenericError(20, Message("xml.defaultMessage")))
        }

        "must return correct error for incorrectly formatted disclosure id" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-pattern-valid: Value 'rbrbrbrbrb' is not facet-valid with respect to pattern '[A-Z]{2}[D]([2]\\d{3}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01]))([A-Z0-9]{6})' for type '#AnonType_DisclosureIDDAC6Disclosure_Type'."
          )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value 'rbrbrbrbrb' of element 'DisclosureID' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.defaultMessage")))
        }

        "must return correct error for missing MessageRefId" in {

          val error1 =
            SaxParseError(lineNumber,
                          "cvc-minLength-valid: Value '' with length = '0' is not facet-valid with respect to minLength '2' for type 'StringMin2Max200_Type'."
            )
          val error2 = SaxParseError(lineNumber, "cvc-type.3.1.3: The value '' of element 'MessageRefId' is not valid.")
          val result = helper.generateErrorMessages(ListBuffer(error1, error2))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.field", List("MessageRefId"))))
        }

        "must return 'Add a line for' message for relevant missing tag values" in {
          val elements = List(
            "Jurisdictions",
            "TransmittingCountry",
            "MessageRefId",
            "MessageTypeIndic",
            "Timestamp",
            "Capacity",
            "Nexus",
            "DocTypeIndic",
            "DocRefId",
            "Role",
            "Arrangement",
            "DisclosureDate",
            "Type",
            "Narrative",
            "Summary",
            "ResCountryCode",
            "TIN",
            "Name",
            "FirstName",
            "LastName",
            "CountryCode",
            "City"
          )

          elements.map { element =>
            val error1 = SaxParseError(
              lineNumber,
              s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'ABC'. One of '{\"urn:oecd:ties:cbc:v2\":$element}' is expected."""
            )
            val result = helper.generateErrorMessages(ListBuffer(error1))
            result mustBe List(GenericError(lineNumber, Message("xml.add.line", Seq(element))))
          }
        }

        "must return 'Add a' message for relevant missing tag values" in {
          val elements = List(
            "DocSpec",
            "MessageSpec"
          )

          elements.map { element =>
            val error1 = SaxParseError(
              lineNumber,
              s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'ABC'. One of '{\"urn:oecd:ties:cbc:v2\":$element}' is expected."""
            )
            val result = helper.generateErrorMessages(ListBuffer(error1))
            result mustBe List(GenericError(lineNumber, Message("xml.add.a.element", Seq(element))))
          }
        }

        "must return 'Add an' message for relevant missing tag values" in {
          val elements = List(
            "Address"
          )

          elements.map { element =>
            val error1 = SaxParseError(
              lineNumber,
              s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'ABC'. One of '{\"urn:oecd:ties:cbc:v2\":$element}' is expected."""
            )
            val result = helper.generateErrorMessages(ListBuffer(error1))
            result mustBe List(GenericError(lineNumber, Message("xml.add.an.element", Seq(element))))
          }
        }

        "must return 'Add' message for relevant missing tag values" in {

          val error1 = SaxParseError(
            lineNumber,
            s"""cvc-complex-type.2.4.a: Invalid content was found starting with element 'ABC'. One of '{\"urn:oecd:ties:cbc:v2\":Entity}' is expected."""
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.add.element", Seq("Entity"))))
        }

        "must return 'X is missing one or more fields, including Y' message for relevant missing tag values" in {

          val error1 = SaxParseError(
            lineNumber,
            s"""cvc-complex-type.2.4.b: The content of element 'ParentElement' is not complete. One of '{\"urn:oecd:ties:cbc:v2\":ChildElement}' is expected."""
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.empty.tag", Seq("ParentElement", "ChildElement"))))
        }

        "must return correct error for AddressFix tag being after AddressFree" in {

          val error1 = SaxParseError(
            lineNumber,
            "cvc-complex-type.2.4.d: Invalid content was found starting with element 'AddressFix'. No child element is expected at this point."
          )
          val result = helper.generateErrorMessages(ListBuffer(error1))
          result mustBe List(GenericError(lineNumber, Message("xml.addressFix.error")))
        }
      }
    }

    "invalidCodeMessage" - {

      "must return correct message for 'IncorpCountryCode'" in {
        val result = helper.invalidCodeMessage("IncorpCountryCode")
        result mustBe Some(Message("xml.not.ISO.code", List("IncorpCountryCode")))
      }

      "must return correct message for 'CountryCode'" in {
        val result = helper.invalidCodeMessage("CountryCode")
        result mustBe Some(Message("xml.not.ISO.code", List("CountryCode")))
      }

      "must return correct message for 'ResCountryCode'" in {
        val result = helper.invalidCodeMessage("ResCountryCode")
        result mustBe Some(Message("xml.not.ISO.code", List("ResCountryCode")))
      }

      "must return correct message for 'TransmittingCountry'" in {
        val result = helper.invalidCodeMessage("TransmittingCountry")
        result mustBe Some(Message("xml.not.ISO.code", List("TransmittingCountry")))
      }

      "must return correct message for 'BizActivities'" in {
        val result = helper.invalidCodeMessage("BizActivities")
        result mustBe Some(Message("xml.not.allowed.value", List("BizActivities")))
      }

      "must return correct message for 'ReportingRole'" in {
        val result = helper.invalidCodeMessage("ReportingRole")
        result mustBe Some(Message("xml.not.allowed.value", List("ReportingRole")))
      }

      "must return correct message for 'MessageTypeIndic'" in {
        val result = helper.invalidCodeMessage("MessageTypeIndic")
        result mustBe Some(Message("xml.not.allowed.value", List("MessageTypeIndic")))
      }

      "must return correct message for 'Role'" in {
        val result = helper.invalidCodeMessage("Role")
        result mustBe Some(Message("xml.not.allowed.value", List("Role")))
      }

      "must return correct message for 'DocTypeIndic'" in {
        val result = helper.invalidCodeMessage("DocTypeIndic")
        result mustBe Some(Message("xml.not.allowed.value", List("DocTypeIndic")))
      }

      "must return correct message for 'SummaryRef'" in {
        val result = helper.invalidCodeMessage("SummaryRef")
        result mustBe Some(Message("xml.not.allowed.value", List("SummaryRef")))
      }

      "must return correct message for 'Language'" in {
        val result = helper.invalidCodeMessage("Language")
        result mustBe Some(Message("xml.not.ISO.language.code", List("Language")))
      }

      "must return correct message for 'ReceivingCountry'" in {
        val result = helper.invalidCodeMessage("ReceivingCountry")
        result mustBe Some(Message("xml.not.ISO.code.receivingCountry"))
      }

      "must return None for unexpected elementName" in {
        val result = helper.invalidCodeMessage("Unexpected-name")
        result mustBe None
      }

    }
  }
}
