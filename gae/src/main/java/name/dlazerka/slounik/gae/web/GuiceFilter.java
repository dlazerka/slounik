package name.dlazerka.slounik.gae.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Bypasses _ah/* requests except some.
 *
 * @author Dzmitry Lazerka
 */
public class GuiceFilter extends com.google.inject.servlet.GuiceFilter {
	private final static Logger logger = LoggerFactory.getLogger(GuiceFilter.class);

	private final Pattern HANDLE_WITH_GUICE = Pattern.compile(
			"^(/_ah/warmup)" +
			"|(/_ah/start)" +
			"|(/_ah/upload/.*)" +
			"|(/_ah/pipeline/.*)" +
			"|(/_ah/mapreduce/.*)" +
			"|(/_ah/channel/connected/.*)" +
			"|(/_ah/channel/disconnected/.*)"
	);

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest) {
			HttpServletRequest req = (HttpServletRequest) request;

			// Break the chain for dev server except warmup (must be handled by app).
			String requestUri = req.getRequestURI();
			boolean isAhRequest = requestUri.startsWith("/_ah/");

			if (!isAhRequest) {
				logger.trace("Handle with Guice: {}", requestUri);
				super.doFilter(request, response, chain);
				return;
			}

			if (shouldNotBypass(requestUri)) {
				logger.trace("Not bypassing Guice: {} {}", req.getMethod(), requestUri);
				super.doFilter(request, response, chain);
				return;
			}

			logger.trace("Bypassing Guice: {} {}", req.getMethod(), requestUri);
			chain.doFilter(request, response);
		} else {
			logger.warn("Got non-HTTP request (length={})", request.getContentLength());
			chain.doFilter(request, response);
		}
	}

	private boolean shouldNotBypass(String requestUri) {
		return HANDLE_WITH_GUICE.matcher(requestUri).matches();
	}
}
