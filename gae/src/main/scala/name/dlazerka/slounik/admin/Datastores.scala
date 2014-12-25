package name.dlazerka.slounik.admin
import com.google.appengine.api.datastore.DatastoreServiceFactory

trait Datastores {
	val datastore = DatastoreServiceFactory.getDatastoreService()
	val asyncDatastore = DatastoreServiceFactory.getAsyncDatastoreService()
}