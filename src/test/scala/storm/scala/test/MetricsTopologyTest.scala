package storm.scala.test

import java.util.{Map => JMap}

import backtype.storm.metric.api.IMetricsConsumer.{DataPoint, TaskInfo}
import backtype.storm.{LocalCluster, Config}
import backtype.storm.metric.api.CountMetric
import backtype.storm.topology.TopologyBuilder
import backtype.storm.tuple.Tuple
import org.scalatest._
import storm.scala.dsl.{StormMetricsConsumer, StormBolt}
import collection.JavaConverters._

/**
 * registers the metric processor to only handle the 'metric:1' metric
 */
class TestMetricsConsumer extends StormMetricsConsumer(allowedMetrics = Set("metric:1")) with ShouldMatchers {
  var count = 0

  setup {
    _argConfig should be (None)  // we did not provide any arguments for this MetricsConsumer
    _conf should not be null
    _context should not be null
    _errorReporter should not be null
  }

  /** process the metric */
  override def processDataPoints(taskInfo: TaskInfo, dataPoints: Set[DataPoint]): Unit = {
    dataPoints should not be 'empty

    // 'metric:2' should not be received here as we did not put it in our allowedMetrics
    dataPoints.foreach (_.name should be ("metric:1"))
    count += 1
  }

  shutdown {
    // we should have received some metrics
    count should be > 0
  }
}

/**
 * registers the metric processor to handle all metrics
 */
class TestMetricsConsumerWithConfig extends StormMetricsConsumer() with ShouldMatchers {
  var count = 0

  setup {
    _argConfig should be ('defined)
    _argConfig.get should (contain key "key" and contain value "value")

    _conf should not be null
    _context should not be null
    _errorReporter should not be null
  }

  /** process the metric */
  override def processDataPoints(taskInfo: TaskInfo, dataPoints: Set[DataPoint]): Unit = {
    dataPoints should not be 'empty

    // all metrics should be received here as we put nothing in our allowedMetrics list
    dataPoints.foreach (dp => Set("metric:1", "metric:2") should contain (dp.name))
    count += 1
  }

  shutdown {
    // we should have received some metrics
    count should be > 0
  }
}

/**
 * a test bolt that just increments the 'metric:1' and 'metric:2' metric on each tick tuple received
 */
class TestBolt extends StormBolt(outputFields = List()) {
  var metric1 : CountMetric = _
  var metric2 : CountMetric = _

  setup {
    metric1 = new CountMetric()
    metric2 = new CountMetric()
    _context.registerMetric("metric:1", metric1, 1)
    _context.registerMetric("metric:2", metric2, 1)
  }

  override def execute(t: Tuple): Unit = {
    metric1.incr()
    metric2.incr()
    t.ack
  }

  override def getComponentConfiguration : JMap[String, Object] = {
    val interval : Int = 1
    Map(Config.TOPOLOGY_TICK_TUPLE_FREQ_SECS -> interval.asInstanceOf[Object]).asJava
  }
}

/**
 *
 */
class MetricsTopologyTest extends FeatureSpec with GivenWhenThen with ShouldMatchers {
  feature("Metrics assigned within a topology should be processed by the wrapped StormMetricsConsumer DSL") {
    scenario("Start topology with registered metric and check if it gets offered to the metric implementation") {
      Given("A test topology with metric 'metric:1' and 'metric:2' registered, and running on ticks")
        val builder = new TopologyBuilder()
        builder.setBolt("tick", new TestBolt, 1)

        val conf = new Config()
        conf setDebug true
        conf registerMetricsConsumer(classOf[TestMetricsConsumer], 1)
        conf registerMetricsConsumer(classOf[TestMetricsConsumerWithConfig], Map("key" -> "value").asJava, 1)

        val cluster = new LocalCluster()
        cluster.submitTopology("MetricsTopologyTest", conf, builder.createTopology())

      When("a tick is seen by the bolt")
      Then("only 'metric:1' is offered to the registered TestMetricsConsumer")
      And("all metrics are offered to the registered TestMetricsConsumerWithConfig")

        Thread sleep 5000
        cluster.killTopology("MetricsTopologyTest")
        Thread sleep 1000
        cluster.shutdown()
    }
  }
}
