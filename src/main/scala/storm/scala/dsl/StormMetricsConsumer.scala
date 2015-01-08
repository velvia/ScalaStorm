package storm.scala.dsl

import java.util.{Map => JMap, Collection => JCollection}

import backtype.storm.metric.api.IMetricsConsumer
import backtype.storm.metric.api.IMetricsConsumer.TaskInfo
import backtype.storm.metric.api.IMetricsConsumer.DataPoint
import backtype.storm.task.{IErrorReporter, TopologyContext}
import collection.JavaConverters._

/**
 * Wrapper around the IMetricsConsumer of Storm
 * Classes implementing this base class need to implement the processDataPoints method
 * i.s.o. the default IMetricsConsumer::handleDataPoints method in order to process metric tuples.
 * This is done because a list of interested metric keys can be given at construction time so that only
 * these metrics will be provided in the overridden processDataPoints method.
 *
 * USAGE :
 *
 * class MyMetricConsumer extends StormMetricsConsumer {
 *  setup {
 *      // all initialization should be done here
 *      // available base properties :
 *      //  _conf           : full storm configuration
 *      //  _argConfig      : metric consumer specific config (as specified in the topology.metrics.consumer.register configuration property)
 *      //  _context        : TopologyContext
 *      //  _errorReporter  : IErrorReporter
 *  }
 *
 *  shutdown {
 *      // all resource cleanup should be here
 *      // available base properties :
 *      //  _conf           : full storm configuration
 *      //  _argConfig      : metric consumer specific config (as specified in the topology.metrics.consumer.register configuration property)
 *      //  _context        : TopologyContext
 *      //  _errorReporter  : IErrorReporter
 *  }
 *
 *  override protected def processDataPoints(taskInfo: TaskInfo, dataPoints: List[DataPoint]): Unit = {
 *      //do something with the delivered data points
 *      //e.g. forward them to your metric system (like ganglia)
 *  }
 * }
 */
abstract class StormMetricsConsumer(val allowedMetrics : List[String]) extends IMetricsConsumer with SetupFunc with ShutdownFunc {

    protected var _conf: JMap[_,_] = _
    protected var _argConfig: Map[String,_] = _
    protected var _context: TopologyContext = _
    protected var _errorReporter : IErrorReporter = _

    /** default constructor
      * empty metric list, thus meaning that you're interested in all delivered metrices */
    def this() = { this(List()) }

    /**
     * Initialize the metric consumer and call its registered setup functions
     *
     * @param conf the storm configuration
     * @param registrationArgument metric plugin specific config as defined in the config file
     * @param context   the topology context
     * @param errorReporter to report back possible exceptions
     */
    final override def prepare(conf: JMap[_, _], registrationArgument: AnyRef, context: TopologyContext, errorReporter: IErrorReporter) : Unit = {
        _context = context
        _errorReporter = errorReporter
        _conf = conf
        _argConfig = registrationArgument.asInstanceOf[JMap[String,_]].asScala.toMap
        _setup()
    }

    /**
     * Derived classes should use the processDataPoints method
     *
     * @param taskInfo  taskInfo of the task that originated the metric tuples
     * @param dataPoints    the list of received data points
     */
    final override def handleDataPoints(taskInfo: TaskInfo, dataPoints: JCollection[DataPoint]) : Unit = {
        val dps = dataPoints.asScala.toList

        if(allowedMetrics.isEmpty) processDataPoints( taskInfo, dps )
        else processDataPoints( taskInfo, dps.filter{ dp => allowedMetrics.contains (dp.name) } )
    }

    /**
     * execute the registered cleanup functions
     */
    final override def cleanup() = _cleanup()

    /**
     * Needs to be implemented in order to be able to process the data points.
     * The passed list of data points is filtered when a list of interested data points
     * was given at construction time.
     *
     * @param taskInfo taskInfo of the task that originated the data points
     * @param dataPoints the list of received data points
     */
    protected def processDataPoints(taskInfo: TaskInfo, dataPoints : List[DataPoint]) : Unit
}
