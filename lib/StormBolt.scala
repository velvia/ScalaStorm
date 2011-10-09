package storm.scala.dsl

import backtype.storm.topology.IRichBolt
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.tuple.{Fields, Tuple, Values}
import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import collection.JavaConversions._
import java.util.Map


class StormBolt(val outputFields: List[String]) extends IRichBolt {
    var _collector:OutputCollector = _
    var _context:TopologyContext = _
    var _conf:java.util.Map[_, _] = _

    type processFuncType = Tuple => List[java.lang.Object]
    var processFn:processFuncType = { t:Tuple => Nil }

    override def prepare(conf:java.util.Map[_, _], context:TopologyContext, collector:OutputCollector) = {
        _collector = collector
        _context = context
        _conf = conf
    }

    override def cleanup = {}

    override def execute(tuple: Tuple) = {
      _collector.emit(tuple, processFn(tuple))
      _collector.ack(tuple)
    }

    override def declareOutputFields(declarer:OutputFieldsDeclarer) = {
        declarer.declare(new Fields(outputFields));
    }

    def process(codeBlock: processFuncType) = { processFn = codeBlock }

    def ack(tuple: Tuple) = _collector.ack(tuple)
}
