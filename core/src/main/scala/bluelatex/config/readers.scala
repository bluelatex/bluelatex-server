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

import scala.util.Try

import scala.concurrent.duration.FiniteDuration

import scala.collection.JavaConverters._

import com.typesafe.config.Config

import java.net.URI

/** A set of standard configuration readers.
 *
 */
trait StdReaders {

  implicit def optionConfigReader[T: ConfigReader]: ConfigReader[Option[T]] = new ConfigReader[Option[T]] {
    def read(config: Config, path: String) =
      if (config.hasPath(path))
        Try(config.as[T](path)).toOption
      else
        None
  }

  implicit object StringConfigReader extends ConfigReader[String] {
    def read(config: Config, path: String) =
      config.getString(path)
  }

  implicit object IntConfigReader extends ConfigReader[Int] {
    def read(config: Config, path: String) =
      config.getInt(path)
  }

  implicit object LongConfigReader extends ConfigReader[Long] {
    def read(config: Config, path: String) =
      config.getLong(path)
  }

  implicit object DurationConfigReader extends ConfigReader[FiniteDuration] {
    import java.util.concurrent.TimeUnit
    def read(config: Config, path: String) =
      FiniteDuration(config.getDuration(path, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
  }

  implicit object BooleanConfigReader extends ConfigReader[Boolean] {
    def read(config: Config, path: String) =
      config.getBoolean(path)
  }

  implicit object UriConfigReader extends ConfigReader[URI] {
    def read(config: Config, path: String) =
      new URI(config.getString(path))
  }

  implicit object StringListConfigReader extends ConfigReader[List[String]] {
    def read(config: Config, path: String) =
      config.getStringList(path).asScala.toList
  }

}
