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
package persistence

import akka.actor.{
  Actor,
  ActorRef,
  ActorSystem,
  PoisonPill,
  Props,
  ReceiveTimeout,
  Status,
  Terminated
}
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.{
  Future,
  ExecutionContext
}
import scala.concurrent.duration._

import scala.io.Codec

import java.io.{
  IOException,
  FileNotFoundException
}
import java.nio.charset.CodingErrorAction

import better.files._
import Cmds._

import com.typesafe.config.ConfigFactory

import config._

import scala.annotation.tailrec

object FsStore {

  val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

}

class FsStore(file: File, extensions: Set[String]) extends Actor {

  import FsStore._

  val conf = ConfigFactory.load()

  // set an initial delay, if no message is received within this period, the actor is killed
  context.setReceiveTimeout(conf.as[Duration]("bluelatex.persistence.fs.timeout"))

  def receive = {

    case Save(data) =>

      try {
        save(file, data)
        sender ! Unit
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case Delete(p) =>
      try {
        delete(file, p)
        sender ! Unit
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case Load(p) =>
      try {
        sender ! load(file, p)
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case ReceiveTimeout =>
      context.stop(self)

  }

  private def save(file: File, data: Data): Unit =
    if (data.size > 0) {

      if (file.exists && !file.isDirectory)
        file.delete()
      mkdirs(file)

      for ((name, d) <- data.children)
        save(file / name, d)

    } else data.content match {
      case Some(c) =>
        if (!file.exists || data.lastChanged > file.lastModifiedTime.toEpochMilli)
          file.overwrite(c)(codec)
      case None =>
        mkdirs(file)
    }

  @tailrec
  private def delete(file: File, path: List[String]): Unit =
    path match {
      case Nil    => file.delete()
      case h :: t => delete(file / h, t)
    }

  private def load(file: File, path: List[String]): Option[Data] =
    path match {
      case Nil =>
        file match {
          case Directory(files) =>
            val children =
              for {
                f <- files
                d <- load(f, Nil)
              } yield (f.name, d)
            Some(Data(children = children.toMap))
          case f @ RegularFile(_) =>
            if (extensions.isEmpty || f.extension.isEmpty || extensions.contains(f.extension.get))
              Some(Data(f.contentAsString(codec)))
            else
              None
          case _ =>
            None
        }
      case h :: t =>
        load(file / h, t)
    }

}
