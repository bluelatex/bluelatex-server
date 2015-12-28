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

package object synchro {
  type PeerId = String
  type PaperId = String
  type Filepath = List[String]

  def leavingMessage(paperId: String) =
    JsObject(
      Map(
        "event" -> JsString("part"),
        "paperid" -> JsString(paperId)))

  def joiningMessage(paperId: String) =
    JsObject(
      Map(
        "event" -> JsString("join"),
        "paperid" -> JsString(paperId)))

  def idlingMessage(paperId: String) =
    JsObject(
      Map(
        "event" -> JsString("idle"),
        "paperid" -> JsString(paperId)))

}
