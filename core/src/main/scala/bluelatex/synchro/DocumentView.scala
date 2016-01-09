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

import akka.actor.{
  Actor,
  ActorRef,
  ReceiveTimeout
}
import akka.event.Logging

import name.fraser.neil.plaintext.DiffMatchPatch

import scala.reflect.classTag

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

import scala.concurrent.duration._

import java.util.{
  Calendar,
  Date
}

/** A document actor handles synchronization commands for a given document of a paper.
 *
 *  @author Lucas Satabin
 */
class DocumentView(peer: String, filepath: Filepath, dmp: DiffMatchPatch) extends Actor {

  implicit private def ec = context.system.dispatcher

  val CommandList = classTag[List[Command]]

  val logger = Logging(context.system, this)

  /** Last version of the text sent to the client */
  var shadow: String = ""
  /** Previous version of the text sent to the client */
  var backupShadow: String = ""

  /** The client's version for the shadow (n) */
  var serverShadowRevision: Long = 0
  /** The server's version for the shadow (m) */
  var clientShadowRevision: Long = 0
  /** The server's version for the backup shadow */
  var backupShadowRevision: Long = 0

  /** List of unacknowledged edits sent to the client */
  var edits = ListBuffer.empty[Command]

  /** Is the delta valid ? */
  var deltaOk = true
  /** Has the view changed since it was last saved ? */
  var changed = false

  /** Does the client set the overwrite flag ? */
  var overwrite = false

  /** Last modification date for this document */
  var lastUpdate: Date = Calendar.getInstance().getTime()

  /** Flag if messages are to be retrieved or not */
  var retrieveMessages = false

  /** The view document content */
  var document = ""

  context.setReceiveTimeout(5.seconds)

  def receive = {
    case CommandList(commands) =>
      context.parent ! GetDoc(filepath)
      context.become(treatingDoc(commands), false)

    case ReceiveTimeout =>
      context.stop(self)
  }

  def treatingDoc(commands: List[Command]): Receive = {
    case document: String =>
      for (Command(revision, action) <- commands) {

        logger.debug(f"Apply SyncAction: peer=$peer, filename=$filepath, revision=$revision, action=$action")
        logger.debug(f"Server shadow revision=${serverShadowRevision}, backup shadow revision=${backupShadowRevision}")

        // Start by checking revision
        if ((revision != serverShadowRevision)
          && (revision == backupShadowRevision)) {
          // If the version number does not equal the version number of the shadow,
          // but does equal the version number of the backup shadow, then
          // the previous response was lost, which means the backup shadow
          // and its version number should be copied over to the shadow (step 4)
          // and the local stack should be cleared. Continue.
          logger.debug("Restore backup shadow")
          restoreBackupShadow()
        }

        // If the version number is equal to one of the edits on the local stack,
        // then this is an acknowledgment of receipt of those edits, which
        // means that edit and those with smaller version numbers should be dropped.
        // Continue.
        edits = edits.filter {
          case Command(editRev, _) => (editRev > revision)
        }

        if (revision != serverShadowRevision) {
          // If the version number is not equal the version number of the shadow,
          // then this means there is a memory/programming/transmission
          // bug so the system should be reinitialized using a 'raw' command.
          // Do not accept any Delta commands for this file
          logger.warning(f"Error: revision ($revision) != serverShadowRevision (${serverShadowRevision}) for file $filepath")
          deltaOk = false
        } else if (revision == serverShadowRevision) {
          // The version number matches the shadow, proceed.
          deltaOk = true
        }

        action match {
          case x: Delta => processDelta(x, revision)
          case x: Raw   => processRaw(x, revision)
          case Nullify  => nullify()
        }
      }

      val actions = flushStack()
      sender ! actions.map(Command(clientShadowRevision, _))
      context.unbecome()

    case ReceiveTimeout =>
      context.stop(self)
  }

  def nullify(): Unit = {
    context.parent ! DeleteDoc(filepath)
    context.stop(self)
  }

  def processDelta(delta: Delta, serverRevision: Long): Unit = {
    logger.debug(f"Process delta command=$delta, for serverRevision=$serverRevision")

    if (!deltaOk) {
      logger.debug("Invalid delta, abort")
      return
    }

    if (serverRevision < serverShadowRevision) {
      // Ignore delta on mismatched server shadow
      logger.debug("Mismatched server shadow, ignore")
    } else if (delta.revision > clientShadowRevision) {
      // System should be re-initialised with Raw command
      deltaOk = false
      logger.debug("Error: wait for Raw command")
    } else if (delta.revision < clientShadowRevision) {
      // Already seen it, drop
      logger.debug("Delta already processed, drop")
    } else {
      logger.debug("Delta and revision ok, process patches")
      applyPatches(delta)
      clientShadowRevision += 1
      update()
    }
    overwrite = delta.overwrite
  }

  def processRaw(raw: Raw, serverRevision: Long): Unit = {
    setShadow(raw.data, raw.revision, serverRevision, raw.overwrite)
  }

  def applyPatches(delta: Delta): Unit = {
    // Compute diffs
    // XXX: not very Scala-ish...
    val diffs =
      try {
        dmp.diff_fromDelta(shadow, delta.data map { _.toString })
      } catch {
        case e: Exception =>
          logger.warning(f"Delta failure, expected length ${shadow.length}")
          deltaOk = false
          null
      }
    if (diffs != null) {

      // Expand diffs into patch
      val patch = dmp.patch_make(shadow, diffs)

      // Update client shadow first
      shadow = dmp.diff_text2(diffs)
      backupShadow = shadow
      backupShadowRevision = serverShadowRevision
      changed = true

      logger.debug(f"Old document text: ${document}")

      // Update server document
      val mastertext = if (delta.overwrite) {
        if (patch != null) {
          shadow
        } else {
          document
        }
      } else {
        val Array(text: String, _) = dmp.patch_apply(patch, document)
        text
      }
      logger.debug(f"New document text: $mastertext")
      document = mastertext

      if (!patch.isEmpty) {
        logger.debug("Patch is not empty -> update modification time")
      }
    }
  }

  def flushStack(): List[SyncAction] = {
    logger.debug("Flush stack")

    val mastertext = document
    val filename = filepath

    if (deltaOk) {
      logger.debug(f"Deltas OK, compute and apply diff on mastertext=$mastertext")
      // compute the diffs with the current master text
      val diffs = dmp.diff_main(shadow, mastertext)
      dmp.diff_cleanupEfficiency(diffs)
      // Convert diffs to Edit commands
      val edits1 = diffs2Edits(diffs)

      logger.debug(f"Computed deltas: $edits1")

      if (overwrite) {
        // Client sending 'D' means number, no error.
        // Client sending 'R' means number, client error.
        // Both cases involve numbers, so send back an overwrite delta.
        edits.append(
          Command(
            serverShadowRevision,
            Delta(serverShadowRevision,
              edits1,
              true)))
      } else {
        // Client sending 'D' means number, no error.
        // Client sending 'R' means number, client error.
        // Both cases involve numbers, so send back a merge delta.
        edits.append(
          Command(
            serverShadowRevision,
            Delta(serverShadowRevision,
              edits1,
              false)))
      }
      serverShadowRevision += 1
    } else {
      // Error server could not parse client's delta.
      logger.debug("Invalid delta(s)")
      // Send a raw dump of the text.
      clientShadowRevision += 1

      if (mastertext.isEmpty) {
        edits.append(
          Command(
            serverShadowRevision,
            Raw(serverShadowRevision,
              "",
              false)))
      } else {
        edits.append(
          Command(
            serverShadowRevision,
            Raw(serverShadowRevision,
              mastertext,
              true)))
      }
    }

    shadow = mastertext
    changed = true

    edits.toList.map {
      case Command(_, command) => command
    }
  }

  /** Utility function to convert a list of DMP's diffs to a list of Edit objects */
  private def diffs2Edits(diffs: java.util.List[DiffMatchPatch.Diff]): List[Edit] =
    diffs.toList map (d => d.operation match {
      case DiffMatchPatch.Operation.INSERT => Add(d.text)
      case DiffMatchPatch.Operation.DELETE => Delete(d.text.length())
      case DiffMatchPatch.Operation.EQUAL  => Equality(d.text.length())
    })

  def restoreBackupShadow(): Unit = {
    edits.clear()
    shadow = backupShadow
    serverShadowRevision = backupShadowRevision
    update()
  }

  def setShadow(text: String, clientRevision: Long, serverRevision: Long, force: Boolean): Unit = {
    deltaOk = true
    shadow = text
    clientShadowRevision = clientRevision
    serverShadowRevision = serverRevision
    backupShadow = shadow
    backupShadowRevision = serverShadowRevision
    edits.clear()

    if (force || document.isEmpty) {
      document = text
    }
    overwrite = force
    update()
  }

  def update(): Unit =
    lastUpdate = Calendar.getInstance().getTime()

}
