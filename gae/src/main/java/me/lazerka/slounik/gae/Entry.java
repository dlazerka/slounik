package me.lazerka.slounik.gae;

import com.google.common.base.Splitter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.SimpleQuery;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * TODO: Consists only of key for faster/cheaper lookups.
 *
 * @author Dzmitry Lazerka
 */
@Entity
@Cache
public class Entry {
	public static final char ARROWS = '↔';

	@Id
	private String key;

	@SuppressWarnings("unused")
	private Entry() {}

	public Entry(Key<Entry> key) {
		this.key = key.getName();
	}

	public Entry(String lemmaFrom, String lemmaTo, String langFrom, String langTo, String dictionaryName) {
		checkArgument(langFrom.indexOf(':') == -1);
		checkArgument(lemmaFrom.indexOf(ARROWS) == -1);
		checkArgument(langTo.indexOf(':') == -1);
		checkArgument(lemmaTo.indexOf(ARROWS) == -1);
		checkNotNull(dictionaryName);

		this.key = langFrom + ':' + lemmaFrom + ARROWS + langTo + ':' + lemmaTo + ARROWS + dictionaryName;

		checkArgument(key.length() < 500); // GAE limit on keys: http://stackoverflow.com/questions/2557632
	}

	/**
	 * Holds logic for key construction.
	 * If `query` ends on ` `, then adds full-match filter. Otherwise adds prefix-match filter.
	 */
	public static SimpleQuery<Entry> addFilter(String query, LoadType<Entry> type, String langFrom) {
		if (query.endsWith(" ")) {
			query = query.substring(0, query.length() - 1);
			return type.filterKey("= " + langFrom + ":" + query + ARROWS);
		}
		return type
				.filterKey(">= " + langFrom + ":" + query)
				.filterKey("< " + langFrom + ":" + query + Character.MAX_VALUE);
	}

	private String getKeyPart(int index) {
		List<String> parts = Splitter.on(ARROWS).splitToList(key);
		String part = parts.get(index / 2);
		List<String> split = Splitter.on(':').splitToList(part);
		return split.get(index % 2);
	}

	public String getLangFrom() {
		return getKeyPart(0);
	}

	public String getLemmaFrom() {
		return getKeyPart(1);
	}

	public String getLangTo() {
		return getKeyPart(2);
	}

	public String getLemmaTo() {
		return getKeyPart(3);
	}

	public String getDictionaryName() {
		return getKeyPart(4);
	}
}
