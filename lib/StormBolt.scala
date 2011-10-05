package storm.scala

import backtype.storm.topology.IRichBolt
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.tuple.{Fields, Tuple, Values}
import backtype.storm.task.OutputCollector
import backtype.storm.task.TopologyContext
import collection.JavaConversions._
import java.util.Map


class StormBolt(val outputFields: List[String]) extends IRichBolt {
    var _collector:OutputCollector = null
    var _context:TopologyContext = null
    var _conf:java.util.Map[_, _] = null

    override def prepare(conf:java.util.Map[_, _], context:TopologyContext, collector:OutputCollector) = {
        _collector = collector
        _context = context
        _conf = conf
    }

    override def cleanup = {}

    override def execute(tuple: Tuple) = {}

    override def declareOutputFields(declarer:OutputFieldsDeclarer) = {
        declarer.declare(new Fields(outputFields));
    }

    def ack(tuple: Tuple) = _collector.ack(tuple)
}
