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

import akka.actor.ActorRef
import akka.event.ActorEventBus

import scala.collection.mutable.Set

/** A message bus is created for each paper.
 *  All peers connecting to a paper must subscribe to the paper message bus
 *  to be notified about other peers events.
 *
 *  @author Lucas Satabin
 */
class MessageEventBus extends ActorEventBus {
  self =>

  // TODO manage dead actor references

  type Event = Message
  type Classifier = String

  private val subscribers = Set.empty[(String, ActorRef)]

  def subscribe(subscriber: ActorRef, to: String): Boolean = {
    val s = subscribers.size
    subscribers += (to -> subscriber)
    s == subscribers.size - 1
  }

  def unsubscribe(subscriber: ActorRef, from: String): Boolean = {
    val s = subscribers.size
    subscribers -= (from -> subscriber)
    s == subscribers.size + 1
  }

  def unsubscribe(subscriber: ActorRef): Unit =
    for {
      pair @ (_, ref) <- subscribers
      if (ref == subscriber)
    } subscribers -= pair

  def publish(event: Message): Unit =
    for {
      (peerId, ref) <- subscribers
      if peerId != event.from
    } ref ! event

}
