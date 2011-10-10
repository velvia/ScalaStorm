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
  val counts = new HashMap[String, Int]() { override def default(key:String) = 0 }
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