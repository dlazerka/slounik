package me.lazerka.slounik.gae.rest;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;
import me.lazerka.slounik.gae.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.List;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Dzmitry Lazerka
 */
@Path("/rest/dict")
public class DictResource {
	private static final Logger logger = LoggerFactory.getLogger(DictResource.class);

	@GET
	@Path("/list/{lang1:[a-z][a-z]}/{lang2:[a-z][a-z]}/")
	@Produces("application/json")
	public List<Dictionary> list(
			@PathParam("lang1") String lang1,
			@PathParam("lang2") String lang2
	) {
		List<Dictionary> result12 = fetch(lang1, lang2);
		List<Dictionary> result21 = fetch(lang2, lang1);

		logger.trace("Found {}+{} results", result12.size(), result21.size());

		return ImmutableList.<Dictionary>builder()
				.addAll(result12)
				.addAll(result12)
				.build();
	}

	private List<Dictionary> fetch(String langFrom, String langTo) {
		return ofy()
				.load()
				.type(Dictionary.class)
				.filter("langFrom =", langFrom)
				.filter("langTo =", langTo)
				.limit(100)
				.chunkAll()
				.list();
	}
}
