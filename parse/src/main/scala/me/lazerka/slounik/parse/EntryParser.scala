package me.lazerka.slounik.parse

/**
 * @author Dzmitry Lazerka
 */
object EntryParser {
	val mainLemmaPattern = "^<b>(.*?)</b>".r

	val lemmaPattern = "[А-Яа-яЎўІіЁё]|([А-Яа-яЎўІіЁё][а-яўіё' -]*[а-яўіё'!-])".r
	val lemmaDeclarationPattern = s"$lemmaPattern( \\(?$lemmaPattern(, $lemmaPattern)*\\)?)?".r

	val underline = "<u>|</u>".r
	val spaces = "\\s+".r
	val wordCap = "[А-ЯЎІЁ][а-яўіё'-]+"
	val wordNocase = "[0-9A-Za-zА-ЯЎІЁа-яўіё'!.–-]+"
	val multiWordCommas = "(?:" + wordNocase + "(?:[ ,]+))+" + wordNocase
	val multiword = "(?:" + wordNocase + "(?:[ ]+))+" + wordNocase
	val drop = Array(
		"""\([^\)]+?\)""",
		"""<i>род\.</i> [^ <]+""",
		"""<b>\s*""" + multiWordCommas + """\s*</b>.*?(?:<br>|$)""",
		"<b>.*?</b>",
		"<i>.*?</i>",
		" -[^ ,;<]+",
		"""(?s)\(.*?\)""",
		"(?s)<a .*?</a>",
		" +-+ +",
		"""[а-я0-9]\)""",
		multiword,
		"<br>",
		"[0-9,;:◊!?.«»°—]+",
		"[0-9°]+",
		wordCap
	)
	val dropPatterns = drop.map(_.r())

	def parseLine(line: String): Seq[Entry] = {
		val mainLemmasString = mainLemmaPattern.findPrefixMatchOf(line).get.group(1)
		val mainLemmas = mainLemmasString
				.split('(')
				.toVector
				.flatMap(_.split(','))
				.map(_.replace(')', ' ').trim)
				.filter(lemma => {
			if (EntryParser.lemmaPattern.pattern.matcher(lemma).matches()) {
				true
			} else {
				println(s"Skipped lemma: $lemma")
				false
			}
		})

		var cleared = underline.replaceAllIn(line, "")

		// Not foldLeft() for debugging.
		dropPatterns.foreach(regex => {
			cleared = regex.replaceAllIn(cleared, " ")
		})
		val lemmas = spaces.split(cleared.trim)
				.distinct
				.filter(!_.isEmpty)
		if (lemmas.isEmpty) {
			println(s"Nothing parsed from line $line")
		}
		val translations = lemmas.filter({
			case lemmaPattern(_*) => true
			case lemma =>
				println(s"Skipped $lemma in $line")
				false
		})
		if (translations.nonEmpty) {
			mainLemmas.map(mainLemma => Entry(mainLemma, translations))
		} else {
			Seq.empty
		}
	}
}
