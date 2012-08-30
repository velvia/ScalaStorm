package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.topology.TopologyBuilder
import collection.mutable.{ListBuffer, HashMap}
import util.Random
import backtype.storm.tuple.{Fields, Tuple}

/*
 * This is an example of streaming aggregation for different web metrics.
 * URLs are coming in with different geo information and devicetypes.
 *
 * It shows off the use of different streams, autoboxing and unboxing
 * in both spouts and bolts, and the use of a clock spout for controlling
 * aggregation.  Also, it demonstrates how to do acking of multiple tuples,
 * and emitting with a message ID to get failure detection.
 */

class WebRequestSpout extends StormSpout(outputFields = List("url", "city", "browser")) {
  val Urls = List("http://www.apple.com/specials", "http://itunes.apple.com/Xk#$kX",
                  "http://hockey.espn.com", "http://oaklandathletics.com", "http://www.espn3.com/today/",
                  "http://www.homedepot.com/Kitchen/2012/02/11")
  val Cities = List("San Jose", "San Francisco", "New York", "Dallas", "Chicago", "Chattanooga")
  val Browsers = List("IE7", "IE8", "IE9", "Firefox4", "Chrome", "Safari", "Mobile Safari", "Android Browser")

  def nextTuple {
    Thread sleep 100
    // Emitting with a unique message ID allows Storm to detect failure and resend tuples
    using msgId(Random.nextInt) emit (Urls(Random.nextInt(Urls.length)),
                                      Cities(Random.nextInt(Cities.length)),
                                      Browsers(Random.nextInt(Browsers.length)) )
  }
}

// This controls the interval at which aggregation buckets roll over
class ClockSpout extends StormSpout(outputFields = List("timestamp")) {
  def nextTuple {
    Thread sleep 5000
    emit (System.currentTimeMillis / 1000)
  }
}


// Split original stream into three
class Splitter extends StormBolt(Map("city" -> List("city"), "browser" -> List("browser"))) {
  def execute(t: Tuple) {
    t matchSeq {
      case Seq(url: String, city: String, browser: String) =>
        using anchor t toStream "city" emit (city)
        using anchor t toStream "browser" emit (browser)
        t ack
    }
  }
}


class FieldAggregator(val fieldName: String) extends StormBolt(outputFields = List(fieldName, "count")) {
  var tuples: ListBuffer[Tuple] = _
  var fieldCounts: collection.mutable.Map[String, Int] = _
  setup {
    tuples = new ListBuffer[Tuple]
    fieldCounts = new HashMap[String, Int].withDefaultValue(0)
  }

  // Only ack when the interval is complete, and ack/anchor all tuples
  def execute(t: Tuple) {
    t matchSeq {
      case Seq(field: String) =>
        fieldCounts(field) += 1
        tuples += t
      case Seq(clockTime: Long) =>
        fieldCounts foreach { case(field, count) =>
          tuples emit (field, count)
        }
        tuples.ack
        fieldCounts.clear
        tuples.clear
    }
  }
}

class CityAggregator extends FieldAggregator("city")
class BrowserAggregator extends FieldAggregator("browser")


object AggregationTopology {
  def main(args: Array[String]) = {
    val builder = new TopologyBuilder

    builder.setSpout("webrequest", new WebRequestSpout, 3)
    builder.setSpout("clock", new ClockSpout)

    builder.setBolt("splitter", new Splitter, 3)
      .shuffleGrouping("webrequest")
    builder.setBolt("city_aggregator", new CityAggregator, 5)
      .fieldsGrouping("splitter", "city", new Fields("city"))
      .allGrouping("clock")
    builder.setBolt("browser_aggregator", new BrowserAggregator, 5)
      .fieldsGrouping("splitter", "browser", new Fields("browser"))
      .allGrouping("clock")

    val conf = new Config
    conf.setDebug(true)
    conf.setMaxTaskParallelism(3)

    val cluster = new LocalCluster
    cluster.submitTopology("webaggregation", conf, builder.createTopology)
    Thread sleep 10000
    cluster.shutdown
  }
}
