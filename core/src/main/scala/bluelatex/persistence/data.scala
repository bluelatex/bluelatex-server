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
package bluelatex.persistence

import scala.collection.mutable.Set

/** Some data to persist.
 *  Data is organized as a tree of `Container`s composed of sub-data.
 *  Leaves represent the content.
 *  This abstraction makes it possible have several storage kinds.
 *  Typically, in a filesystem storage, containers are directories and leaves are files.
 *
 */
sealed trait Data {
  val name: String
}

final case class Container(name: String)(var elements: Set[Data]) extends Data

final case class Leaf(name: String)(var content: String) extends Data
