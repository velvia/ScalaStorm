// Copyright (c) 2011 Evan Chan

package storm.scala.dsl

import backtype.storm.tuple.{Fields, Tuple, Values}
import backtype.storm.task.TopologyContext
import backtype.storm.task.OutputCollector
import backtype.storm.tuple.MessageId
import collection.JavaConversions._
import collection.JavaConverters._


// A base class for the other DSL classes
abstract class BaseEmitDsl(val collector: OutputCollector) {
  var emitFunc: List[AnyRef] => Seq[java.lang.Integer] = collector.emit(_).asScala
  var emitDirectFunc: (Int, List[AnyRef]) => Unit = collector.emitDirect(_, _)

  // The emit function takes in a variable list of (arg1, arg2, ...) which looks
  // like a tuple!   Autoboxing is done.
  // It returns a Seq of java.lang.Integers.
  def emit(values: Any*) = emitFunc(values.toList.map { _.asInstanceOf[AnyRef] })

  // emitDirect is for emitting directly to a specific taskId.
  def emitDirect(taskId: Int, values: Any*) = emitDirectFunc(taskId,
    values.toList.map { _.asInstanceOf[AnyRef] })
}


// unanchored emit:
//    new UnanchoredEmit(collector) emit (val1, val2, ...)
// unanchored emit to a specific stream:
//    new UnanchoredEmit(collector) toStream <streamId> emit (va1, val2, ..)
class UnanchoredEmit(collector: OutputCollector) extends BaseEmitDsl(collector) {
  def toStream(streamId: String) = {
    emitFunc = collector.emit(streamId, _).asScala
    emitDirectFunc = collector.emitDirect(_, streamId, _)
    this
  }
}


// A class/DSL for emitting anchored on a single storm tuple, and acking a tuple.
// emit anchored on one StormTuple:
//    stormTuple emit (val1, val2, .. )
// emit anchored on one StormTuple for a stream:
//    stormTuple toStream <streamID> emit (val1, val2, ...)
class StormTuple(collector: OutputCollector, val tuple:Tuple)
  extends BaseEmitDsl(collector) {
  // Default emit function to one that takes in the tuple as the anchor
  emitFunc = collector.emit(tuple, _).asScala
  emitDirectFunc = collector.emitDirect(_, tuple, _)

  // stream method causes the emit to emit to a specific stream
  def toStream(streamId: String) = {
    emitFunc = collector.emit(streamId, tuple, _).asScala
    emitDirectFunc = collector.emitDirect(_, streamId, tuple, _)
    this
  }

  // Ack this tuple
  def ack = collector.ack(tuple)

  val lastResort: PartialFunction[Seq[Any], Unit] = {
      case _ => throw new RuntimeException("Unhandled tuple " + tuple)
    }

  // Use Scala pattern matching on Storm tuples!
  // Pass a partial function to this method with case Seq(..)
  // statements.  Scala will match up any primitives correctly
  // with their boxed java.lang.Object types in the tuple.
  // Anything not matched by the partial function will result
  // in an exception.
  def matchSeq(f: PartialFunction[Seq[Any], Unit]) = {
    val matchFunc = f orElse lastResort
    matchFunc(tuple.getValues.asScala: Seq[Any])
  }
}


// A class/DSL for emitting anchored on multiple tuples
//
// multi-anchored emit:
//    List(tuple1,tuple2) emit (val1, val2, ...)
class StormTupleList(collector: OutputCollector, val tuples: Seq[Tuple])
  extends BaseEmitDsl(collector) {

  emitFunc = collector.emit(tuples, _).asScala
  emitDirectFunc = collector.emitDirect(_, tuples, _)

  // There is no interface for emitting to a specific stream anchored on multiple tuples.

  // convenience func for acking a list of tuples
  def ack = tuples foreach { collector.ack }
}
