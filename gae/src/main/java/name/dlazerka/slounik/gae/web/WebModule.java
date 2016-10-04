package name.dlazerka.slounik.gae.web;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.util.jackson.ObjectifyJacksonModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Map;

/**
 * Web stuff configuration (servlets, filters, etc).
 *
 * @author Dzmitry Lazerka
 */
public class WebModule extends JerseyServletModule {
	private static final Logger logger = LoggerFactory.getLogger(WebModule.class);

	@Override
	protected void configureServlets() {
		logger.trace("configureServlets");

		// Objectify requires this while using Async+Caching
		// until https://code.google.com/p/googleappengine/issues/detail?id=4271 gets fixed.
		bind(ObjectifyFilter.class).in(Singleton.class);
		filter("/*").through(ObjectifyFilter.class);

		// Route all requests through GuiceContainer.
		serve("/*").with(GuiceContainer.class, getJerseyParams());
		//serve("/image/blobstore-callback-dev").with(BlobstoreCallbackServlet.class);

		bind(UnhandledExceptionMapper.class);

		setUpJackson();
	}

	private void setUpJackson() {
		// Handle "application/json" by Jackson.

		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(MapperFeature.AUTO_DETECT_GETTERS);
		mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS);
		mapper.disable(MapperFeature.AUTO_DETECT_SETTERS);
		mapper.enable(MapperFeature.USE_GETTERS_AS_SETTERS); // default
		mapper.enable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS); // default

		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES); // default
		mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
		mapper.enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
		mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES); // default

		// Probably we don't want to serialize Ref in full, but as Key always.
		mapper.registerModule(new ObjectifyJacksonModule());
		mapper.registerModule(new JodaModule());

		JacksonJsonProvider provider = new JacksonJsonProvider(mapper);
		bind(JacksonJsonProvider.class).toInstance(provider);
	}

	private Map<String, String> getJerseyParams() {
		Map<String,String> params = Maps.newHashMap();

		ImmutableList<String> packagesToScan = ImmutableList.of(
				"name.dlazerka.slounik.gae.rest",
				"name.dlazerka.slounik.gae.admin"
//				"name.dlazerka.slounik.gae.queue",
//				"name.dlazerka.slounik.gae._ah"
		);

		params.put(PackagesResourceConfig.PROPERTY_PACKAGES, Joiner.on(',').join(packagesToScan));
		// Read somewhere that it's needed for GAE.
		params.put(PackagesResourceConfig.FEATURE_DISABLE_WADL, "true");

		// This makes use of custom Auth+filters using OAuth2.
		// Commented because using GAE default authentication.
		// params.put(ResourceConfig.PROPERTY_RESOURCE_FILTER_FACTORIES, AuthFilterFactory.class.getName());

		//params.put("com.sun.jersey.spi.container.ContainerRequestFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		//params.put("com.sun.jersey.spi.container.ContainerResponseFilters", "com.sun.jersey.api.container.filter.LoggingFilter");
		//params.put("com.sun.jersey.config.feature.logging.DisableEntitylogging", "true");
		//params.put("com.sun.jersey.config.feature.Trace", "true");
		return params;
	}
}
