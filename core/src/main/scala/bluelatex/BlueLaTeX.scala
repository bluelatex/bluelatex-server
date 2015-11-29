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

import com.typesafe.config.ConfigFactory

import org.slf4j.LoggerFactory

import scala.io.StdIn

/** The entry point of the \BlueLaTeX application.
 *  It starts everything, loads the configuration and the logging settings.
 *
 */
object BlueLaTeX extends App {

  val logger = LoggerFactory.getLogger(getClass)

  // create the server
  val server = new Server(ConfigFactory.load)

  sys.addShutdownHook {
    logger.info("\\BlueLaTeX has been killed")
    server.stop()
  }

  server.start()

  logger.info("\\BlueLaTeX is up and running")

}
