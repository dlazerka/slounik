package name.dlazerka.slounik.gae.web;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;

/**
 * Handle various exceptions.
 *
 * Note that if you throw a WebApplicationException WITH Response, it doesn't come here.
 *
 * @author Dzmitry Lazerka
 */
@Singleton
@javax.ws.rs.ext.Provider
public class UnhandledExceptionMapper implements ExceptionMapper<Exception> {
	private static final Logger logger = LoggerFactory.getLogger(UnhandledExceptionMapper.class);

	@Inject
	Provider<UriInfo> uriInfoProvider;

	@Inject
	Provider<Request> requestProvider;

	@Override
	public Response toResponse(Exception exception) {
		String method = requestProvider.get().getMethod();
		URI uri = uriInfoProvider.get().getRequestUri();
		String head = method + ' ' + uri;

		if (exception instanceof WebApplicationException) {
			Response response = ((WebApplicationException) exception).getResponse();
			if (response.getStatus() == 404) {
				logger.warn("{} Not Found", head);
			} else {
				logger.warn("{} WebApplicationException: {} {}",
						head,
						response.getStatus(),
						response.getEntity());
			}

			return response;
		}

		if (exception instanceof UnrecognizedPropertyException) {
			logger.warn("{}: {}", head, exception.getMessage());
			return Response.status(Status.BAD_REQUEST)
					.entity(exception.getMessage())
					.type(MediaType.TEXT_PLAIN)
					.build();
		}

		logger.error("{}: Unhandled exception", head, exception);

		return Response.status(Status.INTERNAL_SERVER_ERROR)
				.entity(exception.getMessage())
				.type(MediaType.TEXT_PLAIN)
				.build();
	}
}
