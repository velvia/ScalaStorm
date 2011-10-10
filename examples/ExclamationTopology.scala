package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.testing.TestWordSpout
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}


class ExclamationBolt extends StormBolt(outputFields = List("word")) {
  process { t => emit(t.getString(0) + "!!!") }
}

object ExclamationTopology {
    def main(args: Array[String]) = {
        val builder = new TopologyBuilder()

        builder.setSpout(1, new TestWordSpout(), 10)
        builder.setBolt(2, new ExclamationBolt, 3)
                .shuffleGrouping(1)
        builder.setBolt(3, new ExclamationBolt, 2)
                .shuffleGrouping(2)

        val conf = new Config()
        conf setDebug true

        val cluster = new LocalCluster()
        cluster.submitTopology("test", conf, builder.createTopology())
        Thread sleep 10000
        cluster.killTopology("test")
        cluster.shutdown()
    }
}
