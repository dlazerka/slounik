package me.lazerka.slounik.parse

import scala.language.postfixOps
import scala.util.parsing.combinator._

/**
 * @author Dzmitry Lazerka
 */
object EntryParser2 extends RegexParsers {

	val word = """[А-Яа-яЎўІіЁё][а-яўіё'-]*!?""".r
	val phrase = rep1(word) ^^ { words => words.mkString(" ") }
	val wordComma = """[А-Яа-яЎўІіЁё][а-яўіё']*,""".r
	val words = rep1(wordComma | word) ^^ {_.mkString(" ")}
	val phrasePunctuated = words ~ rep("-" ~ words) ^^ { case w ~ ws => w + " " + ws.mkString(" ") }

	val phrases = rep1sep(phrase, opt(", " | ";"))

	val hint = """<i>[а-яўіё]+\.</i>""".r

	val mainLemma = "<b>" ~> phrase <~ "</b>" <~ (hint ?)
	val mainLemmaPunctuated = "<b>" ~> phrasePunctuated <~ "</b>"

	val simple = mainLemma ~ "—" ~ phrases ^^ { case m ~ t ~ ls => Entry(m, ls.distinct) }

	// Parentheses and italic opener can be misplaced, see `асадка` test.
	// Extra parentheses opener can be at the end, see `папячы` test.
	val usageHint = """\(?<i>\(?""".r ~> phrases <~ """\)?</i>\)?\)?""".r

	val translations = (hint ?) ~> phrases <~ (usageHint ?)

	val variant = """[0-9]+\)""".r ~> translations
	val variants = rep1sep(variant, ";" ?)

	val example = "<br>" ~ mainLemmaPunctuated ~ phrasePunctuated
	val variantB = """<b>[0-9]+\.</b>""".r ~> opt(usageHint) ~> phrases <~ rep(example)
	val variantsB = rep1sep(variantB, "<br>")

	val rest = ("—" ~> variants) | ("<br>" ~> variantsB)

	val multi = mainLemma ~ rest ^^ { case l ~ v => Entry(l, v.flatten.distinct) }
	val global = simple | multi

	def parseLine(line: String): Option[Entry] =
		parseAll(global, line) match {
			case Success(matched, input) =>
				Some(matched)
			case Failure(msg, _) =>
				println(s"Failure: $msg")
				None
			case Error(msg, _) =>
				println(s"Error: $msg")
				None
		}
}
