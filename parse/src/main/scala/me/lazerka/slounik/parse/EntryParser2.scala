package me.lazerka.slounik.parse

import org.slf4j.{LoggerFactory, Logger}

import scala.util.parsing.combinator._

/**
 * @author Dzmitry Lazerka
 */
object EntryParser2 extends RegexParsers {
	val logger: Logger = LoggerFactory.getLogger(this.getClass)

	val word = "[А-Яа-яЎўІіЁё][а-яўіё'-]*!?".r
	val phrase = rep1(word) ^^ { words => new Lemma(words.mkString(" ")) }
	val phrases = rep1sep(phrase, ", ")

	val mainLemma = "<b>" ~> phrase <~ "</b>"
	val simple = mainLemma ~ "—" ~ phrases ^^ { case m ~ t ~ ls => Entry(m, ls) }

	val italicPhrases = "<i>" ~> phrases <~ "</i>"
	val usageHint = "(" ~> italicPhrases <~ ")" <~ opt(")")

	val variant = """[0-9]+\)""".r ~> phrases <~ usageHint
	val variants = rep1sep(variant, ";")

	val multi = mainLemma ~ "—" ~ variants ^^ { case l ~ t ~ v => Entry(l, v.flatten) }
	val global = simple | multi

	def parseLine(line: String): Option[Entry] =
		parse(global, line) match {
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
