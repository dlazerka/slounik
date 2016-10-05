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
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import static com.google.appengine.api.taskqueue.TaskOptions.Method.GET;

/**
 * @author Dzmitry Lazerka
 */
@Path("/queue")
public class QueueResource {
	private static final Logger logger = LoggerFactory.getLogger(QueueResource.class);

	static Queue queue = QueueFactory.getQueue("test");
	static ModulesService modulesService = ModulesServiceFactory.getModulesService();

	static DescriptiveStatistics stat = new DescriptiveStatistics();


	@GET
	@Path("schedule")
	public String get() {
		long now = System.currentTimeMillis();

		TaskOptions taskOptions = TaskOptions.Builder
				.withUrl("/queue/execute")
				.param("scheduledAt", now + "")
				.param("instanceId", getInstanceId())
				.method(GET);

		TaskHandle taskHandle = queue.add(taskOptions);

		logger.info("Scheduled " + taskHandle.getName() + ", at: " + now + ", eta: " + taskHandle.getEtaMillis());

		return "ok";
	}

	private String getInstanceId() {
		String instanceId = "unavailable";
		try {
			instanceId = modulesService.getCurrentInstanceId();
		} catch (ModulesException e) {
			// ok
		}
		return instanceId;
	}

	@GET
	@Path("execute")
	public String execute(
			@QueryParam("scheduledAt") String scheduledAt,
			@QueryParam("instanceId") String instanceId
	) {
		long now = System.currentTimeMillis();
		long scheduled = Long.parseLong(scheduledAt);
		long after = now - scheduled;

		stat.addValue(after);

		long mean = Math.round(stat.getMean());
		long min = Math.round(stat.getMin());
		long max = Math.round(stat.getMax());
		long n = Math.round(stat.getN());
		long var = Math.round(stat.getVariance());

		logger.info("Executed at " + now +
				", instanceId: " + getInstanceId()+
				", after: " + after + "ms" +
				", min: " + min +
				", max: " + max +
				", n: " + n +
				", mean: " + mean +
				", var: " + var +
				", p50: " + stat.getPercentile(0.5) +
				", p90: " + stat.getPercentile(0.90) +
				", p95: " + stat.getPercentile(0.95) +
				", p98: " + stat.getPercentile(0.98) +
				", p99: " + stat.getPercentile(0.99) +
				", p99.9: " + stat.getPercentile(0.999)
		);

		return "ok";
	}
}
