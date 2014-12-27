package me.lazerka.slounik.gae.rest;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Locale;

/**
 * @author Dzmitry Lazerka
 */
public enum Lang {
	BE,
	RU,
	;

	public static class LangSerializer extends JsonSerializer<Lang> {
		@Override
		public void serialize(Lang value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
			jgen.writeString(value.name().toLowerCase(Locale.US));
		}
	}
}
