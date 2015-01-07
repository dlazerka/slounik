package me.lazerka.slounik.gae.admin;

import com.google.common.base.Splitter;
import me.lazerka.slounik.gae.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.util.ArrayList;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Dzmitry Lazerka
 */
@Path("/admin/upload")
public class UploadResource {
	private static final Logger logger = LoggerFactory.getLogger(UploadResource.class);

	@POST
	@Consumes("text/plain")
	@Produces("text/plain")
	public String post(
			String data,
			@Context HttpServletRequest req
	) {
		List<String> lines = Splitter.on('\n').splitToList(data);
		logger.info("Received upload: {} chars, {} lines", data.length(), lines.size());
		int batchSize = 0;

		List<Entry> batch = new ArrayList<>(10000);
		for(String keyLine : lines) {
			try {
				batchSize += keyLine.length();
				List<String> split = Splitter
						.on(':') // original line may contain old values
						.limit(2)
						.splitToList(keyLine);
				String key = split.get(0);
				String line = split.get(1).replace('↵', '\n');

				Entry entry = new Entry(key, line);
				batch.add(entry);

				if (batchSize > 500000) {
					logger.trace("Saving {} entities", batch.size());
					ofy().save().entities(batch);
					batch.clear();
					batchSize = 0;
				}
			} catch (Exception e) {
				logger.error("At line {}", keyLine);
				throw e;
			}
		}

		if (!batch.isEmpty()) {
			logger.trace("Saving {} entities", batch.size());
			ofy().save().entities(batch).now();
		}

		logger.info("Saved {} lines", lines.size());
		return "Stored lines: " + lines.size();
	}
}
