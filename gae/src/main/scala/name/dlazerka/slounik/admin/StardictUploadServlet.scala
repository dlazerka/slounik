package name.dlazerka.slounik.admin

import scala.collection.JavaConversions.asJavaIterable

import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.io.IOUtils

import com.google.appengine.api.datastore.Blob
import com.google.appengine.api.datastore.Entity
import com.google.appengine.api.datastore.Key
import com.google.appengine.api.datastore.KeyFactory

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletException
import name.dlazerka.slounik.StarSlounik

class StardictUploadServlet extends AbstractUploadServlet {
	override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
		var nameDict: String = null
		var nameIdx: String = null
		var contentDict: Array[Byte] = null
		var contentIdx: Array[Byte] = null
		var langFrom: String = null
		var langTo: String = null
		try {
			val upload = new ServletFileUpload()
			val iterator: FileItemIterator = upload.getItemIterator(req)
			while (iterator.hasNext) {
				val item: FileItemStream = iterator.next()
				val value = IOUtils.toByteArray(item.openStream())
				logger.debug("Got a field: {}, name = {}", item.getFieldName, item.getName)
				if (!item.isFormField) {
					logger.info("Got an uploaded file: {}, name = {}, contentType={}, headers={}",
						        Array(item.getFieldName,
						              item.getName,
						              item.getContentType,
						              item.getHeaders + "")
					)
					val name =  item.getName
					if (name.endsWith(".idx")) {
						nameIdx = name
						contentIdx = value
					} else if (name.endsWith(".dict")) {
						nameDict = name
						contentDict = value
					}
				} else if ("langFrom" == item.getFieldName) {
					langFrom = new String(value, "UTF-8")
				} else if ("langTo" == item.getFieldName) {
					langTo = new String(value, "UTF-8")
				}
			}

			if (nameIdx == null ||
				contentIdx == null ||
				nameDict == null ||
				contentDict == null ||
				langFrom == null ||
				langTo == null
			) {
				throw new IllegalArgumentException("Slounik is not initialized: " + nameIdx + " " +
						nameDict + " " + langFrom + langTo)
			}
			if (langFrom.equals(langTo)) {
				throw new IllegalArgumentException("Languages must not be equal")
			}
			save(nameIdx, contentIdx, nameDict, contentDict, langFrom, langTo)
			resp.setContentType("text/plain")
			resp.setStatus(HttpServletResponse.SC_OK)
		} catch {
		  case e: Exception => throw new ServletException(e)
		}
	}

	def save(nameIdx: String, contentIdx: Array[Byte], nameDict: String, contentDict: Array[Byte], langFrom: String, langTo: String) {
		logger.info("Saving stardict {}.", Array(nameIdx, contentIdx.length, nameDict, contentDict.length).toString)

		val txn = datastore.beginTransaction()
		val name = nameIdx.substring(0, nameIdx.lastIndexOf("."))

//		if (datastore.get(txn, parentKey) != null) {
//			throw new IllegalArgumentException(parentKey + " already exists")
//		}

		val parentKey = KeyFactory.createKey(StarSlounik.KIND, name)
		val parentEntity = new Entity(parentKey)
		parentEntity.setProperty(StarSlounik.NAME, name)
		parentEntity.setProperty(StarSlounik.LANG_FROM, langFrom)
		parentEntity.setProperty(StarSlounik.LANG_TO, langTo)
		datastore.put(txn, parentEntity)

		val idxEntities = toEntities(StarSlounik.KIND_IDX, contentIdx, parentKey)
		datastore.put(txn, asJavaIterable(idxEntities))
		val dictEntities = toEntities(StarSlounik.KIND_DICT, contentDict, parentKey)
		datastore.put(txn, asJavaIterable(dictEntities))

		txn.commit()
	}

	def toEntities(kind: String, content: Array[Byte], parentKey: Key): List[Entity] = {
		val slices = content.grouped((1 << 20) - 10240).zipWithIndex.toList
		for ((slice, i) <- slices) yield {
			val entity: Entity = new Entity(kind, i+1, parentKey)
			entity.setUnindexedProperty(StarSlounik.CONTENT, new Blob(slice))
			entity
		}
	}
}
