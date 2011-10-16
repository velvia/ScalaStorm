package storm.scala.dsl

import backtype.storm.tuple.{Fields, Tuple, Values}
import backtype.storm.task.TopologyContext
import backtype.storm.task.OutputCollector
import backtype.storm.tuple.MessageId
import collection.JavaConversions._


// A class to extend storm's Tuple to enable a DSL for emitting tuples.
// emit anchored on one StormTuple:
//    stormTuple emit (val1, val2, .. )
// emit anchored on one StormTuple for a stream:
//    stormTuple stream <streamID> emit (val1, val2, ...)
class StormTuple(val collector:OutputCollector, val tuple:Tuple) {
  // Default emit function to one that takes in the tuple as the anchor
  var emitFunc: List[AnyRef] => java.util.List[java.lang.Integer] = collector.emit(tuple, _)

  // stream method causes the emit to emit to a specific stream
  def stream(streamId: Int) = {
    emitFunc = collector.emit(streamId, tuple, _)
    this
  }

  // The emit function takes in a variable list of (arg1, arg2, ...) which looks
  // like a tuple!
  // The args are translated to a list for efficiency.
  def emit(values: AnyRef*) = emitFunc(values.toList)

  // Ack this tuple
  def ack = collector.ack(tuple)
}
