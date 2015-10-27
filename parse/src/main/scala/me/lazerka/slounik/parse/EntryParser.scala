package me.lazerka.slounik.parse

import scala.util.parsing.combinator._

/**
 * @author Dzmitry Lazerka
 */
object EntryParser extends RegexParsers {
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

	def parseLine(line: String): (Array[String], String) = {
		var cleared = underline.replaceAllIn(line, "")

		// Not foldLeft() for debugging.
		dropPatterns.foreach(regex => {
			cleared = regex.replaceAllIn(cleared, " ")
		})
		val lemmas = spaces.split(cleared.trim)
				.filter(!_.isEmpty)
				.distinct
		if (lemmas.isEmpty) {
			println(s"Nothing parsed from line $line")
		}
		val result = lemmas.filter ({
			case lemmaPattern(_*) => true
			case lemma =>
				println(s"Skipped $lemma in $line")
				false
		})
		(result, line)
	}
}
