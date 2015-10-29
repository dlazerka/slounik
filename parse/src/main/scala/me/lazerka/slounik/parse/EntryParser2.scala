package me.lazerka.slounik.parse

import org.slf4j.{LoggerFactory, Logger}

import scala.language.postfixOps
import scala.util.parsing.combinator._

/**
 * @author Dzmitry Lazerka
 */
object EntryParser2 extends RegexParsers {
	val logger: Logger = LoggerFactory.getLogger(this.getClass)

	val word = """[А-Яа-яЎўІіЁё][а-яўіё'-]*!?""".r
	val shortenedWord = """[а-яўіё'-]+\.""".r
	val phrase = rep1(word) ^^ { words => new Phrase(words.mkString(" ")) }
	val phrases = rep1sep(phrase, opt(", " | ";"))

	val mainLemma = "<b>" ~> phrase <~ "</b>"

	val simple = mainLemma ~ "—" ~ phrases ^^ { case m ~ t ~ ls => Entry(m, ls.toSet) }

//	val usageHint = "(<i>" ~> phrases <~ "</i>)" <~ opt(")")
//	val usageHint = ("(<i>" | "<i>(") ~> phrases <~ (")</i>" | "</i>") <~ opt(")")

	val hint = """<i>[а-яўіё]+\.</i>""".r
	val translations = (hint ?) ~> phrases

	// Parentheses and italic opener can be misplaced, see `асадка` test.
	val usageHint = """\(?<i>\(?""".r ~> phrases <~ """\)?</i>\)?\)?""".r
	// Extra parentheses opener can be at the end, see `папячы` test.
	val variant = """[0-9]+\)""".r ~> translations <~ (usageHint ?)

	val variants = rep1sep(variant, ";" ?)

	val multi = mainLemma ~ "—" ~ variants ^^ { case l ~ t ~ v => Entry(l, v.flatten.toSet) }
	val global = simple | multi

	def parseLine(line: String): Option[Entry] =
		parseAll(global, line) match {
			case Success(matched, input) =>
				Some(matched)
			case Failure(msg, _) =>
				logger.info(s"Failure: $msg")
				None
			case Error(msg, _) =>
				logger.error(s"Error: $msg")
				None
		}
}
