package me.lazerka.slounik.gae.rest;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Dzmitry Lazerka
 */
public class EntryBean {
	private final Map<Lang, String> lemmas;

	@JsonProperty
	private final String dictName;

	public EntryBean(String lemmaFrom, String lemmaTo, Lang from, Lang to, String dictName) {
		lemmas = new EnumMap<>(Lang.class);
		lemmas.put(from, lemmaFrom);
		lemmas.put(to, lemmaTo);
		this.dictName = dictName;
	}

@JsonAnyGetter
@JsonSerialize(keyUsing = Lang.LangSerializer.class)
public Map<Lang, String> getLemmas() {
	return lemmas;
}
}
