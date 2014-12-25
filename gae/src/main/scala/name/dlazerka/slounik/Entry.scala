package name.dlazerka.slounik;

class Entry(val lang1: String, val lang2: String, val lemma1: String, val lemma2: String) {

}

object Entry {
	val KIND = "Entry"
	val LANGS = "Langs"
	val LEMMAS = "Lemmas";
	val SLOUNIK_IDS = "SlounikIDs";
	val SEPARATOR = "↔";
	val MAPPER_VERSION = "MapperVersion";
}
