package name.dlazerka.slounik.admin.parser;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.tools.mapreduce.Context;

import java.io.IOException;

/**
 * @author Dzmitry Lazerka
 */
public abstract class AppEngineMapper<T, T1, T2, T3> {
	public abstract void map(Key key, Entity line, Context context0) throws IOException;
}
