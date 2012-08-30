// Copyright (c) 2011 Evan Chan

package storm.scala.dsl

import java.util.Map
import backtype.storm.task.TopologyContext
import backtype.storm.spout.SpoutOutputCollector
import backtype.storm.topology.OutputFieldsDeclarer
import backtype.storm.topology.base.BaseRichSpout
import backtype.storm.tuple.Fields
import collection.JavaConverters._
import collection.JavaConversions._

abstract class StormSpout(val outputFields: List[String],
                          val isDistributed: Boolean = false) extends BaseRichSpout with SetupFunc {
  var _context:TopologyContext = _
  var _collector:SpoutOutputCollector = _

  def open(conf: Map[_, _], context: TopologyContext, collector: SpoutOutputCollector) = {
    _context = context
    _collector = collector
    _setup()
  }

  // nextTuple needs to be defined by each spout inheriting from here
  //def nextTuple() {}

  def declareOutputFields(declarer: OutputFieldsDeclarer) =
    declarer.declare(new Fields(outputFields))

  // DSL for emit and emitDirect.
  // [toStream(<streamId>)] emit (val1, val2, ..)
  // [using][msgId <messageId>] [toStream <streamId>] emit (val1, val2, ...)
  // [using][msgId <messageId>] [toStream <streamId>] emitDirect (taskId, val1, val2, ...)
  def using = this

  def msgId(messageId: Any) = new MessageIdEmitter(_collector, messageId.asInstanceOf[AnyRef])

  def toStream(streamId: String) = new StreamEmitter(_collector, streamId)

  // Autoboxing is done for both emit and emitDirect
  def emit(values: Any*) = _collector.emit(values.toList.map { _.asInstanceOf[AnyRef] })

  def emitDirect(taskId: Int, values: Any*) = _collector.emitDirect(taskId,
    values.toList.map { _.asInstanceOf[AnyRef] })
}


class StreamEmitter(collector: SpoutOutputCollector, streamId: String) {
  def emit(values: AnyRef*) = collector.emit(streamId, values.toList)

  def emitDirect(taskId: Int, values: AnyRef*) = collector.emitDirect(taskId, streamId, values.toList)
}


class MessageIdEmitter(collector: SpoutOutputCollector, msgId: AnyRef) {
  var emitFunc: List[AnyRef] => Seq[java.lang.Integer] = collector.emit(_, msgId).asScala
  var emitDirectFunc: (Int, List[AnyRef]) => Unit = collector.emitDirect(_, _, msgId)

  def toStream(streamId: String) = {
    emitFunc = collector.emit(streamId, _, msgId)
    emitDirectFunc = collector.emitDirect(_, streamId, _, msgId)
    this
  }

  def emit(values: AnyRef*) = emitFunc(values.toList)

  def emitDirect(taskId: Int, values: AnyRef*) = emitDirectFunc(taskId, values.toList)
}
