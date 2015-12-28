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
  Props,
  Terminated
}
import scala.collection.mutable.Map

import name.fraser.neil.plaintext.DiffMatchPatch

class Synchronizer(store: ActorRef) extends Actor {

  val dmp = new DiffMatchPatch

  val papers = Map.empty[String, ActorRef]

  def getPaper(paperId: String) =
    papers.get(paperId) match {
      case Some(ref) =>
        ref
      case None =>
        // create the paper
        val act = context.actorOf(Props(classOf[Paper], paperId, store, dmp), paperId)
        context.watch(act)
        papers(paperId) = act
        act
    }

  def receive = {
    case (paperId: String, m) =>
      getPaper(paperId).forward(m)

    case Terminated(ref) =>
      papers -= ref.path.name
  }

}
