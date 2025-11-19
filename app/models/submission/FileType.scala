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

package models.submission

import play.api.libs.json.*

sealed trait FileType

case object NormalFile extends FileType
case object LargeFile extends FileType

object FileType {

  val values: Seq[FileType] = Seq(NormalFile, LargeFile)

  def fromString(fileType: String): FileType = fileType.toUpperCase match {
    case "NORMAL" => NormalFile
    case "LARGE"  => LargeFile
    case _        => throw new NoSuchElementException
  }

  given Format[FileType] = Format(
    Reads {
      case obj: JsObject if obj.keys == Set("NormalFile") => JsSuccess(NormalFile)
      case obj: JsObject if obj.keys == Set("LargeFile")  => JsSuccess(LargeFile)
      case _                                              => JsError("Expected JsObject")
    },
    Writes {
      case NormalFile => Json.obj("NormalFile" -> Json.obj())
      case LargeFile  => Json.obj("LargeFile" -> Json.obj())
    }
  )
}
