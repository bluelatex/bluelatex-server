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

import scala.collection.mutable.{
  Map,
  Set
}

import akka.actor.{
  Actor,
  ActorRef,
  ActorSystem,
  PoisonPill,
  Props,
  ReceiveTimeout,
  Stash,
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

import java.io.IOException
import java.nio.charset.CodingErrorAction

import better.files._
import Cmds._

import com.typesafe.config.ConfigFactory

import config._

object FsStore {

  val codec = Codec("UTF-8")
  codec.onMalformedInput(CodingErrorAction.REPLACE)
  codec.onUnmappableCharacter(CodingErrorAction.REPLACE)

}

class FsStore(file: File) extends Actor with Stash {

  import FsStore._

  val children = Map.empty[String, ActorRef]

  val conf = ConfigFactory.load()

  // set an initial delay, if no message is received within this period, the actor is killed
  context.setReceiveTimeout(conf.as[Duration]("bluelatex.persistence.fs.timeout"))

  def getOrCreate(name: String): ActorRef =
    children.get(name) match {
      case Some(a) =>
        a
      case None =>
        val a = context.actorOf(Props(classOf[FsStore], file / name), name)
        children(name) = a
        context.watch(a)
        a
    }

  def receive = initial

  val initial: Receive = {

    case Save(data) =>

      val ref = getOrCreate(data.name)
      val f = file / data.name

      // we are asked to save some data. but what kind of data is it?
      data match {
        case cont @ Container(name) =>
          try {
            if (f.exists)
              f.delete()
            mkdirs(f)

            for (e <- cont.elements) {
              ref ! Save(e)
            }
            context.become(saving(sender, Set.empty[String]))
          } catch {
            case e: Exception =>
              sender ! Status.Failure(e)
          }

        case leaf @ Leaf(name) =>
          try {
            // if the leaf already exists and is a directory, remove it
            if (f.isDirectory)
              f.delete()

            // write content into the file (erase previous content if any)
            f.overwrite(leaf.content)

            sender ! Unit
          } catch {
            case e: Exception =>
              sender ! Status.Failure(e)
          }

      }

    case Delete(Nil) =>
      try {
        // delete this file
        file.delete()

        sender ! Unit
        self ! PoisonPill
      } catch {
        case e: Exception =>
          sender ! Status.Failure(e)
      }

    case Delete(h :: t) =>
      // delete the given child
      file match {
        case Directory(_) =>
          getOrCreate(h).forward(Delete(t))
        case RegularFile(_) =>
          sender ! Status.Failure(new IOException(f"$file is not a directory"))
      }

    case Load(Nil) =>
      // load the data represented by this actor
      file match {
        case Directory(files) =>
          for (f <- files)
            getOrCreate(f.name) ! Load(Nil)
          context.become(loading(sender, Set.empty[Data]))
        case f @ RegularFile(_) =>
          sender ! Leaf(f.name)(f.contentAsString)
        case _ =>
          sender ! Status.Failure(new IOException(f"$file is not a regular file or directory"))
      }

    case Load(h :: t) =>
      file match {
        case Directory(_) =>
          getOrCreate(h).forward(Load(t))
        case _ =>
          sender ! Status.Failure(new IOException(f"$file is not a directory"))
      }

    case Terminated(child) =>
      // the children actor is no more active, remove it from the watched actors
      children -= child.path.name

    case ReceiveTimeout =>
      context.stop(self)

  }

  def loading(original: ActorRef, loaded: Set[Data]): Receive = {

    case d: Data =>
      loaded += d
      if (loaded.size == children.size) {
        original ! Container(file.name)(loaded)
        unstashAll()
        context.become(initial)
      }

    case Load(_) | Save(_) | Delete(_) =>
      // stash to treat it later
      stash()

    case Terminated(child) =>
      // the children actor is no more active, remove it from the watched actors
      children -= child.path.name
      loaded.filterNot(_.name == child.path.name)

  }

  def saving(original: ActorRef, saved: Set[String]): Receive = {

    case Unit =>
      saved += sender.path.name
      if (saved.size == children.size) {
        original ! Unit
        unstashAll()
        context.become(initial)
      }

    case Load(_) | Save(_) | Delete(_) =>
      // stash to treat it later
      stash()

    case Terminated(child) =>
      // the children actor is no more active, remove it from the watched actors
      children -= child.path.name
      saved -= child.path.name

  }

}
