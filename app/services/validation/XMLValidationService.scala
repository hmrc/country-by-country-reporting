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

package services.validation

import models.validation.SaxParseError
import org.xml.sax.{ErrorHandler, SAXParseException}
import org.xml.sax.helpers.DefaultHandler

import java.io.{InputStream, StringReader}
import java.net.URL
import javax.inject.Inject
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory}
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Using
import scala.xml.factory.XMLLoader
import scala.xml.{Elem, NodeSeq}

class XMLValidationService @Inject() () {

  private val schemaLang: String = javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI

  private def xmlValidatingParser(schema: Schema): SAXParser = {

    val factory: SAXParserFactory = SAXParserFactory.newInstance()
    factory.setSchema(schema)
    factory.setNamespaceAware(true)
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    factory.setXIncludeAware(false)
    factory.newSAXParser()
  }

  private def xmlLoader(filePath: String, errorList: ListBuffer[SaxParseError]): XMLLoader[Elem] = {

    val url: URL       = getClass.getResource(filePath)
    val schema: Schema = javax.xml.validation.SchemaFactory.newInstance(schemaLang).newSchema(url)
    trait AccumulatorState extends DefaultHandler {
      override def warning(e: SAXParseException): Unit    = errorList += SaxParseError(e.getLineNumber, e.getMessage)
      override def error(e: SAXParseException): Unit      = errorList += SaxParseError(e.getLineNumber, e.getMessage)
      override def fatalError(e: SAXParseException): Unit = errorList += SaxParseError(e.getLineNumber, e.getMessage)
    }

    new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def parser: SAXParser = xmlValidatingParser(schema)
      override def adapter           = new scala.xml.parsing.NoBindingFactoryAdapter with AccumulatorState
    }
  }

  def validate(upScanUrl: String, filePath: String): Either[ListBuffer[SaxParseError], Elem] = {
    val list: ListBuffer[SaxParseError] = new ListBuffer[SaxParseError]
    val loadedXML                       = xmlLoader(filePath, list).load(new URL(upScanUrl))
    if (list.isEmpty) Right(loadedXML) else Left(list)
  }

  def validate(xml: NodeSeq, filePath: String): Either[ListBuffer[SaxParseError], Elem] = {
    val list: ListBuffer[SaxParseError] = new ListBuffer[SaxParseError]
    val loadedXML                       = xmlLoader(filePath, list).load(new StringReader(xml.mkString))
    if (list.isEmpty) Right(loadedXML) else Left(list)
  }

  def validateUrlStreamAsync(upScanUrl: String, xsdResourcePath: String)(implicit ec: ExecutionContext): Future[Either[List[SaxParseError], Unit]] =
    Future {
      Using.resource(new URL(upScanUrl).openStream()) { is =>
        validateStream(is, xsdResourcePath)
      }
    }

  private def loadSchemaFromResource(xsdResourcePath: String): Schema = {
    val xsdUrl: URL =
      Option(getClass.getResource(xsdResourcePath))
        .getOrElse(throw new IllegalArgumentException(s"XSD resource not found: $xsdResourcePath"))

    SchemaFactory.newInstance(schemaLang).newSchema(xsdUrl)
  }

  private def toErrorHandler(errorList: ListBuffer[SaxParseError]): ErrorHandler =
    new ErrorHandler {
      override def warning(e: SAXParseException): Unit =
        errorList += SaxParseError(e.getLineNumber, e.getMessage)

      override def error(e: SAXParseException): Unit =
        errorList += SaxParseError(e.getLineNumber, e.getMessage)

      override def fatalError(e: SAXParseException): Unit =
        errorList += SaxParseError(e.getLineNumber, e.getMessage)
    }

  private def validateStream(xmlInput: InputStream, xsdResourcePath: String): Either[List[SaxParseError], Unit] = {
    val errors = ListBuffer.empty[SaxParseError]

    val schema    = loadSchemaFromResource(xsdResourcePath)
    val validator = schema.newValidator()
    validator.setErrorHandler(toErrorHandler(errors))

    try
      validator.validate(new StreamSource(xmlInput))
    catch {
      case e: SAXParseException =>
        errors += SaxParseError(e.getLineNumber, e.getMessage)
      case e: Exception =>
        errors += SaxParseError(-1, e.getMessage)
    }

    if (errors.isEmpty) Right(()) else Left(errors.toList)
  }

}
