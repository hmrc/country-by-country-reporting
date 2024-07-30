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

package models.sdes

import play.api.libs.json.{JsError, JsString, JsSuccess, Reads, Writes}

sealed trait Algorithm
case object MD5 extends Algorithm
case object SHA1 extends Algorithm
case object SHA2 extends Algorithm
case object SHA256 extends Algorithm
case object SHA512 extends Algorithm

object Algorithm {

  val values: Set[Algorithm] = Set(MD5, SHA1, SHA2, SHA256, SHA512)

  def apply(algorithm: String): Algorithm = algorithm.toLowerCase match {
    case "md5"     => MD5
    case "sha1"    => SHA1
    case "sha2"    => SHA2
    case "sha-256" => SHA256
    case "sha-512" => SHA512
    case _         => throw new IllegalArgumentException(s"Unsupported algorithm $algorithm")
  }

  implicit val writes: Writes[Algorithm] = Writes[Algorithm] {
    case MD5    => JsString("md5")
    case SHA1   => JsString("SHA1")
    case SHA2   => JsString("SHA2")
    case SHA256 => JsString("SHA-256")
    case SHA512 => JsString("SHA-512")
  }

  implicit val reads: Reads[Algorithm] = Reads[Algorithm] {
    case JsString("md5")     => JsSuccess(MD5)
    case JsString("SHA1")    => JsSuccess(SHA1)
    case JsString("SHA2")    => JsSuccess(SHA2)
    case JsString("SHA-256") => JsSuccess(SHA256)
    case JsString("SHA-512") => JsSuccess(SHA512)
    case value               => JsError(s"Unexpected Algorithm: $value")
  }
}
