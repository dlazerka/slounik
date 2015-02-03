package me.lazerka.slounik.gae;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * Currently created only manually.
 *
 * @author Dzmitry Lazerka
 */
@Entity
@Cache
public class Dictionary {
	@Id
	@JsonProperty
	private String id;

	@JsonProperty
	private String name;

	@JsonProperty
	private String url;

	@JsonProperty
	private String from;

	@JsonProperty
	private String to;

	@SuppressWarnings("unused")
	private Dictionary() {}
}
