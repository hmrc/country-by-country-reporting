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

package models

import scala.xml.NodeSeq

package object xml {

  trait XmlReads[T]:
    def read(xml: NodeSeq): T

  given seqReads[T](using r: XmlReads[T]): XmlReads[Seq[T]] with
    def read(xml: NodeSeq): Seq[T] =
      xml.map(r.read)

  given optionReads[T](using r: XmlReads[T]): XmlReads[Option[T]] with
    def read(xml: NodeSeq): Option[T] =
      xml.headOption.map(r.read)

  given optionSeqReads[T](using r: XmlReads[T]): XmlReads[Option[Seq[T]]] with
    def read(xml: NodeSeq): Option[Seq[T]] =
      if (xml.isEmpty) None else Some(xml.map(r.read))

  def fromXml[T](xml: NodeSeq)(using r: XmlReads[T]): T = r.read(xml)

  extension (xml: NodeSeq)
    def \#(label: String): NodeSeq =
      xml.headOption
        .map(_.child.filter(_.label == label))
        .getOrElse(NodeSeq.Empty)

}
