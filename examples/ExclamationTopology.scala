package storm.scala

import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.testing.TestWordSpout
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}


object ExclamationTopology {

    class ExclamationBolt extends StormBolt(List("word")) {
      process { t:Tuple => List(t.getString(0) + "!!!") }
    }

    def main(args: Array[String]) = {
        val builder = new TopologyBuilder()

        builder.setSpout(1, new TestWordSpout(), 10)
        builder.setBolt(2, new ExclamationBolt(), 3)
                .shuffleGrouping(1)
        builder.setBolt(3, new ExclamationBolt(), 2)
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
