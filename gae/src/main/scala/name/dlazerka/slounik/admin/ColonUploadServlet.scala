package name.dlazerka.slounik.admin

import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.io.IOUtils

import com.google.appengine.api.datastore.Entity
import com.google.appengine.api.datastore.Text

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.ServletException
import name.dlazerka.slounik.ColonSlounik

class ColonUploadServlet extends AbstractUploadServlet {
	override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
		var name: String = null
		var content: String = null
		var langFrom: String = null
		var langTo: String = null
		try {
			val upload = new ServletFileUpload()
			val iterator: FileItemIterator = upload.getItemIterator(req)
			while (iterator.hasNext) {
				val item: FileItemStream = iterator.next()
				val value = IOUtils.toString(item.openStream(), "UTF-8")


				logger.debug("Got a field: {}, name = {}", item.getFieldName.toString, item.getName)

				if (!item.isFormField) {
					logger.info("Got an uploaded file: {}, name = {}, contentType={}, headers={}",
						        Array(item.getFieldName,
						              item.getName,
						              item.getContentType,
						              item.getHeaders + "")
					)
					content = value
					name =  item.getName
				} else if ("langFrom" == item.getFieldName) {
					langFrom = value
				} else if ("langTo" == item.getFieldName) {
					langTo = value
				}
			}

			if (name == null || content == null || langFrom == null || langTo == null) {
				throw new IllegalArgumentException("Slounik is not initialized: " + name + " " + langFrom + langTo)
			}
			if (langFrom.equals(langTo)) {
				throw new IllegalArgumentException("Languages must not be equal")
			}
			save(name, content, langFrom, langTo)
			resp.setContentType("text/plain")
			resp.setStatus(HttpServletResponse.SC_OK)
		} catch {
		  case e: Exception => throw new ServletException(e)
		}
	}

	def save(name: String, content: String, langFrom: String, langTo: String) {
		logger.info("Saving colon of {} characters.", content.length().toString)
		val entity = new Entity(ColonSlounik.KIND, "colon")
		entity.setProperty(ColonSlounik.NAME, name)
		entity.setUnindexedProperty(ColonSlounik.CONTENT, new Text(content))
		entity.setProperty(ColonSlounik.LANG_FROM, langFrom)
		entity.setProperty(ColonSlounik.LANG_TO, langTo)
		val key = datastore.put(entity)
		logger.info("Saved, key={}.", key)
	}
}
