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

import scala.collection.mutable.Map

/** Some data to persist.
 *  Data is organized as a trie map.
 *  This abstraction makes it possible to have several storage kinds.
 *
 */
final class Data {

  private val _children = Map.empty[String, Data]

  var content: Option[String] = None

  def get(path: List[String]): Option[String] =
    path match {
      case Nil    => content
      case h :: t => _children.get(h).flatMap(_.get(t))
    }

  def getOrElseUpdate(path: List[String], s: => String): String =
    path match {
      case Nil =>
        content match {
          case Some(d) =>
            d
          case None =>
            val _d = s
            content = Some(_d)
            _d
        }
      case h :: t =>
        _children.get(h) match {
          case Some(c) =>
            c.getOrElseUpdate(t, s)
          case None =>
            val _d = s
            update(path, _d)
            _d
        }
    }

  def update(name: String, data: Data): Unit =
    _children(name) = data

  def update(path: List[String], data: String): Unit =
    path match {
      case Nil    => content = Some(data)
      case h :: t => _children.getOrElseUpdate(h, Data()).update(t, data)
    }

  def update(name: String, data: String): Unit =
    _children.get(name) match {
      case Some(d) => d.content = Some(data)
      case None    => _children(name) = Data(data)
    }

  def delete(path: List[String]): Unit =
    path match {
      case Nil     => content = None
      case List(n) => _children.remove(n)
      case h :: t  => _children.get(h).foreach(_.delete(t))
    }

  def contains(path: List[String]): Boolean =
    path match {
      case Nil    => content.isDefined
      case h :: t => _children.get(h).map(_.contains(t)).getOrElse(false)
    }

  def size = _children.size

  def children = _children.view

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

  def apply(content: String): Data = {
    val d = new Data
    d.content = Some(content)
    d
  }

  def apply(): Data =
    new Data

  def apply(sub: (String, Data)*): Data = {
    val d = new Data
    for ((name, d1) <- sub)
      d(name) = d1
    d
  }

}
