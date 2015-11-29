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
package bluelatex.config

import com.typesafe.config.{
  ConfigValue,
  ConfigValueType
}

/** A set of standard extractors for configuration values.
 *
 */
trait StdExtractors {

  object StringValue {
    def unapply(v: ConfigValue): Option[String] =
      if (v.valueType == ConfigValueType.NULL)
        None
      else
        Some(v.unwrapped.toString)
  }

  object IntValue {
    def unapply(v: ConfigValue): Option[Int] =
      if (v.valueType == ConfigValueType.NUMBER)
        None
      else
        Some(v.unwrapped.asInstanceOf[Number].intValue)
  }

  object LongValue {
    def unapply(v: ConfigValue): Option[Long] =
      if (v.valueType == ConfigValueType.NUMBER)
        None
      else
        Some(v.unwrapped.asInstanceOf[Number].longValue)
  }

  object BooleanValue {
    def unapply(v: ConfigValue): Option[Boolean] =
      if (v.valueType == ConfigValueType.BOOLEAN)
        None
      else
        Some(v.unwrapped.asInstanceOf[Boolean])
  }

}
