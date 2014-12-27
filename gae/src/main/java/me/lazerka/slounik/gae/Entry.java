package me.lazerka.slounik.gae;

import com.google.common.base.Splitter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.SimpleQuery;
import me.lazerka.slounik.gae.rest.Lang;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Main entity of words storage, optimized for searching.
 *
 * Holds logic for `key`, no other class should know the format of `key`.
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
	 */
	public static SimpleQuery<Entry> addFullMatchFilter(String query, Lang from, LoadType<Entry> type) {
		checkArgument(query.indexOf(ARROWS) == -1, query);
		return addPrefixMatchFilter(query + ARROWS, from, type);
	}

	public static SimpleQuery<Entry> addPrefixMatchFilter(String query, Lang from, LoadType<Entry> type) {
		String fromS = from.name().toLowerCase();
		return type
				.filterKey(">=", Key.create(Entry.class, fromS + ':' + query))
				.filterKey("<", Key.create(Entry.class, fromS + ':' + query + Character.MAX_VALUE));
	}

	private String getKeyPart(int index) {
		List<String> parts = Splitter.on(ARROWS).splitToList(key);
		String part = parts.get(index / 2);
		List<String> split = Splitter.on(':').splitToList(part);
		return split.get(index % 2);
	}

	public Lang getLangFrom() {
		return Lang.valueOf(getKeyPart(0).toUpperCase());
	}

	public String getLemmaFrom() {
		return getKeyPart(1);
	}

	public Lang getLangTo() {
		return Lang.valueOf(getKeyPart(2).toUpperCase());
	}

	public String getLemmaTo() {
		return getKeyPart(3);
	}

	public String getDictionaryName() {
		return getKeyPart(4);
	}
}
