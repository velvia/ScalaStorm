ScalaStorm provides a Scala DSL for Nathan Marz's [Storm](https://github.com/nathanmarz/storm) real-time computation system. It also provides a framework for Scala and SBT development of Storm topologies.

For example, here is the SplitSentence bolt from the word count topology:

```scala
class SplitSentence extends StormBolt(outputFields = List("word")) {
  def execute(t: Tuple) = t matchSeq {
    case Seq(sentence: String) => sentence split " " foreach
      { word => using anchor t emit (word) }
    t ack
  }
}
```

A couple things to note here:

* The matchSeq DSL enables Scala pattern matching on Storm tuples.  Notice how it gives you a nice way to name and identify the type of each component.  Now imagine the ability to match on different tuple types, like in a join, easily and elegantly!
* The emit DSL reads like English and easily takes multiple args (val1, val2, ...)
* Output fields are easily declared
* It's easy to see exactly when the emits and ack happen

Useful features for Scala developers:

* Auto-boxing of Scala primitives in tuple emit and matchSeq
* A BoltDsl trait for using the DSL from any thread/actor/class

0.2.4
=====
Added support for multiple streams in Spouts:
```scala
class MultiStreamSpout extends StormSpout(Map("city" -> List("city"), "browser" -> List("browser"))) {
}
```
Switched to Apache Storm distribution
Build system updated to sbt 0.13.5
Build system supports crosscompiling for scala 2.9/2.10
ShutdownFunc trait added to StormSpout

Please Read For 0.2.2 / Storm 0.8.0+ Users
=========================================
Storm 0.8.0 emits are no longer thread safe.  You may see NullPointerExceptions with DisruptorQueue in the stack trace.
If you are doing emits from multiple threads or actors, you will need to synchronize your emits or have them
come from a single thread.  You should synchronize on the collector instance:

```scala
   _collector.synchronized { tuple emit (val1, val2) }
```

## Functional Trident (NEW!)

There is a sample Trident topology, in src/storm/scala/examples/trident.  It features an
experimental new DSL for doing functional Trident topologies (see FunctionalTrident.scala).  I am
currently soliciting feedback for this feature, so drop me a line if you like it.

Getting Started
===============

The latest version of scala-storm, 0.2.2, corresponds to Storm 0.8.1 and is available from Maven Central.  Add this to your build.sbt:

```scala
libraryDependencies += "com.github.velvia" %% "scala-storm" % "0.2.2"
```

Version 0.2.0 is available from Maven central and corresponds to Storm 0.7.1.
```scala
libraryDependencies += "com.github.velvia" %% "scala-storm" % "0.2.0"
```

In both cases, you will need additional repos, as maven central does not host the Storm/clojure jars:
```scala
resolvers ++= Seq("clojars" at "http://clojars.org/repo/",
                  "clojure-releases" at "http://build.clojure.org/releases")
```

If you want to build from source:

* Download [sbt](https://github.com/harrah/xsbt/wiki) version 0.10.1 or above
* clone this project
* In the root project dir, type `sbt test:run`.  SBT will automatically download all dependencies, compile the code, and give you a menu of topologies to run.

To help you get started, the ExclamationTopology and WordCountTopology examples from storm starter have been included.

Bolt DSL
========
The Scala DSL for bolts is designed to support many different bolt designs, including all 10 variants of the collector emit() and emitDirect() APIs.  Getting started consists of extending the StormBolt class, passing a list of output fields, and defining the execute method:

```scala
class ExclamationBolt extends StormBolt(outputFields = List("word")) {
  def execute(t: Tuple) = {
    t emit (t.getString(0) + "!!!")
    t ack
  }
}
```

If you need to emit to multiple output streams, that can be done by passing a Map with the key being the stream name/Id, and the value being the list of fields for each stream (See the AggregationTopology example):
```scala
class Splitter extends StormBolt(Map("city" -> List("city"), "browser" -> List("browser"))) {
}
```

BoltDsl trait
-------------
If you want to use the emit DSL described below in a thread or Actor, you can use the BoltDsl trait.  You just have to initialise the _collector variable.

```scala
class DataWorker(val collector: OutputCollector) extends Actor with BoltDsl {
  _collector = collector
  ...
  def receive = {
    no anchor emit (someString, someInt)
  }
}
```

matchSeq
--------
The `matchSeq` method passes the storm tuple as a Scala Seq to the given code block with one or more case statements. The case statements need to have Seq() in order to match the tuple.  If none of the cases match, then by default a handler which throws a RuntimeError will be used.  It is a good idea to include your own default handler.

matchSeq allows easy naming and safe typing of tuple components, and allows easy parsing of different tuple types.  Suppose that a bolt takes in a data stream from one source and a clock or timing-related stream from another source. It can be handled like this:

```scala
def execute(t: Tuple) = t matchSeq {
	case Seq(username: String, followers: List[String]) => // process data
	case Seq(timestamp: Integer) =>   // process clock event
}
```

Unboxing will be automatically performed.  Even though everything going over the wire has to be a subset of java.lang.Object, if you match on a Scala primitive, it will automatically unbox it for you.

By default, if none of the cases are matched, then ScalaStorm will throw a RuntimeException with a message "unhandled tuple".  This can be useful for debugging in local mode to quickly discover matching errors.  If you want to handle the unhandled case yourself, simply add `case _ => ...` as the last case.

emit and emitDirect
-------------------
emit takes a variable number of AnyRef arguments which make up the tuple to emit.  emitDirect is the same but the first argument is the Int taskId, followed by a variable number of AnyRefs.

To emit a tuple anchored on one tuple, where t is of type Tuple, do one of the following:

```scala
using anchor t emit (val1, val2, ...)
using anchor t emitDirect (taskId, val1, val2, ...)
anchor(t) emit (val1, val2, ...)
t emit (val1, val2, ...)
```

To emit a tuple to a particular stream:

```scala
using anchor t toStream 5 emit (val1, val2, ...)
using anchor t toStream 5 emitDirect (taskId, val1, val2, ...)
```

To emit anchored on multiple tuples (can be any Seq, not just a List):

```scala
using anchor List(t1, t2) emit (val1, val2, ...)
using anchor List(t1, t2) emitDirect (taskId, val1, val2, ...)
```

To emit unanchored:

```scala
using no anchor emit (val1, val2, ...)
using no anchor emitDirect (taskId, val1, val2, ...)
using no anchor toStream 5 emit (val1, val2, ...)
using no anchor toStream 5 emitDirect (taskId, val1, val2, ...)
```

ack
---
```scala
t ack                // Ack one tuple
List(t1, t2) ack     // Ack multiple tuples, in order of list
```

A note on types supported by emit (...)
---------------------------------------
Any scala type may be passed to emit() so long as it can be autoboxed into an AnyRef (java.lang.Object).  This includes Scala Ints, Longs, and other basic types.

Spout DSL
=========
The Scala Spout DSL is very similar to the Bolt DSL.  One extends the StormSpout class, declaring the output fields, and defines the nextTuple method:

```scala
class MySpout extends StormSpout(outputFields = List("word", "author")) {
  def nextTuple = {}
}
```

Spout emit DSL
--------------
The spout emit DSL is very similar to the bolt emit DSL.  Again, all variants of the SpoutOutputCollector emit and emitDirect APIs are supported.  The basic forms for emitting tuples are as follows:

```scala
emit (val1, val2, ...)
emitDirect (taskId, val1, val2, ...)
```

To emit a tuple with a specific message ID:

```scala
using msgId 9876 emit (val1, val2, ...)
using msgId 9876 emitDirect (taskId, val1, val2, ...)
```

To emit a tuple to a specific stream:

```scala
toStream 6 emit (val1, val2, ...)
toStream 6 emitDirect (taskId, val1, val2, ...)
using msgId 9876 toStream 6 emit (val1, val2, ...)
using msgId 9876 toStream 6 emitDirect (taskId, val1, val2, ...)
```

Bolt and Spout Setup
====================
You will probably need to initialize per-instance variables at each bolt and spout for all but the simplest of designs.  You should not do this in the Bolt or Spout constructor, as the constructor is only called before submitting the Topology.  What you instead need to do is to override the prepare() and open() methods, and do your setup in there, but there is a convenient `setup` DSL that lets you perform whatever per-instance initialization is needed, in a concise and consistent manner.  To use it:

```scala
class MyBolt extends StormBolt(List("word")) {
  var myIterator: Iterator[Int] = _
  setup { myIterator = ...  }
}
```

License
=======
Apache 2.0.   Please see LICENSE.md.
All contents copyright (c) 2012, Evan Chan.
