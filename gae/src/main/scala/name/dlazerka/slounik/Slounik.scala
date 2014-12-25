package name.dlazerka.slounik;


class Slounik {
	val NAME = "Name"
	val CONTENT = "Content"
	val LANG_FROM = "LangFrom"
	val LANG_TO = "LangTo"
	val LEMMA = "Lemma";
}

object ColonSlounik extends Slounik {
	val KIND = "ColonSlounik"
}

object StarSlounik extends Slounik {
	val KIND = "StarSlounik"
	val KIND_IDX = "IdxStarSlounik"
	val KIND_DICT = "DictStarSlounik"
	val FILE_TYPE = "FileType"
}
