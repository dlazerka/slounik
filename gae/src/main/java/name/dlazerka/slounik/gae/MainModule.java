package name.dlazerka.slounik.gae;

import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.mail.MailService;
import com.google.appengine.api.mail.MailServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import name.dlazerka.slounik.gae.web.WebModule;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;

/**
 * Main project configuration.
 *
 * @author Dzmitry Lazerka
 */
public class MainModule extends AbstractModule {
	private static final Logger logger = LoggerFactory.getLogger(MainModule.class);

	@Override
	protected void configure() {
		// Nota available due to access restrictions on GAE.
		// DateTimeZone.setDefault(DateTimeZone.UTC);

		install(new WebModule());

		install(new ObjectifyModule());

		bindGaeServices();

		logger.debug("MainModule set up.");
	}

	private void bindGaeServices() {
		bind(BlobstoreService.class).toInstance(BlobstoreServiceFactory.getBlobstoreService());
		bind(ChannelService.class).toInstance(ChannelServiceFactory.getChannelService());
		bind(DatastoreService.class).toInstance(DatastoreServiceFactory.getDatastoreService());
		bind(ImagesService.class).toInstance(ImagesServiceFactory.getImagesService());
		bind(MailService.class).toInstance(MailServiceFactory.getMailService());
		bind(MemcacheService.class).toInstance(MemcacheServiceFactory.getMemcacheService());
		bind(URLFetchService.class).toInstance(URLFetchServiceFactory.getURLFetchService());
		bind(UserService.class).toInstance(UserServiceFactory.getUserService());
	}

	@Provides
	@Named("now")
	private DateTime now() {
		return DateTime.now(DateTimeZone.UTC);
	}

}
