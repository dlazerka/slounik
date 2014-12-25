package me.lazerka.slounik.gae.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dzmitry Lazerka
 */
public class EntryBean {
	@JsonProperty
	String lemmaFrom;

	@JsonProperty
	String lemmaTo;

	@JsonProperty
	String langFrom;

	@JsonProperty
	String langTo;

	public EntryBean(String lemmaFrom, String lemmaTo, String langFrom, String langTo) {
		this.lemmaFrom = lemmaFrom;
		this.lemmaTo = lemmaTo;
		this.langFrom = langFrom;
		this.langTo = langTo;
	}
}
