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

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport

import akka.actor.ActorSystem

/** A \BlueLaTeX service must extend this trait and specify the
 *  API.
 *
 */
abstract class Service(val system: ActorSystem) extends SprayJsonSupport with BlueLaTeXProtocol with Directives {

  def route: Route

}
