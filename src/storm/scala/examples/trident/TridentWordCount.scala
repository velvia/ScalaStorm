package storm.scala.examples.trident

import collection.JavaConversions._

import storm.trident.tuple.TridentTuple
import storm.trident.operation.{TridentCollector, BaseFunction}
import backtype.storm.tuple.{Fields, Values}
import storm.trident.TridentTopology
import storm.trident.testing.{MemoryMapState, FixedBatchSpout}
import backtype.storm.{StormSubmitter, LocalDRPC, LocalCluster, Config}
import storm.trident.operation.builtin._
import storm.trident.state.{State, QueryFunction}

object TridentWordCount extends App {
  class Split extends BaseFunction {
    def execute(tuple: TridentTuple, collector: TridentCollector) {
      tuple getString 0 split " " foreach { word => collector.emit(List(word)) }
    }
  }

  def buildTopology(drpc: LocalDRPC) = {
    val spout = new FixedBatchSpout(new Fields("sentence"), 3,
                new Values("the cow jumped over the moon"),
                new Values("the man went to the store and bought some candy"),
                new Values("four score and seven years ago"),
                new Values("how many apples can you eat"),
                new Values("to be or not to be the person"));
    spout.setCycle(true);

    val topology = new TridentTopology();
    val wordCounts = topology.newStream("spout1", spout)
      .parallelismHint(16)
      .each(new Fields("sentence"), new Split(), new Fields("word"))   // flatmap
      .persistentAggregate(new MemoryMapState.Factory(), new Count(), new Fields("count"))

    topology.newDRPCStream("words", drpc)
        .each(new Fields("args"), new Split(), new Fields("word"))
        .groupBy(new Fields("word"))
        .stateQuery(wordCounts, new Fields("word"),
                    // Unfortunately asInstanceOf[] needed to get around an apparent scalac bug
                    (new MapGet).asInstanceOf[QueryFunction[_ <: State, _]], new Fields("count"))
        .each(new Fields("count"), new FilterNull())
        .aggregate(new Fields("count"), new Sum(), new Fields("sum"))

    topology.build()
  }

  val conf = new Config
  conf.setMaxSpoutPending(20)
  if (args.length == 0) {
    val drpc = new LocalDRPC
    val cluster = new LocalCluster
    cluster.submitTopology("wordCounter", conf, buildTopology(drpc))
    (0 to 100).foreach { i =>
      println("DRPC RESULT: " + drpc.execute("words", "cat the dog jumped"))
      Thread sleep 1000
    }
  } else {
    conf.setNumWorkers(3)
    StormSubmitter.submitTopology(args(0), conf, buildTopology(null));
  }
}
