package storm.scala.dsl

import collection.JavaConversions._

import storm.trident.tuple.TridentTuple
import storm.trident.operation.{TridentCollector, BaseFunction}
import backtype.storm.tuple.Fields
import scala.language.implicitConversions

/**
 * Functional DSL for Trident so you can easily use Scala closures with Trident.
 */
object FunctionalTrident {
  class MapFuncT1(func: TridentTuple => Any) extends BaseFunction {
    def execute(tuple: TridentTuple, collector: TridentCollector) {
      collector.emit(List(func(tuple).asInstanceOf[AnyRef]))
    }
  }

  class FlatMapFuncT1(func: TridentTuple => Seq[Any]) extends BaseFunction {
    def execute(tuple: TridentTuple, collector: TridentCollector) {
      func(tuple).foreach { thing => collector.emit(List(thing.asInstanceOf[AnyRef])) }
    }
  }

  class FunctionalStream(origStream: storm.trident.Stream) {
    // Example usage:
    // stream.map("sentence" -> "numwords") { _.getString(0).split(" ").length }
    def map(fieldMapping: (String, String))(mapFunc: TridentTuple => Any) =
      origStream.each(new Fields(fieldMapping._1),
                      new MapFuncT1(mapFunc),
                      new Fields(fieldMapping._2))

    // Example usage:
    // stream.flatMap("sentence" -> "words") { _.getString(0).split(" ") }
    def flatMap(fieldMapping: (String, String))(mapFunc: TridentTuple => Seq[Any]) =
      origStream.each(new Fields(fieldMapping._1),
                      new FlatMapFuncT1(mapFunc),
                      new Fields(fieldMapping._2))
  }

  implicit def TridentStreamToFunctionalStream(stream: storm.trident.Stream) =
    new FunctionalStream(stream)
}
