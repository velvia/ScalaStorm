package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.testing.TestWordSpout
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}


class ExclamationBolt extends StormBolt(outputFields = List("word")) {
  def execute(t: Tuple) {
    t emit(t.getString(0) + "!!!")
    t ack
  }
}

object ExclamationTopology {
    def main(args: Array[String]) = {
        val builder = new TopologyBuilder()

        builder.setSpout("words", new TestWordSpout(), 10)
        builder.setBolt("exclaim1", new ExclamationBolt, 3)
                .shuffleGrouping("words")
        builder.setBolt("exclaim2", new ExclamationBolt, 2)
                .shuffleGrouping("exclaim1")

        val conf = new Config()
        conf setDebug true

        val cluster = new LocalCluster()
        cluster.submitTopology("test", conf, builder.createTopology())
        Thread sleep 10000
        cluster.killTopology("test")
        cluster.shutdown()
    }
}
