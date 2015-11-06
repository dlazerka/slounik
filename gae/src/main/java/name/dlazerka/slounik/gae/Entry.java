package name.dlazerka.slounik.gae;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.*;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.SimpleQuery;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.base.Preconditions.*;

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
	private static final char SEPARATOR = '|';

	/**
	 * For optimization, we store all needed for query here.
	 * Format :
	 * langsSorted|lemma|from|toLemma1&toLemma2&toLemma3|dict
	 */
	@Id
	private String key;

	/** Raw line original */
	@Unindex
	private String line;

	////////// Parsed ///////////////
	@Ignore
	@JsonProperty
	private String lemma;

	@Ignore
	@JsonProperty
	private String from;

	@Ignore
	@JsonProperty
	private String to;

	@Ignore
	@JsonProperty
	private String dict;

	@Ignore
	@JsonProperty
	private List<String> translations;

	@SuppressWarnings("unused")
	private Entry() {}

	public Entry(Key<Entry> key) {
		this.key = key.getName();
		parseKey();
	}

	public Entry(@Nonnull String key, @Nonnull String line) {
		this.key = checkNotNull(key);
		this.line = line;
		checkArgument(key.length() < 500, key);
		parseKey();
	}

	@OnLoad
	private void parseKey() {
		List<String> split = Splitter.on(SEPARATOR).splitToList(key);
		checkState(split.size() == 5, key);

		lemma = split.get(1);
		from = split.get(2);
		checkArgument(from.length() == 2);

		String langs = split.get(0);
		to = langs.startsWith(from) ? langs.substring(2, 4) : langs.substring(0, 2);

		translations = ImmutableList.copyOf(Splitter.on('&').split(split.get(3)));
		dict = split.get(4);
	}

	/**
	 * Holds logic for key construction.
	 */
	public static SimpleQuery<Entry> addFullMatchFilter(String lang1, String lang2, String query, LoadType<Entry> type) {
		checkArgument(query.indexOf(SEPARATOR) == -1, query);
		return addPrefixMatchFilter(lang1, lang2, query + SEPARATOR, type);
	}

	public static SimpleQuery<Entry> addPrefixMatchFilter(String lang1, String lang2, String query, LoadType<Entry> type) {
		checkArgument(query.indexOf(SEPARATOR) == -1 || query.indexOf(SEPARATOR) == query.length() - 1, query);
		String langs = lang1.compareTo(lang2) < 0 ? (lang1 + lang2) : (lang2 + lang1);

		return type
				.filterKey(">=", Key.create(Entry.class, langs + SEPARATOR + query))
				.filterKey("<", Key.create(Entry.class, langs + SEPARATOR + query + Character.MAX_VALUE));
	}

	public String getLine() {
		return line;
	}
}
