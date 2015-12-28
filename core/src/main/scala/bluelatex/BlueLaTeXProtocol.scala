/*
 * This file is part of the \BlueLaTeX project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package bluelatex

import spray.json._

import synchro._

import better.files._

trait BlueLaTeXProtocol extends SyncJsonProtocol {

  implicit object fileWriter extends JsonWriter[File] {

    def write(file: File): JsObject =
      file match {
        case f @ RegularFile(_) =>
          JsObject(
            Map(
              "type" -> JsString("file"),
              "name" -> JsString(f.name)))
        case d @ Directory(children) =>
          JsObject(
            Map(
              "type" -> JsString("directory"),
              "name" -> JsString(d.name),
              "children" -> JsArray(children.map(write(_)).toVector)))
        case _ =>
          serializationError(f"$file cannot be serialized to a json object")
      }

  }

}

object BlueLaTeXProtocol extends BlueLaTeXProtocol
