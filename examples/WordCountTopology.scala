package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}
import collection.mutable.{Map, HashMap}
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


// An example of using matchSeq for Scala pattern matching of Storm tuples
// plus using the emit and ack DSLs.
class SplitSentence extends StormBolt(outputFields = List("word")) {
  def execute(t: Tuple) = t matchSeq {
    case Seq(sentence: String) => sentence split " " foreach
      { word => using anchor t emit (word) }
    t ack
  }
}


class WordCount extends StormBolt(List("word", "count")) {
  var counts: Map[String, Int] = _
  setup {
    counts = new HashMap[String, Int]().withDefaultValue(0)
  }
  def execute(t: Tuple) = t matchSeq {
    case Seq(word: String) =>
      counts(word) += 1
      using anchor t emit (word, counts(word))
      t ack
  }
}


object WordCountTopology {
  def main(args: Array[String]) = {
    val builder = new TopologyBuilder

    builder.setSpout("randsentence", new RandomSentenceSpout, 5)
    builder.setBolt("split", new SplitSentence, 8)
        .shuffleGrouping("randsentence")
    builder.setBolt("count", new WordCount, 12)
        .fieldsGrouping("split", new Fields("word"))

    val conf = new Config
    conf.setDebug(true)
    conf.setMaxTaskParallelism(3)

    val cluster = new LocalCluster
    cluster.submitTopology("word-count", conf, builder.createTopology)
    Thread sleep 10000
    cluster.shutdown
  }
}