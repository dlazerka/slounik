package me.lazerka.slounik.parse

import me.lazerka.slounik.parse.EntryParser2._
import org.scalatest.{FreeSpec, Matchers}

/**
 * @author Dzmitry Lazerka
 */
//noinspection UnitMethodIsParameterless
class EntryParser2Test extends FreeSpec with Matchers {
		"встрепать" in {
			val result = EntryParser2.parseLine("<b>встрепать</b> — ускудлаціць, узлахмаціць")
			result.isDefined shouldBe true
			result.get shouldBe Entry(Phrase("встрепать"), Set(Phrase("ускудлаціць"), Phrase("узлахмаціць")))
		}

		"папячы" in {
			val result = EntryParser2.parseLine(
				"""|<b>папячы</b> —
	               | 1) испечь, изжарить (<i>долго, неоднократно</i>));
	               | 2) обжечь, опалить (<i>огнём, солнцем</i>);
	               | 3) попечь, пожарить (<i>некоторое время</i>)
	               |""".stripMargin)
			result.isDefined shouldBe true
			result.get shouldBe Entry(Phrase("папячы"), Set(
				Phrase("испечь"),
				Phrase("изжарить"),
				Phrase("обжечь"),
				Phrase("опалить"),
				Phrase("попечь"),
				Phrase("пожарить")))
		}

		"асадка" in {
			val result = EntryParser2.parseLine(
				"""|<b>асадка</b> —
			       | 1) насадка, присадка;
			       | 2) оправка, обрамление;
			       | 3) установка;
			       | 4) <i>уст.</i> ручка <i>(письменная принадлежность</i>)
			       |""".stripMargin)
			result.isDefined shouldBe true
			result.get shouldBe Entry(Phrase("асадка"), Set(
				Phrase("насадка"),
				Phrase("присадка"),
				Phrase("оправка"),
				Phrase("обрамление"),
				Phrase("установка"),
				Phrase("ручка")))
		}

		"кіраванне" in {
			val result = EntryParser2.parseLine(
				"""|<b>кіраванне</b> — 1) управление; 2) руководство; правление 3) <i>лінгв.</i> управление
		           |""".stripMargin)

			result.isDefined shouldBe true
			result.get shouldBe Entry(Phrase("кіраванне"), Set(
				Phrase("управление"),
				Phrase("руководство"),
				Phrase("правление"),
				Phrase("управление")))
		}
/*
		"ух" in {
			val result = EntryParser2.parseLine(
	            """|<b>ух</b> <i>межд.</i><br>
				   |<b>1.</b> <i>(при выражении восхищения, удивления)</i> ух<br>
				   |<b>2.</b> <i>(при выражении чувства усталости и других чувств)</i> ух<br><b>ух, как жарко</b>
				   | ух, як горача<br>
				   |<b>3.</b> <i>(при выражении резкого низкого звука от удара, выстрела)</i> ух<br>
				   |<b>ух! - раздался глухой удар</b> ух! - раздаўся глухі ўдар
		           |""".stripMargin)

			result.isDefined shouldBe true
			result.get shouldBe Entry(Phrase("ух"), Set(
				Phrase("ух"),
				Phrase("руководство"),
				Phrase("правление"),
				Phrase("управление")))
		}
*/
		"debug" in {
			val line =
				"""| <i>лінгв.</i>
				   |""".stripMargin

			parseAll(hint, line) match {
				case Success(matched, input) =>
					println(matched)
				case NoSuccess(msg, next) =>
					fail(msg)
			}

		}
}
