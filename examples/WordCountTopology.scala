package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}
import collection.mutable.HashMap
import util.Random


class RandomSentenceSpout extends StormSpout(outputFields = List("sentence")) {
  val sentences = List("the cow jumped over the moon",
                       "an apple a day keeps the doctor away",
                       "four score and seven years ago",
                       "snow white and the seven dwarfs",
                       "i am at two with nature")
  def nextTuple = {
    Thread sleep 100
    emit (sentences(Random.nextInt(sentences.length)))
  }
}


class SplitSentence extends StormBolt(outputFields = List("word")) {
  def execute(t: Tuple) {
    t.getString(0) split " " foreach
      { word => using anchor t emit (word) }
    t ack
  }
}


class WordCount extends StormBolt(outputFields = List("word", "count")) {
  // NOTE: Scala 2.9.1 has a withDefaultValue method, but it is not serializable
  // so can't be used with Storm.  :(
  val counts = new HashMap[String, Int]() { override def default(key:String) = 0 }
  def execute(t: Tuple) {
    val word = t.getString(0)
    counts(word) += 1
    using anchor t emit (word, counts(word): java.lang.Integer)
    t ack
  }
}


object WordCountTopology {
  def main(args: Array[String]) = {
    val builder = new TopologyBuilder

    builder.setSpout(1, new RandomSentenceSpout, 5)
    builder.setBolt(2, new SplitSentence, 8)
        .shuffleGrouping(1)
    builder.setBolt(3, new WordCount, 12)
        .fieldsGrouping(2, new Fields("word"))

    val conf = new Config
    conf.setDebug(true)
    conf.setMaxTaskParallelism(3)

    val cluster = new LocalCluster
    cluster.submitTopology("word-count", conf, builder.createTopology)
    Thread sleep 10000
    cluster.shutdown
  }
}