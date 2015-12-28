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
package synchro

import spray.json._

trait SyncJsonProtocol extends DefaultJsonProtocol {

  implicit val messageFormat = jsonFormat3(Message)

  implicit object editFormat extends JsonFormat[Edit] {

    def read(json: JsValue): Edit =
      json match {
        case JsString(Edit(e)) =>
          e
        case _ =>
          deserializationError("edit expected")
      }

    def write(edit: Edit): JsValue =
      JsString(edit.toString)

  }

  implicit val deltaFormat = jsonFormat3(Delta)

  implicit val rawFormat = jsonFormat3(Raw)

  implicit object syncActionFormat extends JsonFormat[SyncAction] {

    def read(json: JsValue): SyncAction =
      json match {
        case JsObject(fields) =>
          fields.get("name") match {
            case Some(JsString("delta")) =>
              fields.get("argument") match {
                case Some(d) => d.convertTo[Delta]
                case _       => deserializationError("delta action expected")
              }
            case Some(JsString("raw")) =>
              fields.get("argument") match {
                case Some(r) => r.convertTo[Raw]
                case _       => deserializationError("raw action expected")
              }
            case Some(JsString("nullify")) =>
              Nullify
            case _ =>
              deserializationError(f"unknown action")
          }
        case _ =>
          deserializationError("synchronization action expected")
      }

    def write(action: SyncAction): JsValue =
      action match {
        case d: Delta =>
          JsObject(
            Map(
              "name" -> JsString("delta"),
              "argument" -> d.toJson))
        case r: Raw =>
          JsObject(
            Map(
              "name" -> JsString("raw"),
              "argument" -> r.toJson))
        case Nullify =>
          JsObject(
            Map(
              "name" -> JsString("nullify")))
      }
  }

  implicit val commandFormat = jsonFormat2(Command)

  implicit val commandsFormat: JsonFormat[Commands] = lazyFormat(jsonFormat2(Commands))

  implicit val synchronizationMessageFormat = jsonFormat3(SynchronizationMessage)

}

object SyncJsonProtocol extends SyncJsonProtocol
