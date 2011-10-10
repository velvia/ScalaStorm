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

    def emit(values: java.lang.Object*) = _collector.emit(_tuple, values.toList)

    def ack = _collector.ack(_tuple)
}
