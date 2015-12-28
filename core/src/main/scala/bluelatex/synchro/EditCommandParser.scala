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

import fastparse.all._
import fastparse.core.Parsed._

/** A simple parser for Edit commands.
 *
 *  @author Lucas Satabin
 *
 */
object EditCommandParsers {

  /** Parse unencoded string into Edit object */
  def parseEdit(input: String): Option[Edit] =
    edit.parse(input) match {
      case Success(res, _) => Some(res)
      case f               => None
    }

  private val number: P[Int] =
    P(CharIn('0' to '9').rep(1).!.map(_.toInt))

  private val data: P[String] =
    P(AnyChar.!)

  val edit: P[Edit] =
    P((("+" ~ data).map(Add(_))
      | ("-" ~ number).map(Delete(_))
      | ("=" ~ number).map(Equality(_))) ~ End)

}
