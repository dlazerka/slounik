package name.dlazerka.slounik.gae.rest;

import com.google.appengine.api.modules.ModulesException;
import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskHandle;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.common.base.Stopwatch;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.ws.rs.*;
import java.util.List;

import static com.google.appengine.api.taskqueue.TaskOptions.Method.GET;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * @author Dzmitry Lazerka
 */
@Path("/queue")
@Produces("text/plain")
public class QueueResource {
	private static final Logger logger = LoggerFactory.getLogger(QueueResource.class);

	static Queue queue = QueueFactory.getQueue("test");
	static ModulesService modulesService = ModulesServiceFactory.getModulesService();

	@GET
	@Path("schedule")
	public String get() {
		long now = System.currentTimeMillis();

		TaskOptions taskOptions = TaskOptions.Builder
				.withUrl("/queue/execute")
				.param("scheduledAt", now + "")
				.method(GET);

		String instanceId = getInstanceId();
		if (instanceId != null) {
			taskOptions = taskOptions.param("instanceId", getInstanceId());
		}

		TaskHandle taskHandle = queue.add(taskOptions);

		logger.debug("Scheduled " + taskHandle.getName() + ", at: " + now + ", eta: " + taskHandle.getEtaMillis());

		return "ok";
	}

	@Nullable
	private String getInstanceId() {
		try {
			return modulesService.getCurrentInstanceId();
		} catch (ModulesException e) {
			return null;
		}
	}

	@GET
	@Path("execute")
	public String execute(
			@QueryParam("scheduledAt") String scheduledAt,
			@QueryParam("instanceId") @Nullable String instanceId,
	        @HeaderParam("X-AppEngine-TaskName") String taskName,
	        @HeaderParam("X-AppEngine-TaskETA") String taskEta,
	        @HeaderParam("X-AppEngine-TaskExecutionCount") String taskExecutionCount,
	        @HeaderParam("X-AppEngine-TaskRetryCount") String taskRetryCount
 	) {
		long now = System.currentTimeMillis();
		long scheduled = Long.parseLong(scheduledAt);
		long delay = now - scheduled;

		TaskDelay taskDelay = new TaskDelay();
		taskDelay.delay = delay;
		taskDelay.scheduledAt = new DateTime(scheduled);
		taskDelay.executedAt = new DateTime(now);
		taskDelay.scheduledInstanceId = instanceId;
		taskDelay.executedInstanceId = getInstanceId();

		taskDelay.taskName = taskName;
		taskDelay.taskEta = taskEta;
		taskDelay.taskExecutionCount = taskExecutionCount;
		taskDelay.taskRetryCount = taskRetryCount;

		ofy().save().entity(taskDelay).now();

		logger.debug("Executed at " + now + ", delay: " + delay + "ms");

		return "ok";
	}

	@GET
	@Path("/stat")
	public String stat() {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<TaskDelay> list = ofy().load().type(TaskDelay.class)
				.chunk(1000)
				.list();
		list.get(0);
		stopwatch.stop();

		Stopwatch stopwatch2 = Stopwatch.createStarted();
		DescriptiveStatistics stat = new DescriptiveStatistics();
		for (TaskDelay taskDelay : list) {
			stat.addValue(taskDelay.delay);
		}

		String response = getSummary(stat) +
				"\n fetched in: " + stopwatch.elapsed(MILLISECONDS) + "ms" +
				"\n computed in: " + stopwatch2.elapsed(MILLISECONDS) + "ms";
		logger.info(response);

		return response;
	}

	/**
	 * library("ggplot2")
	 > library("dplyr")
	 * dmp = tbl_dt(read.csv('dump', header = TRUE))
	 * ggplot(dmp) + aes(x=dmp$delay) + geom_histogram(bins = 200, origin = 1) +
	 *      scale_x_log10(breaks=c(20, 30, 50, 75, 100, 150, 200, 500, 1000, 1700))
	 */
	@GET
	@Path("/dump")
	@Produces("text/tsv")
	public String dump() {
		Stopwatch stopwatch = Stopwatch.createStarted();
		List<TaskDelay> list = ofy().load().type(TaskDelay.class)
				.chunk(1000)
				.list();
		list.get(0);
		stopwatch.stop();

		StringBuilder sb = new StringBuilder();
		sb.append("delay\n");
		Stopwatch stopwatch2 = Stopwatch.createStarted();
		for (TaskDelay taskDelay : list) {
			sb.append(taskDelay.delay).append('\n');
		}
		String response = sb.toString();

		logger.info("Fetched {}rows in: {}ms, computed in: {}ms",
				list.size(), stopwatch.elapsed(MILLISECONDS), stopwatch2.elapsed(MILLISECONDS));

		return response;
	}

	private String getSummary(DescriptiveStatistics stat) {
		return " n: " + (long) Math.round(stat.getN()) +
		"\n min: "   + Math.round(stat.getMin()) + "ms " +
		"\n max: "   + Math.round(stat.getMax()) + "ms " +
		"\n mean: "  + Math.round(stat.getMean()) + "ms " +
		"\n var: "   + Math.round(stat.getVariance()) +
		"\n p50: "   + Math.round(stat.getPercentile(50)) + "ms " +
		"\n p90: "   + Math.round(stat.getPercentile(90)) + "ms " +
		"\n p95: "   + Math.round(stat.getPercentile(95)) + "ms " +
		"\n p98: "   + Math.round(stat.getPercentile(98)) + "ms " +
		"\n p99: "   + Math.round(stat.getPercentile(99)) + "ms " +
		"\n p99.9: " + Math.round(stat.getPercentile(99.9)) + "ms "  +
		"\n instanceId: " + getInstanceId();
	}
}
