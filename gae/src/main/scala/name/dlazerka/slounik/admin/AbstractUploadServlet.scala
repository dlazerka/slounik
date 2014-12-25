package name.dlazerka.slounik.admin

import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.Logger

import com.google.appengine.api.datastore.DatastoreServiceFactory

import javax.servlet.http.HttpServlet

abstract class AbstractUploadServlet extends HttpServlet {
	protected val logger = Logger(LoggerFactory.getLogger(this.getClass))
  	protected val datastore = DatastoreServiceFactory.getDatastoreService
	protected val asyncDatastore = DatastoreServiceFactory.getAsyncDatastoreService
}
