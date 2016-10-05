package name.dlazerka.slounik.gae.rest;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import org.joda.time.DateTime;

/**
 * @author Dzmitry Lazerka
 */
@Entity
public class TaskDelay {
	@Id
	Long id;

	@Index
	long delay;

	DateTime scheduledAt;
	DateTime executedAt;

	String scheduledInstanceId;
	String executedInstanceId;

	String taskName;
	String taskEta;
	String taskExecutionCount;
	String taskRetryCount;

	public TaskDelay() {
	}
}
