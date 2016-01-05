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

/** Some data to persist.
 *  Data is organized as a trie map.
 *  Leaves represent the content.
 *  This abstraction makes it possible have several storage kinds.
 *  Typically, in a filesystem storage, containers are directories and leaves are files.
 *
 */
final case class Data(content: Option[String] = None, children: Map[String, Data] = Map.empty) {

  val lastChanged: Long = System.currentTimeMillis

  def get(path: List[String]): Option[String] =
    path match {
      case Nil    => content
      case h :: t => children.get(h).flatMap(_.get(t))
    }

  def getOrElse(path: List[String], s: => String): String =
    path match {
      case Nil =>
        content match {
          case Some(d) =>
            d
          case None =>
            s
        }
      case h :: t =>
        children.get(h) match {
          case Some(c) =>
            c.getOrElse(t, s)
          case None =>
            s
        }
    }

  def updated(name: String, data: String): Data =
    copy(children = children.updated(name, children.getOrElse(name, Data()).copy(content = Some(data))))

  def updated(path: List[String], data: String): Data =
    path match {
      case Nil =>
        copy(content = Some(data))
      case h :: t =>
        copy(children = children.updated(h, children.getOrElse(h, Data()).updated(t, data)))
    }

  def delete(path: List[String]): Data =
    path match {
      case Nil     => Data()
      case List(n) => copy(children = children - n)
      case h :: t =>
        children.get(h) match {
          case Some(d) =>
            copy(children = children.updated(h, d.delete(t)))
          case None =>
            this
        }
    }

  def contains(path: List[String]): Boolean =
    path match {
      case Nil    => content.isDefined
      case h :: t => children.get(h).map(_.contains(t)).getOrElse(false)
    }

  def size = children.size

  def foreach(f: (List[String], String) => Unit): Unit = {
    def loop(htap: List[String], d: Data): Unit =
      d.content match {
        case None =>
          for ((n, d) <- d.children)
            loop(n :: htap, d)
        case Some(s) =>
          f(htap.reverse, s)
          for ((n, d) <- d.children)
            loop(n :: htap, d)
      }
    loop(Nil, this)
  }

}

object Data {

  def apply(content: String): Data =
    Data(content = Some(content))

  def apply(sub: (String, Data)*): Data =
    Data(children = Map(sub: _*))

}
