package name.dlazerka.slounik

import java.io.{ByteArrayOutputStream, PrintStream, Writer}
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import com.google.appengine.api.datastore.FetchOptions.Builder.withLimit
import com.google.appengine.api.datastore.{DatastoreServiceFactory, Entity, PreparedQuery, Query}
import com.typesafe.scalalogging.Logger
import org.apache.commons.lang3.StringEscapeUtils
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._

class SearchServlet extends HttpServlet {
	val logger = Logger(LoggerFactory.getLogger(this.getClass))
	val MAX_QUERY_LENGTH = 100
	val LANGS_COUNT = 2
	val MAX_RESULTS = 100
	val asyncDatastore = DatastoreServiceFactory.getAsyncDatastoreService
	val datastore = DatastoreServiceFactory.getDatastoreService

	override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
		try {
			val query = req.getParameter("q")
			if (query == null) {
				throw new QueryException("Query is null")
			}
			if (query.length() > MAX_QUERY_LENGTH) {
				throw new QueryException("Query max length is " + MAX_QUERY_LENGTH)
			}

			val searchResults = search(query)
			resp.setContentType("application/json")
			resp.setCharacterEncoding("UTF-8")
			writeSearchResultsAsXml(searchResults, query, resp.getWriter);
		} catch {
			case e: QueryException => logger.warn(e.getMessage);
			case e: Throwable =>
				logger.error(e.getMessage)
				val ba = new ByteArrayOutputStream()
				e.printStackTrace(new PrintStream(ba))
				logger.error(new String(ba.toByteArray, "UTF-8"))
		}
	}

	def search(query: String): List[Entry] = {
		logger.info("Searching '{}'", query)
		val prepared = if (query.endsWith(" ")) {
		  val query2 = query.substring(0, query.length() - 1)
			prepareQuery(query2)
		} else {
		  preparePrefixQuery(query)
		}

		val entities = prepared.asIterable(withLimit(MAX_RESULTS + 1))
		var searchResults = List[Entry]()
		for (entity <- entities) {
			val entry = getEntry(entity)
			searchResults ::= entry
		}
		searchResults
	}

	def prepareQuery(lemma: String): PreparedQuery = {
		val query = new Query(Entry.KIND)
		query.addFilter(Entry.LEMMAS, Query.FilterOperator.EQUAL, lemma)
		datastore.prepare(query)
	}

	def preparePrefixQuery(prefix: String): PreparedQuery = {
		val query = new Query(Entry.KIND)
		query.addFilter(Entry.LEMMAS, Query.FilterOperator.GREATER_THAN_OR_EQUAL, prefix)
		query.addFilter(Entry.LEMMAS, Query.FilterOperator.LESS_THAN, prefix + Char.MaxValue)
		datastore.prepare(query)
	}

	def getEntry(entry: Entity): Entry = {
		val name = entry.getKey.getName
		val offset = name.indexOf(Entry.SEPARATOR)
		val lang1 = name.substring(0, 2)
		val lemma1 = name.substring(3, offset)
		val lang2 = name.substring(offset + 1, offset + 3)
		val lemma2 = name.substring(offset + 4)
		new Entry(lang1, lang2, lemma1, lemma2)
	}

	def writeSearchResultsAsXml(
	  searchResults: List[Entry],
	  query: String,
	  writer: Writer)
	{
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
		writer.write("<searchResults>\n")
		writer.write("	<query>" + StringEscapeUtils.escapeXml(query) + "</query>\n")
		searchResults foreach {entry =>
			writer.write("	<entry>\n")
			writer.write("		<lexeme lang=\"" + entry.lang1 + "\">" + entry.lemma1 + "</lexeme>\n")
			writer.write("		<lexeme lang=\"" + entry.lang2 + "\">" + entry.lemma2 + "</lexeme>\n")
			writer.write("	</entry>\n");
		}
		writer.write("</searchResults>\n")
	}
}
