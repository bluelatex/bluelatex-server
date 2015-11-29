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
package service

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ContentTypes._

import spray.json._

class CoreService extends Service {

  def route =
    path("info") {
      val info =
        JsObject(
          Map(
            "version" -> JsString(BlueLaTeXInfo.version),
            "scalaVersion" -> JsString(BlueLaTeXInfo.scalaVersion),
            "buildTime" -> JsNumber(BlueLaTeXInfo.buildTime)))
      complete(info)
    }

}
