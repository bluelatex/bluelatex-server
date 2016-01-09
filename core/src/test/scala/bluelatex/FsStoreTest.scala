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
  Props
}

import akka.pattern.ask

import scala.concurrent.duration._
import scala.concurrent.Await

import akka.testkit.{
  TestActors,
  TestKit,
  ImplicitSender
}
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues

import scala.collection.{ mutable => mu }

import better.files._
import Cmds._

class FsStoreTest(_system: ActorSystem)
    extends TestKit(_system)
    with ImplicitSender
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with OptionValues {

  def this() = this(ActorSystem("fsstore-test-system"))

  val data = Data().updated(List("tata", "titi"), "piouc").updated(List("tata", "tutu"), "plop").updated("tete", "gloups")

  val base = File.newTempDir()

  val tata = base / "tata"
  val titi = tata / "titi"
  val tutu = tata / "tutu"
  val tete = base / "tete"
  val toto = tata / "toto"

  mkdirs(tata)
  toto.write("pimp")

  val actor = system.actorOf(Props(classOf[FsStore], base, Set.empty), base.name)

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "a directory structure" should "be saved to FS when Save message is sent" in {
    actor ! Save(data)
    expectMsg(Unit)

    titi.contentAsString should be("piouc")
    tutu.contentAsString should be("plop")
    tete.contentAsString should be("gloups")

  }

  it should "not delete other present files in directories" in {
    toto.exists should be(true)
    toto.contentAsString should be("pimp")
  }

  it should "be loaded from FS when Load message is sent" in {
    actor ! Load(List())

    val data = expectMsgClass(classOf[Option[Data]])

    data should be('defined)

    data.value.foreach {
      case (List("tata", "titi"), s) =>
        s should be("piouc")
      case (List("tata", "tutu"), s) =>
        s should be("plop")
      case (List("tata", "toto"), s) =>
        s should be("pimp")
      case (List("tete"), s) =>
        s should be("gloups")
      case (p, s) =>
        fail("Unexpected data")
    }
  }

  it should "be deleted from FS when Delete message is sent" in {

    actor ! Delete(List("tata"))

    expectMsg(Unit)

    tete.exists should be(true)
    tata.exists should be(false)
    titi.exists should be(false)
    tutu.exists should be(false)
    toto.exists should be(false)
    tete.contentAsString should be("gloups")

  }

  "a file" should "only be saved to disk if it was modified" in {
    val modificationTime = tete.lastModifiedTime

    actor ! Save(data)

    expectMsg(Unit)

    tete.exists should be(true)
    tata.exists should be(true)
    titi.exists should be(true)
    tutu.exists should be(true)
    toto.exists should be(false)
    tete.lastModifiedTime should be(modificationTime)
  }

}
