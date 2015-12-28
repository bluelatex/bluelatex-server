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

import scopt.OptionParser

/** The entry point of the \BlueLaTeX application.
 *  It starts everything, loads the configuration and the logging settings.
 *
 */
object BlueLaTeX extends App {

  val optionParser = new OptionParser[CmdLine]("bluelatex") {
    head("bluelatex", BlueLaTeXInfo.version)
    opt[String]('c', "config") valueName ("<file>") action {
      case (f, o) =>
        o.copy(conf = Some(f))
    } text ("Specify the configuration file to use")
    opt[Unit]('d', "debug") action {
      case ((), o) =>
        o.copy(debug = true)
    } text ("Start \\BlueLaTeX in debug mode, allowing to stop it from the command line")
  }

  val options = optionParser.parse(args, CmdLine()) match {

    case Some(CmdLine(debug, conf)) =>

      for (c <- conf) {
        System.setProperty("config.file", c)
        ConfigFactory.invalidateCaches()
      }

      val logger = LoggerFactory.getLogger(getClass)

      // create the server
      val server = new Server

      sys.addShutdownHook {
        logger.info("\\BlueLaTeX has been killed")
        server.stop()
      }

      server.start()

      logger.info("\\BlueLaTeX is up and running")

      if (debug) {
        println("Press enter to stop server")
        StdIn.readLine
        server.stop()
      }

    case None =>
    // error message has been displayed, just don't do anything at all
  }

}
