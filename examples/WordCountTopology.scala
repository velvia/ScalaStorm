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