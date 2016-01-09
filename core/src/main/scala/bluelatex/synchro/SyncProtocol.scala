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

import scala.concurrent.{
  Future,
  ExecutionContext
}

import spray.json._

/* Protocol adapted from Neil Fraser's mobwrite protocol:
 * http://code.google.com/p/google-mobwrite/wiki/Protocol
 */

/** A command sent for persisting a paper.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
final case object PersistPaper

/** A command sent for retrieving the last modification date of a paper.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
final case object GetLastModificationDate

/** A command sent for retrieving the document at the given path.
 *
 *  @author Lucas Satabin
 */
final case class GetDoc(path: Filepath)

/** A command sent for deleting the document at the given path.
 *
 *  @author Lucas Satabin
 */
final case class DeleteDoc(path: Filepath)

/** A command sent for updating the document content at given path.
 *
 *  @author Lucas Satabin
 */
final case class UpdateDoc(path: Filepath, content: String)

final case class Commands(value: List[Command], children: Map[String, Commands]) {

  def map(f: ((Filepath, List[Command])) => Future[List[Command]])(implicit ec: ExecutionContext): Future[Commands] = {
    def loop(path: Filepath, commands: Commands): Future[Commands] = {
      val value = f(path.reverse, commands.value)
      val children =
        for ((n, cs) <- commands.children)
          yield (n, loop(n :: path, cs))
      val children1 = Future.sequence(children.map { case (a, b) => b.map(a -> _) }).map(_.toMap)
      for {
        value <- value
        children <- children1
      } yield Commands(value, children)
    }
    loop(Nil, this)
  }

  def withFilter(f: ((Filepath, List[Command])) => Boolean): Commands =
    ???

}

/** A synchronization message as exchanged between client and server.
 *  A message is sent by or to a peer identified by its `peerId`,
 *  may contain zero or more application specific messages, and then the commands
 *  to apply on documents.
 *  Commands are represented as a json trie map.
 *  The paths in the trie map represent document paths in the paper.
 *  Each node contains a list of commands to perform in order on the document.
 *
 *  @author Lucas Satabin
 */
final case class SynchronizationMessage(peerId: String, messages: List[Message], commands: Commands)

/** Broadcast a message to all peers currently viewing the session paper.
 *
 *  @author Audric Schiltknecht
 */
final case class Message(from: PeerId, json: JsObject, filename: Filepath)

/** A command to apply on a file from a given peer
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
final case class Command(revision: Long, action: SyncAction)

/** An action to apply on a file from a given peer
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 */
sealed trait SyncAction

/** Request an edit to be made to the current session peer and file
 *  with given client (when sent by client)
 *  or server (when sent by server) revision.
 */
final case class Delta(revision: Long, data: List[Edit], overwrite: Boolean) extends SyncAction

/** Transmit the entire contents of the session file.
 */
final case class Raw(revision: Long, data: String, overwrite: Boolean) extends SyncAction

/** Delete the session file.
 */
case object Nullify extends SyncAction

/** Commands to edit file.
 *
 *  @author Audric Schiltknecht
 *  @author Lucas Satabin
 *
 */
sealed trait Edit {
  val length: Int
}

object Edit {

  def unapply(s: String): Option[Edit] =
    EditCommandParsers.parseEdit(s)
}

/** Keep `length` characters from current position.
 */
final case class Equality(length: Int) extends Edit {
  override def toString = s"=$length"
}
/** Delete `length` characters from the current position.
 */
final case class Delete(length: Int) extends Edit {
  override def toString = s"-$length"
}
/** Add the text at the current position.
 */
final case class Add(text: String) extends Edit {
  val length = text.length
  override def toString = s"+$text"
}
