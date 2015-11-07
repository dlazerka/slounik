package name.dlazerka.slounik.parse

import name.dlazerka.slounik.parse.SkarnikParser._
import org.scalatest.{FreeSpec, Matchers}

import scala.util.parsing.input.CharSequenceReader

/**
 * @author Dzmitry Lazerka
 */
//noinspection UnitMethodIsParameterless
class SkarnikParserTest extends FreeSpec with Matchers {

	"debug" in {
		val contents = "Аахен\r\n\t[m1][i][c][p]г.[/p][/c][/i] [trn]Аахен[/trn], [i][com][!trs]-на[/!trs][/com] " +
				"[c][p]муж.[/p][/c][/i][/m]\r\nаахенский\r\n\t[m1][trn]аахенскі[/trn][/m]"
		val result = meaning(new CharSequenceReader(contents))
		result match {
			case Success(matched, input) =>
				matched
			case Failure(msg, input: Input) =>
				printFailure(msg, contents, input.offset)
				fail()
			case Error(msg, input: Input) =>
				printFailure(msg, contents, input.offset)
				throw new Exception(msg)
		}
	}
}
