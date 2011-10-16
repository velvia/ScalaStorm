package storm.scala.dsl

import backtype.storm.topology.IRichBolt
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.tuple.{Fields, Tuple, Values}
import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import collection.JavaConversions._
import java.util.Map


// The StormBolt class is an implementation of IRichBolt which
// provides a Scala DSL for making Bolt development concise.
class StormBolt(val outputFields: List[String]) extends IRichBolt {
    var _collector:OutputCollector = _
    var _context:TopologyContext = _
    var _conf:java.util.Map[_, _] = _
    var _tuple:Tuple = _

    var processFn: Tuple => Unit = { t => null }

    override def prepare(conf:java.util.Map[_, _], context:TopologyContext, collector:OutputCollector) = {
        _collector = collector
        _context = context
        _conf = conf
    }

    override def cleanup = {}

    override def execute(tuple: Tuple) = {
      _tuple = tuple
      processFn(tuple)
      _collector.ack(tuple)
    }

    override def declareOutputFields(declarer:OutputFieldsDeclarer) = {
        declarer.declare(new Fields(outputFields));
    }

    def process(codeBlock: Tuple => Unit) = { processFn = codeBlock }

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

    implicit def stormTupleListConverter(tuples: List[Tuple]) =
      new StormTupleList(_collector, tuples)
}
