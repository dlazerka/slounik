package me.lazerka.slounik.gae.rest;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.SimpleQuery;
import me.lazerka.slounik.gae.Entry;
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
@Path("/rest/entry")
public class EntryResource {
	private static final Logger logger = LoggerFactory.getLogger(EntryResource.class);

	private static final int MAX_QUERY_LENGTH = 100;
	private static final int MAX_RESULTS = 100;

	@GET
	@Path("search/{lang1:[a-z][a-z]}/{lang2:[a-z][a-z]}/{query}")
	@Produces("application/json")
	public List<Entry> list(
			@PathParam("lang1") String lang1,
			@PathParam("lang2") String lang2,
			@PathParam("query") String query
	) {
		validateQuery(query);

		List<Key<Entry>> keys = fetch(query, lang1, lang2);

		logger.trace("Found {} results", keys.size());

		List<Entry> result = new ArrayList<>(keys.size());
		for(Key<Entry> entryKey : keys) {
			result.add(new Entry(entryKey));
		}
		return result;
	}

	private void validateQuery(String query) {
		if (query == null) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST)
							.entity("Query is null")
							.build());
		}
		if (query.length() > MAX_QUERY_LENGTH) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST)
							.entity("Query max length is " + MAX_QUERY_LENGTH)
							.build());
		}
	}

	private List<Key<Entry>> fetch(String query, String lang1, String lang2) {
		LoadType<Entry> type = ofy()
				.load()
				.type(Entry.class);
		SimpleQuery<Entry> simpleQuery;
		try {
			if (query.endsWith(" ")) {
				query = query.substring(0, query.length() - 1);
				simpleQuery = Entry.addFullMatchFilter(lang1, lang2, query, type);
			} else {
				simpleQuery = Entry.addPrefixMatchFilter(lang1, lang2, query, type);
			}
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
					.entity("Unacceptable query")
					.build());
		}
		return simpleQuery
				.limit(MAX_RESULTS)
				.chunkAll()
				.keys()
				.list();
	}
}
