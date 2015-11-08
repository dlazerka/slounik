package name.dlazerka.slounik.parse

import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader

/**
 * @author Dzmitry Lazerka
 */
object SkarnikParser extends Parsers {
	type Elem = Char
	type StringP = Parser[String]
	type OptStringP = Parser[Option[String]]
	type Strings = Parser[List[String]]
	type CharP = Parser[Char]

	def str(s: String) = acceptSeq(s.toCharArray)

	val any = elem("any", e => e != '\n' && e != '\r')
	val alpha = elem("alpha", e => e != '\n' && e != '\r' && e != '[')
	val alphaString: StringP = rep1(alpha) ^^ { _.mkString }
	val newline = '\r' ~ '\n' named "newline"
	val commentedLine = rep1('#', alpha) <~ newline named "commentedLine"

	val letter = elem("letter", _.isLetter)
	val digit = elem("digit", _.isDigit)

	def lemmaLine = rep1(letter) <~ newline ^^ { letters => new String(letters.toArray)} named "lemmaLine"

	def lang: StringP = str("[lang id=") ~> digit ~> ']' ~> rep1(alpha) <~ opt(i | c) <~ str("[/lang]") ^^
			{ _.mkString } named "[lang]"
	def trs: OptStringP = str("[!trs]") ~> alphaString <~ str("[/!trs]") ^^ {a => None} named "[!trs]"
	def com: OptStringP = str("[com]") ~> opt(alphaString) ~> repsep(lang | trs, ' ') <~ str("[/com]") ^^
			{a => None } named "[com]"

	// TODO: input contains [/i] that don't match opening [i]. Refactor so that [i] are just ignored.
//	def i = opt(str("[i]") | str("[/i]")) named "i"
	def i: OptStringP = str("[i]") ~> opt(com) ~> opt(' ') <~ opt(c) <~ opt(repsep(p, str(", ") | ' ')) <~ str("[/i]") ^^
			{a => None } named "[i]"

	def b: OptStringP = str("[b]") ~> lang <~ str("[/b]") ^^ {a => None} named "[b]"
	def c: OptStringP = str("[c]") ~> repsep(p | i, str(", ")) <~ str("[/c]") ^^ {a => None} named "[c]"
	def p: OptStringP = str("[p]") ~> alphaString <~ str("[/p]") ^^ {a => None} named "[p]"
	def ex: OptStringP = str("[ex]") ~> b ~> alphaString <~ str("[/ex]") ^^ { a => None } named "[ex]"
	def star: OptStringP = str("[*]") ~> ex <~ str("[/*]") named "[*]"
	def trn: StringP = str("[trn]") ~> alphaString <~ str("[/trn]") <~ opt(str(", ") ~ i) named "[trn]"
	def ref: StringP = str("[ref]") ~> alphaString <~ str("[/ref]") <~ opt(str(", ") ~ i) named "[ref]"

	def explanation: OptStringP = ((i | c) <~ ' ') | star named "explanation"

	def meaningOpen: CharP = '[' ~> 'm' ~> rep1(digit) ~> ']'
	def meaning: Strings = meaningOpen ~> opt(explanation) ~>
			repsep(trn, str(", ")) <~
			opt(explanation) <~ opt(ref) <~ str("[/m]") named
			"meaning"
	def bLine: Strings = str("[b]") ~> rep1(alpha) <~ str("[/b]") <~ opt(' ' <~ rep(any)) ^^
			{ a => List() } named "bLine"

	def tabbedLine: Strings = '\t' ~> (meaning | bLine) <~ newline named "tabbedLine"

	def entry = rep1(lemmaLine) ~ rep1(tabbedLine) ^^ {
		case lemma ~ meanings =>
			val foundMeanings = meanings.flatten
			if (foundMeanings.isEmpty) {
				println(s"No translations found for $lemma")
				Seq.empty
			} else {
				lemma.map(new Entry(_, foundMeanings))
			}
	} named "entry"

	def global = rep1(commentedLine) ~> rep1(entry) ^^ {_.flatten} named "global"

	def parse(contents: String): Seq[Entry] = {
		phrase(global)(new CharSequenceReader(contents)) match {
			case Success(matched, input) =>
				matched
			case Failure(msg, input: Input) =>
				printFailure(msg, contents, input.offset)
				Seq.empty
			case Error(msg, input: Input) =>
				printFailure(msg, contents, input.offset)
				throw new Exception(msg)
		}
	}

	def printFailure(msg: String, contents: String, offset: Int) = {
		println(s"Failure @ $offset: " + replace(msg))
		val regionSize = 50
		println(replace(contents.slice(offset - regionSize, offset + regionSize)))
		val pad = replace(contents.slice(offset - regionSize, offset)).length
		println("^".padTo(pad + 1, " ").reverse.mkString)
	}

	def replace(s: String) = s.replaceAll("\r", "\\\\r").replaceAll("\n", "\\\\n").replaceAll("\t", "\\\\t")

}
