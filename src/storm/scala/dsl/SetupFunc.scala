// Copyright (c) 2011 Evan Chan

package storm.scala.dsl

// A trait with a DSL for defining initialization that should happen when every instance
// of a Bolt starts up.  Initialization logic in general should not go in the class
// constructor, because that will run only on the machine where you submit a topology,
// not on the nodes themselves.  Only prepare() (and thus setup) get run on each node.
//
// Use it like this:
// class MyBolt extends StormBolt(List("word")) {
//   var myIterator: Iterator[Int] = _
//   setup { myIterator = ...  }
// }
trait SetupFunc {
  var _setup:() => Unit = defaultSetup

  def defaultSetup() = {}

  def setup(func: => Unit) = { _setup = func _ }
}