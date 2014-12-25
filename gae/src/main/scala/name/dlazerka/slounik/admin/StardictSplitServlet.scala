package name.dlazerka.slounik.admin

import java.nio.charset.Charset

import scala.collection.JavaConversions.asScalaIterator
import scala.collection.JavaConversions.asJavaIterable

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.appengine.api.datastore.Blob
import com.google.appengine.api.datastore.Entity
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory
import com.google.appengine.api.datastore.Query
import com.google.appengine.api.datastore.Text
import com.google.appengine.api.datastore.Transaction

import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import name.dlazerka.slounik.StarSlounik

class StardictSplitServlet extends HttpServlet with Datastores {
  	val logger: Logger = LoggerFactory.getLogger(this.getClass)
	val utf8 = Charset.forName("UTF-8")
	val NAME = "name"

	override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
		logger.info("Got request: {}", req.getRequestURI)

		val name = req.getParameter(NAME)
		val key = KeyFactory.createKey(StarSlounik.KIND, name)
		val txn = datastore.beginTransaction()
		val slounik = datastore.get(txn, key)
		val langFrom = slounik.getProperty("LangFrom")
		val langTo = slounik.getProperty("LangTo")
		val idx = readJoin(StarSlounik.KIND_IDX, key, txn)
		val dict = readJoin(StarSlounik.KIND_DICT, key, txn).toArray
		txn.rollback()

		var pool = List[Entity]()
		var pool_size = 0

		var i = 0
		while (idx.hasNext) {
			val lemmaBytes = idx.takeWhile(_ != 0).toArray
			val lemma = new String(lemmaBytes, utf8)
		  	val dict_offset = readInt(idx)
		  	val dict_size = readInt(idx)
		    val lineBytes = dict.slice(dict_offset, dict_offset + dict_size).toArray
		  	val line = new String(lineBytes, utf8)
		  	val entity = new Entity("StardictLine", name + " " + lemma)
		  	entity.setProperty("Lemma", lemma)
		  	entity.setUnindexedProperty("Content",
		  			if (line.length() < 500)
		  				line
		  			else
		  				new Text(line)
		  	)
		  	entity.setProperty("Offset", dict_offset)
		  	entity.setProperty("Size", dict_size)
		  	entity.setProperty("SlounikKey", key)
		  	entity.setProperty("LangFrom", langFrom)
		  	entity.setProperty("LangTo", langTo)
		  	pool ::= entity
		  	pool_size += lineBytes.length + entity.getKey.getName.getBytes.length + 100
		  	i += 1
		  	if (pool_size > 750000) {
			    datastore.put(asJavaIterable(pool))
		  		logger.info("Saved {} ({}) lines", pool.length, i)
		  		pool = List()
		  		pool_size = 0
		  	}
		}
		datastore.put(asJavaIterable(pool))
		logger.info("Total lines saved {}", i)
	}

	def readJoin(kind: String, parentKey: Key, txn: Transaction): Iterator[Byte] = {
		val q = new Query(kind, parentKey)
		val pq = datastore.prepare(txn, q)
		val entities: Iterator[Entity] = pq.asIterator()
		val blobs = entities map {_.getProperty(StarSlounik.CONTENT).asInstanceOf[Blob]}
		val bytesIter: Iterator[Array[Byte]] = blobs map {_.getBytes}
		bytesIter.foldLeft(Iterator[Byte]()) (_ ++ _)
	}

	def readInt(iter: Iterator[Byte]): Int = {
		try {
			iter.take(4).foldLeft(0)((a, b) => (a << 8) | (b & 0x7f) | (b & 0x80))
		} catch {
			case e: NoSuchElementException => throw new RuntimeException("EOF while integer expected", e)
		}
	}
}
