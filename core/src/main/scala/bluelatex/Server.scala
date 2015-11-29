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

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import com.typesafe.config.Config

import scala.concurrent.Future

import config._

class Server(conf: Config) extends StdReaders {

  implicit val system = ActorSystem("bluelatex")
  implicit val materializer = ActorMaterializer()
  implicit val ec = system.dispatcher

  val prefix =
    conf.as[String]("bluelatex.api.prefix")

  val services =
    conf.as[List[String]]("bluelatex.api.services")

  val route =
    services match {
      case s :: sl =>
        def serv(s: String): Service = Class.forName(s).newInstance.asInstanceOf[Service]
        sl.foldLeft(serv(s).route)((acc, s) => acc ~ serv(s).route)
      case Nil =>
        reject
    }

  val prefixed =
    if (prefix.isEmpty)
      route
    else
      pathPrefix(separateOnSlashes(prefix)) {
        route
      }

  private var bindingFuture: Future[Http.ServerBinding] = null

  def start(): Unit =
    if (bindingFuture == null)
      bindingFuture = Http().bindAndHandle(prefixed, conf.as[String]("bluelatex.http.host"), conf.as[Int]("bluelatex.http.port"))

  def stop(): Unit =
    if (bindingFuture != null) {
      bindingFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.shutdown()) // and shutdown when done
      bindingFuture = null
    }

}
