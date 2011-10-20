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

Getting Started
===============

* Download [sbt](https://github.com/harrah/xsbt/wiki) version 0.10.1 or above
* clone this project
* In the root project dir, type `sbt run`.  SBT will automatically download all dependencies, compile the code, and give you a menu of topologies to run.

That's it!

Bolt DSL
========

Spout DSL
=========