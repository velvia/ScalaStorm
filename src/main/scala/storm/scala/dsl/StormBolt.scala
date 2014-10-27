// Copyright (c) 2011 Evan Chan

package storm.scala.dsl

import backtype.storm.topology.base.BaseRichBolt
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.tuple.{Fields, Tuple}
import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import scala.language.implicitConversions

// The StormBolt class is an implementation of IRichBolt which
// provides a Scala DSL for making Bolt development concise.
// To use, extend this class and implement the execute(t: Tuple) method.
//
// The following DSLs for emitting are supported:
//   using anchor <tuple> emit (...)
//   anchor(<tuple>) emit (...)
//   <tuple> emit (...)
//   using no anchor emit (...)
abstract class StormBolt(val streamToFields: collection.Map[String, List[String]])
    extends BaseRichBolt with SetupFunc with ShutdownFunc with BoltDsl {
    var _context: TopologyContext = _
    var _conf: java.util.Map[_, _] = _

    // A constructor for the common case when you just want to output to the default stream
    def this(outputFields: List[String]) = { this(Map("default" -> outputFields)) }

    def prepare(conf:java.util.Map[_, _], context:TopologyContext, collector:OutputCollector) {
        _collector = collector
        _context = context
        _conf = conf
        _setup()
    }

    def declareOutputFields(declarer: OutputFieldsDeclarer) {
      streamToFields foreach { case(stream, fields) =>
        declarer.declareStream(stream, new Fields(fields:_*))
      }
    }
    
    override def cleanup() = _cleanup()
}

/**
 *  This trait allows the bolt emit DSL to be used outside of the main bolt class, say in an Actor or
 *  separate thread.  To use it you just need to initialise _collector before using it.
 */
trait BoltDsl {
  var _collector: OutputCollector = _

  // Declare an anchor for emitting a tuple
  def anchor(tuple: Tuple) = new StormTuple(_collector, tuple)

  def anchor(tuples: List[Tuple]) = new StormTupleList(_collector, tuples)

  // Use this for unanchored emits:
  //    using no anchor emit (val1, val2, ...)
  def no(s: String) = new UnanchoredEmit(_collector)

  val anchor = ""

  // Combine with anchor for a cool DSL like this:
  // using anchor t emit (val1, val2, ..)
  def using = this

  // implicitly convert to a stormTuple for easy emit syntax like
  // tuple emit (val1, val2, ...)
  implicit def stormTupleConvert(tuple: Tuple) =
    new StormTuple(_collector, tuple)

  implicit def stormTupleListConverter(tuples: Seq[Tuple]) =
    new StormTupleList(_collector, tuples)
}
