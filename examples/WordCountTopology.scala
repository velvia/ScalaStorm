package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}
import collection.mutable.HashMap

class SplitSentence extends StormBolt(outputFields = List("word")) {
  process { t => t.getString(0) split " " foreach { word => emit(word) } }
}

class WordCount extends StormBolt(outputFields = List("word", "count")) {
  // NOTE: withDefaultValue is new to 2.9.1.  If using <= 2.9.0.x, you need to
  // replace it with { override def default(key:String) = 0 }
  val counts = new HashMap[String, Int]().withDefaultValue(0)
  process { t =>
    val word = t.getString(0)
    counts(word) += 1
    emit(word, counts(word): java.lang.Integer)
  }
}


object WordCountTopology {
  def main(args: Array[String]) = {
    val builder = new TopologyBuilder
  }
}