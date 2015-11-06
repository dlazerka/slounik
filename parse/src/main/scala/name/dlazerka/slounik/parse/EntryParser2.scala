/*
 *    Copyright 2015 Dzmitry Lazerka
 *
 *    This file is part of Slounik.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package name.dlazerka.slounik.parse

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

	val hint = "<i>\\(?".r ~> rep1( "[а-яўіё]+\\.?".r) <~ "\\)?</i>".r

	val secondMainLemma = "(" ~> phrase <~ ")"
	val mainLemmas = "^<b>".r ~> phrase ~ rep(secondMainLemma) <~ "</b>" <~ (hint ?) ^^ {case p ~ ps => p :: ps}
	val mainLemmaPunctuated = "<b>" ~> phrasePunctuated <~ "</b>"

	val phrases = rep1sep(phrase, opt(", " | ";"))

	// Parentheses and italic opener can be misplaced, see `асадка` test.
	// Extra parentheses opener can be at the end, see `папячы` test.
	val usageHint = """\(?<i>\(?""".r ~> phrases <~ """\)?</i>\)?\)?""".r

	val simple = mainLemmas ~ "—" ~ phrases <~ opt(usageHint) ^^ {
		case m ~ t ~ ls => m.map(mainLemma => Entry(mainLemma, ls.distinct))
	}

	val translations = (hint ?) ~> phrases <~ opt(usageHint)

	val variant = """[0-9]+\)""".r ~> translations
	val variants = rep1sep(variant, ";" ?)

	val example = "<br>" ~ mainLemmaPunctuated ~ phrasePunctuated
	val variantB = """<b>[0-9]+\.</b>""".r ~> opt(usageHint) ~> phrases <~ rep(example)
	val variantsB = rep1sep(variantB, "<br>")

	val rest = ("—" ~> variants) | ("<br>" ~> variantsB)

	val multi = mainLemmas ~ rest ^^ { case mls ~ v => mls.map(mainLemma => Entry(mainLemma, v.flatten.distinct)) }

//	val withForms = mainLemmas ~ word ~ ", .*".r ^^ {
//		case mls ~ w ~ t => mls.map((ml: String) => Entry(ml, Seq(w)))
//	}
	val global = multi | simple

	def parseLine(line: String): Seq[Entry] =
		parseAll(global, line) match {
			case Success(matched, input) =>
				matched
			case Failure(msg, input: Input) =>
				printFailure(msg, line, input.offset)
				Seq.empty
			case Error(msg, input: Input) =>
				printFailure(msg, line, input.offset)
				throw new Exception(msg)
		}

	def printFailure(msg: String, line: String, offset: Int) = {
		Console.out.print("Failure @ `")
		val s1 = line.slice(offset - 10, offset)
		val s2 = line.slice(offset, offset + 1)
		val s3 = line.slice(offset + 1, offset + 10)
		Console.out.print(s1)
		Console.err.print(s2)
		Console.out.print(s3)
		//				val spot = line.slice(input.offset - 10, input.offset + 10)
		//				println(s"Failure @ `$spot`: $msg")
		Console.out.println(s"`: $msg")
	}
}
