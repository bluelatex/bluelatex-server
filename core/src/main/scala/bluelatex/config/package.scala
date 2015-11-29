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

import collection.JavaConverters._

import com.typesafe.config.{
  Config,
  ConfigValue
}

/** The `config` package contains utilities to deal with configuration files.
 *  It mainly provides scala-ish access to the configuration class and exposes
 *  nice utilities to iterate over configuratons keys.
 *
 */
package object config extends StdReaders with StdExtractors {

  implicit class RichConfig(val config: Config) extends AnyVal {

    def get[T: ConfigReader](path: String): Option[T] =
      implicitly[ConfigReader[Option[T]]].read(config, path)

    def as[T: ConfigReader](path: String): T =
      implicitly[ConfigReader[T]].read(config, path)

    def map[K, V](f: ((String, ConfigValue)) => (K, V)): Map[K, V] =
      for {
        kv <- config.root.asScala.toMap
      } yield f(kv)

    def foreach(f: ((String, ConfigValue)) => Unit): Unit =
      for {
        kv <- config.root.asScala
      } f(kv)

    def map[U](f: ((String, ConfigValue)) => U): Iterable[U] =
      for {
        kv <- config.root.asScala
      } yield f(kv)

    def flatMap[K, V](f: ((String, ConfigValue)) => Map[K, V]): Map[K, V] =
      for {
        kv <- config.root.asScala.toMap
        u <- f(kv)
      } yield u

    def flatMap[U](f: ((String, ConfigValue)) => U): Iterable[U] =
      for {
        kv <- config.root.asScala
      } yield f(kv)

    def filter(f: ((String, ConfigValue)) => Boolean): Map[String, ConfigValue] =
      for {
        kv <- config.root.asScala.toMap
        if f(kv)
      } yield kv

    def withFilter(f: ((String, ConfigValue)) => Boolean): Map[String, ConfigValue] =
      filter(f)

  }

}
