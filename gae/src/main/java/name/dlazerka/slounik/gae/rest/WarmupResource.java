package name.dlazerka.slounik.gae.rest;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.SimpleQuery;
import name.dlazerka.slounik.gae.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Dzmitry Lazerka
 */
@Path("/_ah/warmup")
public class WarmupResource {
	private static final Logger logger = LoggerFactory.getLogger(WarmupResource.class);

	@GET
	public String get() {
		logger.info("Warmed up");
		return "ok";
	}
}
