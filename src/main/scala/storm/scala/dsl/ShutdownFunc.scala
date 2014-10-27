package storm.scala.dsl

// @author rwagner
// 
// A trait with a DSL for defining shutdown code, that should happen when an instance
// of a Bolt shuts down up. Acts as a counterpart for SetupFunc.
// 
// Use it like this:
// class MyBolt extends StormBolt(List("word")) {
//   val myResouce= AnyResource(config)
//   setup { myResouce.open()  }
//   shutdown { myResouce.close()  }
// }
trait ShutdownFunc {

  // register a shutdown function
  def shutdown(sf: => Unit) = _shutdownFunctions ::= sf _

  // fire all registered shutdown functions
  protected def _cleanup() = _shutdownFunctions.foreach(_())

  // list of registered shutdown functions
  private var _shutdownFunctions: List[() => Unit] = Nil
}