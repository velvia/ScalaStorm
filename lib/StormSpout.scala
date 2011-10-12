package storm.scala.dsl

import java.util.Map
import backtype.storm.task.TopologyContext
import backtype.storm.spout.SpoutOutputCollector
import backtype.storm.topology.{OutputFieldsDeclarer, IRichSpout}
import backtype.storm.tuple.Fields
import collection.JavaConversions._


abstract class StormSpout(val outputFields: List[String],
                          val isDistributed: Boolean = false) extends IRichSpout {
  var _context:TopologyContext = _
  var _collector:SpoutOutputCollector = _

  def open(conf: Map[_, _], context: TopologyContext, collector: SpoutOutputCollector) = {
    _context = context
    _collector = collector
  }

  def close() = {}

  // nextTuple needs to be defined by each spout inheriting from here
  //def nextTuple() {}

  def declareOutputFields(declarer: OutputFieldsDeclarer) =
    declarer.declare(new Fields(outputFields))

  def ack(tuple: AnyRef) = {}

  def fail(tuple: AnyRef) = {}

  // Emits the arguments passed as a tuple.
  // Since this is a spout, anchoring is not needed.
  def emit(values: AnyRef*) = _collector.emit(values.toList)
}