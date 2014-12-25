package me.lazerka.slounik.gae;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * @author Dzmitry Lazerka
 */
@Entity(name = "Slounik")
@Cache
public class Dictionary {
	@Id
	private String name;

	@AlsoLoad("LangFrom")
	private String langFrom;

	@AlsoLoad("LangTo")
	private String langTo;

	@SuppressWarnings("unused")
	private Dictionary() {}

	public Dictionary(String name, String langFrom, String langTo) {
		this.name = name;
		this.langFrom = langFrom;
		this.langTo = langTo;
	}

	public String getName() {
		return name;
	}

	public String getLangFrom() {
		return langFrom;
	}

	public String getLangTo() {
		return langTo;
	}
}
