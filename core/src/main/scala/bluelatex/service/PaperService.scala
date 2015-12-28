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

import persistence._

import synchro._

import scala.concurrent.duration._

import java.util.UUID

import akka.pattern.ask

import spray.json._

import better.files._
import Cmds._

trait PaperService {
  this: CoreService =>

  implicit val timeout = akka.util.Timeout(2.seconds)

  def paperRoute =
    pathPrefix("papers") {
      pathEndOrSingleSlash {
        post {
          val newId = UUID.randomUUID.toString
          onSuccess((store ? Save(Data(newId -> Data())))) { _ =>
            complete(JsString(newId))
          }
        }
      } ~
        pathPrefix(Segment) { paperId =>
          pathEndOrSingleSlash {
            delete {
              (persistenceDir / paperId).delete()
              complete(JsBoolean(true))
            }
          } ~
            pathPrefix("files") {
              pathEndOrSingleSlash {
                get {
                  complete((persistenceDir / paperId).toJson)
                }
              } ~
                getFromDirectory((persistenceDir / paperId).toString) ~
                post {
                  extractUnmatchedPath { path =>
                    val local =
                      if (path.startsWithSlash)
                        path.dropChars(1).toString
                      else
                        path.toString
                    mkdirs(persistenceDir / paperId / local)
                    uploadedFile("file") {
                      case (metadata, file) =>
                        val f = file.toScala
                        f.moveTo(local / metadata.fileName)
                        complete(JsBoolean(true))
                    } ~
                      complete(JsBoolean(true))
                  }
                } ~
                delete {
                  extractUnmatchedPath { path =>
                    val local =
                      if (path.startsWithSlash)
                        persistenceDir / paperId / path.dropChars(1).toString
                      else
                        persistenceDir / paperId / path.toString
                    if (local.exists) {
                      local.delete()
                      complete(JsBoolean(true))
                    } else {
                      complete(JsBoolean(false))
                    }
                  }
                }
            } ~
            path("synchronization") {
              post {
                entity(as[SynchronizationMessage]) { msg =>
                  complete("toto")
                }
              }
            }
          }
        }

}
