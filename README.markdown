\BlueLaTeX Server [![Build Status](https://travis-ci.org/gnieh/bluelatex.png?branch=master)](https://travis-ci.org/bluelatex/bluelatex-server)
=================

The \BlueLaTeX server that allows people to collaboratively write documents that serializes to a hierarchy of text entities with real-time synchronization.

Originally designed to specifically edit \LaTeX documents, it is actually independent of the file format.
For example it can as well synchronize markdown documents.
The server exposes a Rest API which can be called by any client of your choice that talks HTTP.

Repository Layout
-----------------

\BlueLaTeX is composed of several modules that are distributed over several folders:
 - `core` contains the core server features, such as the core Rest Api to manage users, sessions and document synchronization,

Building \BlueLaTeX
-------------------

\BlueLaTeX uses [sbt](http://scala-sbt.org) as build system and dependency manager. Building \BlueLaTeX can be done by simply typing `sbt compile` in a console.

It requires java8 to build due to dependencies to the autocloseable feature for I/O.

License
-------

See the [LICENSE](https://github.com/bluelatex/bluelatex-server/blob/master/LICENSE) file.
