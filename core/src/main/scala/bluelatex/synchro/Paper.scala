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

import persistence.{
  Load => PLoad,
  Save => PSave,
  Delete => PDelete
}

import scala.collection.mutable.Map
import akka.actor.{
  Actor,
  ActorRef,
  Props,
  Status,
  Stash,
  Terminated
}
import akka.event.Logging

import java.util.{
  Date,
  Calendar
}

import name.fraser.neil.plaintext.DiffMatchPatch

import scala.annotation.tailrec

import java.io.FileNotFoundException

/** This actor handles synchronisation of documents.
 *  It represents the files of a given document.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
class Paper(
  paperId: PaperId,
  store: ActorRef,
  dmp: DiffMatchPatch)
    extends Actor with Stash {

  private val messageBus =
    new MessageEventBus

  private val peers = Map.empty[String, ActorRef]

  private def getPeerRef(peerId: String): ActorRef =
    peers.get(peerId) match {
      case Some(ref) =>
        ref
      case None =>
        // create the actor
        val act = context.actorOf(Props(classOf[Peer], paperId, peerId, messageBus), peerId)
        context.watch(act)
        peers(peerId) = act
        act
    }

  private var documents: Data = Data()

  private var lastModificationTime = Calendar.getInstance().getTime()

  val logger = Logging(context.system, this)

  override def preStart(): Unit = {
    // the paper is loaded
    store ! PLoad(List(paperId))
  }

  def receive = starting

  def starting: Receive = {
    case d: Data =>
      documents = d
      unstashAll()
      context.become(started)

    case Status.Failure(_: FileNotFoundException) =>
      // if it does not exist yet, just use the fresh one
      unstashAll()
      context.become(started)

    case Status.Failure(e) =>
      // log the error and stop actor
      logger.error(f"Error while loading synchornization actor for $paperId", e)
      context.stop(self)

    case SynchronizationMessage(_, _, _) | PersistPaper | GetLastModificationDate | GetDoc(_) | DeleteDoc(_) | UpdateDoc(_, _) =>
      stash()
  }

  def started: Receive = {

    case Status.Failure(e) =>
      // log the error
      logger.error(f"An error occurred when persisting paper $paperId", e)

    case session @ SynchronizationMessage(peerId, _, _) =>
      // forward to the peer actor
      getPeerRef(peerId).forward(session)

    case GetDoc(path) =>
      // get or create document
      sender ! documents.getOrElseUpdate(path, "")

    case DeleteDoc(path) =>
      documents.delete(path)

    case UpdateDoc(path, content) =>
      documents(path) = content
      lastModificationTime = Calendar.getInstance.getTime

    case PersistPaper =>
      persistPapers()

    case GetLastModificationDate =>
      sender ! lastModificationTime

    case Terminated(peer) =>
      peers -= peer.path.name
      // if all peers terminated, then terminate this paper representation
      if (peers.isEmpty)
        context.stop(self)

  }

  override def postStop(): Unit = {
    logger.info(f"Stop command received for paper $paperId")
    persistPapers()
  }

  def persistPapers(): Unit =
    store ! PSave(documents)

}
