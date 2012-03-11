package storm.scala.examples

import storm.scala.dsl._
import backtype.storm.Config
import backtype.storm.LocalCluster
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.{Fields, Tuple, Values}
import collection.mutable.{ListBuffer, HashMap}
import util.Random

/*
 * This is an example of streaming aggregation for different web metrics.
 * URLs are coming in with different geo information and devicetype
 *
 * It shows off the use of different streams, autoboxing and unboxing
 * in both spouts and bolts, and the use of a clock spout for streaming
 * buckets.  Also, it demonstrates how to do acking of multiple tuples.
 */

class WebRequestSpout extends StormSpout(outputFields = List("url", "city", "browser")) {
  val Urls = List("http://www.apple.com/specials", "http://itunes.apple.com/Xk#$kX",
                  "http://hockey.espn.com", "http://oaklandathletics.com", "http://www.espn3.com/today/",
                  "http://www.homedepot.com/Kitchen/2012/02/11")
  val Cities = List("San Jose", "San Francisco", "New York", "Dallas", "Chicago", "Chattanooga")
  val Browsers = List("IE7", "IE8", "IE9", "Firefox4", "Chrome", "Safari", "Mobile Safari", "Android Browser")

  def nextTuple {
    Thread sleep 100
    emit (Urls(Random.nextInt(Urls.length)), Cities(Random.nextInt(Cities.length)),
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

class CityAggregator extends StormBolt(outputFields = List("city", "count")) {
  var tuples: ListBuffer[Tuple] = _
  var cityCounts: HashMap[String, Int] = _
  setup {
    tuples = new ListBuffer[Tuple]
    cityCounts = new HashMap[String, Int]
  }
  def execute(t: Tuple) {
    t matchSeq {
      case Seq(clockTime: Long) =>
        cityCounts foreach { case(city, count) =>
          tuples emit (city, count)
        }
    }
  }
}

class BrowserAggregator extends StormBolt(outputFields = List("browser", "count")) {
  def execute(t: Tuple) {}
}