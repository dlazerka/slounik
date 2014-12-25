package name.dlazerka.db;

import static com.google.appengine.api.datastore.FetchOptions.Builder.withDefaults;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Email;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

public class DBServlet extends HttpServlet {
	private static final Logger logger = LoggerFactory.getLogger(DBServlet.class);

	private static final Pattern KEY_ID_PATTERN = Pattern.compile("^(\\w+)\\((\\d+)\\)$");
	private static final Pattern KEY_NAME_PATTERN = Pattern.compile("^(\\w+)\\(\"([^\"]+)\"\\)$");

	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	/**
	 * Supported value types for filters.
	 */
	enum Type {
		STRING,
		LONG,
		KEY,
		BOOLEAN,
		DATE,
		EMAIL,
		NULL,
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		logger.debug("Got request: {}", req.getRequestURI());
		resp.setContentType("application/json");
		resp.setCharacterEncoding("UTF-8");
		try {
			String query = req.getParameter("query");
			if (query == null) {
				resp.getWriter().write("No query given");
				return;
			}
			JSONObject jsonQuery = new JSONObject(query);
			Query q = buildQuery(jsonQuery);
			FetchOptions o = buildOptions(jsonQuery);

			String pathInfo = req.getPathInfo();
			if ("/list".equals(pathInfo)) {
				Iterable<Entity> entities = list(q, o);
				String json = toJson(entities);
				resp.getWriter().write(json);
			} else if ("/delete".equals(pathInfo)) {
				delete(q, o);
			} else if ("/count".equals(pathInfo)) {
				int count = count(q, o);
				String json = toJson(count);
				resp.getWriter().write(json);
			} else {
				logger.warn("Unknown pathInfo: {}", pathInfo);
			}
		} catch (Exception e) {
			try {
				writeAsJson(e, resp.getWriter());
			} catch (JSONException e1) {
				logger.error("Unable to report: " + e1);
			}
			throw new ServletException(e);
		}
	}

	Query buildQuery(JSONObject jsonQuery) throws JSONException, ParseException {
		Query q = newQuery(jsonQuery);
		JSONArray jsonFilters = jsonQuery.getJSONArray("filters");
		for (int i = 0; i < jsonFilters.length(); i++) {
			JSONObject jsonFilter = jsonFilters.getJSONObject(i);
			String field = jsonFilter.getString("field");
			FilterOperator operator = FilterOperator.valueOf(jsonFilter.getString("operator"));
			String valueStr = jsonFilter.getString("value");
			Type type = Type.valueOf(jsonFilter.getString("type"));

			if (field.trim().isEmpty()) {
				continue;
			}
			if (field.equals(Entity.KEY_RESERVED_PROPERTY)) {
				type = Type.KEY;
			}
			Object value;
			switch (type) {
			case STRING: value = valueStr; break;
			case LONG: value = Long.parseLong(valueStr); break;
			case KEY: value = asKey(valueStr); break;
			case BOOLEAN: value = Boolean.parseBoolean(valueStr); break;
			case DATE: value = SimpleDateFormat.getDateInstance().parse(valueStr); break;
			case EMAIL: value = new Email(valueStr); break;
			case NULL: value = null; break;
			default: throw new RuntimeException();
			}
			q.addFilter(field, operator, value);
		}
		return q;
	}

	private Query newQuery(JSONObject jsonQuery) throws JSONException {
		String kind = null;
		if (jsonQuery.has("kind")) {
			kind = jsonQuery.getString("kind");
			if ("".equals(kind.trim())) {
				kind = null;
			}
		}
		String ancestor = null;
		if (jsonQuery.has("ancestor")) {
			ancestor = jsonQuery.getString("ancestor");
			if ("".equals(ancestor.trim())) {
				ancestor = null;
			}
		}
		Query q;
		if (ancestor != null && kind != null) {
 			Key key = asKey(ancestor);
			q = new Query(kind, key);
		} else if (ancestor != null) {
 			Key key = asKey(ancestor);
			q = new Query(key);
		} else if (kind != null) {
			q = new Query(kind);
		} else {
			throw new IllegalArgumentException("No 'kind' or 'ancestor' argument");
		}
		return q;
	}

	Key asKey(String valueStr) {
		valueStr = valueStr.trim();
		Matcher matcher = KEY_ID_PATTERN.matcher(valueStr);
		if (!matcher.matches()) {
			matcher = KEY_NAME_PATTERN.matcher(valueStr);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Unable to parse " + valueStr +
						" using patterns " + KEY_NAME_PATTERN + " or " + KEY_ID_PATTERN);
			}
			return KeyFactory.createKey(matcher.group(1), matcher.group(2));
		}
		return KeyFactory.createKey(matcher.group(1), Long.parseLong(matcher.group(2)));
	}

	private FetchOptions buildOptions(JSONObject jsonQuery) throws JSONException {
		int limit = jsonQuery.getInt("limit");
		FetchOptions o = withDefaults();
		o.limit(limit);
		o.chunkSize(limit < 1000 ? limit : (limit / 10)); // don't really know what to put here
		return o;
	}

	void writeAsJson(Exception e, Writer writer) throws JSONException {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		JSONWriter jsonWriter = new JSONWriter(writer);
		jsonWriter.object().key("error").value(sw.toString()).endObject();
	}

	public Iterable<Entity> list(Query q, FetchOptions o) {
		logger.info("{} {}", q, o);
		PreparedQuery pq = datastore.prepare(q);
		return pq.asIterable(o);
	}

	String toJson(int count) throws JSONException {
		JSONStringer json = new JSONStringer();
		json.object().key("count").value(count).endObject();
		return json.toString();
	}

	String toJson(Iterable<Entity> entities) throws JSONException {
		JSONStringer json = new JSONStringer();
		json.object().key("entities").array();

		for (Entity entity : entities) {
			json.object();
			toJsonProperty(json, Entity.KEY_RESERVED_PROPERTY, entity.getKey());
			for (Map.Entry<String, Object> property : entity.getProperties().entrySet()) {
				String key = property.getKey();
				Object value = property.getValue();
				toJsonProperty(json, key, value);
			}
			json.endObject();
		}
		json.endArray();
		json.endObject();
		return json.toString();
	}

	private void toJsonProperty(JSONStringer json,
			String key, Object value)
					throws JSONException {
		String valueStr = value + "";
		if (valueStr.length() > 127) valueStr = valueStr.substring(0, 127);
		json.key(key).object();
		json.key("value").value(valueStr);
		json.key("type").value(value == null ? "null" : value.getClass().getName());
		json.endObject();
	}

	public void delete(Query q, FetchOptions o) throws IOException {
		q.setKeysOnly();
		Iterable<Entity> entities = list(q, o);

		final int bulkSize = 1000;
		ArrayList<Key> keysBulk = new ArrayList<Key>(bulkSize);
		for (Entity entity : entities) {
			keysBulk.add(entity.getKey());
			if (keysBulk.size() == bulkSize) {
				logger.info("Deleting bulk of " + keysBulk.size());
				datastore.delete(keysBulk);
				keysBulk.clear();
			}
		}
		logger.info("Deleting final bulk of " + keysBulk.size());
		datastore.delete(keysBulk);
		logger.info("All completed.");
	}

	public int count(Query q, FetchOptions o) throws IOException {
		int count = 0;
		q.setKeysOnly();
		Iterable<Entity> entities = list(q, o);
		logger.info("Starting counting...");
		for (@SuppressWarnings("unused") Entity entity : entities) {
			count++;
		}
		logger.info("Counted: " + count);
		return count;
	}

}
