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

import config._

import akka.actor.{
  Actor,
  ActorRef,
  Props,
  Terminated,
  ReceiveTimeout
}
import akka.pattern.ask
import akka.event.Logging
import akka.util.Timeout

import scala.concurrent.duration._

import scala.collection.mutable.{
  Map,
  ListBuffer
}

import com.typesafe.config.ConfigFactory

import name.fraser.neil.plaintext.DiffMatchPatch

import scala.annotation.tailrec

/** Represents a peer connected to a paper project.
 *  It manages the views per document, the messages.
 *  It allows to disconnect a peer after some idle time.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
class Peer(paperId: String, peerId: String, messageBus: MessageEventBus, dmp: DiffMatchPatch) extends Actor {

  implicit private def ec = context.system.dispatcher

  private val messages = ListBuffer.empty[Message]

  private val conf = ConfigFactory.load()

  private val timeout = conf.as[Duration]("bluelatex.synchronization.idle-timeout")

  private val views = Map.empty[String, ActorRef]

  val logger = Logging(context.system, this)

  override def preStart(): Unit = {
    super.preStart()
    // subscribe to receive the messages sent by other peers.
    messageBus.subscribe(self, peerId)
    // I joined, others must know
    messageBus.publish(Message(peerId, joiningMessage(paperId), Nil))
    // set timeout to go into idle mode when not receiving any messages
    context.setReceiveTimeout(timeout)
  }

  override def postStop(): Unit = {
    super.postStop()
    // unsubscribe from message stream
    messageBus.unsubscribe(self)
    // I left, others must know
    messageBus.publish(Message(peerId, leavingMessage(paperId), Nil))
  }

  private def getDocumentView(path: Filepath): ActorRef = {
    val name = path.mkString("/")
    views.get(name) match {
      case Some(c) =>
        c
      case None =>
        val act = context.actorOf(Props(classOf[DocumentView], peerId, path, dmp), name)
        context.watch(act)
        views(name) = act
        act
    }
  }

  def receive = connected

  val connected: Receive = {
    case m: Message =>
      // we received a new message from another peer
      messages.append(m)

    case m @ (GetDoc(_) | DeleteDoc(_) | UpdateDoc(_, _)) =>
      context.parent.forward(m)

    case SynchronizationMessage(peerId, messages, commands) =>
      try {
        // broadcast all the messages to other peers
        implicit val timeout = Timeout(5.seconds)
        for (m <- messages)
          messageBus.publish(m)
        val futureCommands =
          for ((path, commands) <- commands)
            yield (getDocumentView(path) ? commands).mapTo[List[Command]]
        // TODO do MUCH MUCH better
        val responseCommands = concurrent.Await.result(futureCommands, 5.seconds)

        sender ! responseCommands
      } catch {
        case e: Exception =>
          logger.error(f"Error while processing synchronization from peer $peerId", e)
          sender ! akka.actor.Status.Failure(e)
      }

    case ReceiveTimeout =>
      // I haven't get any message in a while, I may go into idle mode
      // of course others must know
      messageBus.publish(Message(peerId, idlingMessage(paperId), Nil))
      context.setReceiveTimeout(timeout)
      context.become(idle)

    case Terminated(view) =>
      views -= view.path.name

  }

  val idle: Receive = {

    case ReceiveTimeout =>
      // well I've been idle for a while, just stop me
      context.stop(self)

    case Terminated(view) =>
      views -= view.path.name

    case m =>
      // I received a new message, I'm active again,
      // others must know and then I will process it
      messageBus.publish(Message(peerId, joiningMessage(paperId), Nil))
      context.become(connected)
      self.forward(m)
  }

}
