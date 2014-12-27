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
	@Path("search/{query}")
	@Produces("application/json")
	public List<EntryBean> list(@PathParam("query") String query) {
		validateQuery(query);

		List<Key<Entry>> ruKeys = fetch(query, Lang.RU);
		List<Key<Entry>> beKeys = fetch(query, Lang.BE);

		int size = ruKeys.size() + beKeys.size();
		logger.trace("Found {} results: {} ruKeys, {} beKeys", size, ruKeys.size(), beKeys.size());

		List<EntryBean> result = new ArrayList<>(size);
		for(Key<Entry> entryKey : ruKeys) {
			Entry entry = new Entry(entryKey);
			result.add(new EntryBean(
					entry.getLemmaFrom(),
					entry.getLemmaTo(),
					entry.getLangFrom(),
					entry.getLangTo(),
					entry.getDictionaryName()
			));
		}
		result.add(new EntryBean(
				"sdf",
				"fdg",
				Lang.BE,
				Lang.RU,
				"sdfg"
		));
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
		if (query.indexOf(Entry.ARROWS) != -1) {
			throw new WebApplicationException(
					Response.status(Status.BAD_REQUEST)
							.entity("Query may not contain " + Entry.ARROWS)
							.build());
		}
	}

	private List<Key<Entry>> fetch(String query, Lang from) {
		LoadType<Entry> type = ofy()
				.load()
				.type(Entry.class);
		SimpleQuery<Entry> simpleQuery;
		if (query.endsWith(" ")) {
			query = query.substring(0, query.length() - 1);
			simpleQuery = Entry.addFullMatchFilter(query, from, type);
		} else {
			simpleQuery = Entry.addPrefixMatchFilter(query, from, type);
		}
		return simpleQuery
				.limit(MAX_RESULTS)
				.chunkAll()
				.keys()
				.list();
	}
}
