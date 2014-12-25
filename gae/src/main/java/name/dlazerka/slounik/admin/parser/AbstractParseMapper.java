package name.dlazerka.slounik.admin.parser;

import com.google.appengine.api.datastore.*;
import com.google.appengine.tools.mapreduce.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import name.dlazerka.slounik.Entry;
import name.dlazerka.slounik.StarSlounik;
//import org.apache.hadoop.io.NullWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Parses each Line and saves one Entry for each line.
 *
 * Using Java because unable to inherit AppEngineMapper using Scala.
 *
 * @see https://groups.google.com/d/topic/scala-user/B37t9-xXkiQ/discussion
 */
public abstract class AbstractParseMapper extends
		AppEngineMapper<Key, Entity, Void, Void> {
	private static final Logger logger = LoggerFactory.getLogger(StarParseMapper.class);
	private static final Pattern WORD_PATTERN = Pattern.compile("[А-Яа-яЎўІіЁё][а-яўіё'-]*");

	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

	@Override
	public void map(Key key, Entity line, Context context0) throws IOException {
		String lemma1 = (String) line.getProperty(StarSlounik.LEMMA());
		if (!WORD_PATTERN.matcher(lemma1).matches()) {
			// Drop non-words
			return;
		}
		String content = getContent(line);

		Iterable<String> lemmas = mapInternal(content);
		for (String lemma2 : lemmas) {
			if (!WORD_PATTERN.matcher(lemma2).matches()) {
				logger.warn("{} is not word, lemma={}, slounikKey={}",
						new String[] {lemma2, lemma1, line.getProperty("SlounikKey").toString()});
				continue;
			}
			for (int i = 0; i < 10; i++) {
				Transaction txn = datastore.beginTransaction();
				try {
					save(lemma1, lemma2, line, txn);
					txn.commit();
					break;
				} catch (ConcurrentModificationException e) {
				} catch (DatastoreFailureException e) {
				} catch (DatastoreTimeoutException e) {
				} finally {
					if (txn.isActive()) txn.rollback();
					if (i == 9) logger.error("{}:{} was not saved", new String[]{lemma1, lemma2});
				}
			}
		}
	}

	private String getContent(Entity line) {
		Object obj = line.getProperty(StarSlounik.CONTENT());
		String content;
		if (obj instanceof String) {
			content = (String) obj;
		} else {
			content = ((Text) obj).getValue();
		}
		return content;
	}

	protected abstract Iterable<String> mapInternal(String content)
			throws IOException;

	protected String getVersion() {
		return "2011-11-10 00:37";
	}

	@SuppressWarnings("unchecked")
	protected void save(String lemmaFrom, String lemmaTo, Entity line, Transaction txn) {
		lemmaFrom = lemmaFrom.toLowerCase().trim();
		lemmaTo = lemmaTo.toLowerCase().trim();

		if (lemmaFrom.contains(Entry.SEPARATOR()) || lemmaTo.contains(Entry.SEPARATOR())) {
			throw new IllegalArgumentException();
		}

		String langFrom = (String) line.getProperty(StarSlounik.LANG_FROM());
		String langTo = (String) line.getProperty(StarSlounik.LANG_TO());
		if (langFrom == null || langTo == null) {
			throw new NullPointerException();
		}
		String lexeme1 = langFrom + ':' + lemmaFrom;
		String lexeme2 = langTo + ':' + lemmaTo;

		String keyName;
		if (lexeme1.compareTo(lexeme2) <= 0) {
			keyName = lexeme1 + Entry.SEPARATOR() + lexeme2;
		} else {
			keyName = lexeme2 + Entry.SEPARATOR() + lexeme1;
		}

		Key slounikKey = (Key) line.getProperty("SlounikKey");

		Key key = KeyFactory.createKey(Entry.KIND(), keyName);

		Set<Key> slounikKeys = Sets.newHashSet(slounikKey);
		Entity entry;
		try {
			entry = datastore.get(txn, key);
			if (getVersion().equals(entry.getProperty(Entry.MAPPER_VERSION()))) {
				return;
			}
			slounikKeys.addAll((Collection<Key>)entry.getProperty(Entry.SLOUNIK_IDS()));
		} catch (EntityNotFoundException e) {
			entry = new Entity(key);
			entry.setProperty(Entry.LANGS(), Lists.newArrayList(langFrom, langTo));
			entry.setProperty(Entry.LEMMAS(), Lists.newArrayList(lemmaFrom, lemmaTo));
		}
		entry.setProperty(Entry.MAPPER_VERSION(), getVersion());
		entry.setProperty(Entry.SLOUNIK_IDS(), slounikKeys);

		datastore.put(txn, entry);
	}

}
