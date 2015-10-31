package me.lazerka.slounik.parse

import me.lazerka.slounik.parse.EntryParser2._
import org.scalatest.{FreeSpec, Matchers}

/**
 * @author Dzmitry Lazerka
 */
//noinspection UnitMethodIsParameterless
class EntryParser2Test extends FreeSpec with Matchers {
		"встрепать" in {
			val result = EntryParser2.parseLine("<b>встрепать</b> — ускудлаціць, узлахмаціць").head
			result shouldBe Entry("встрепать", Seq("ускудлаціць", "узлахмаціць"))
		}

		"папячы" in {
			val result = EntryParser2.parseLine(
				"""|<b>папячы</b> —
	               | 1) испечь, изжарить (<i>долго, неоднократно</i>));
	               | 2) обжечь, опалить (<i>огнём, солнцем</i>);
	               | 3) попечь, пожарить (<i>некоторое время</i>)
	               |""".stripMargin).head
			result shouldBe Entry("папячы", Seq(
				"испечь",
				"изжарить",
				"обжечь",
				"опалить",
				"попечь",
				"пожарить"))
		}

		"асадка" in {
			val result = EntryParser2.parseLine(
				"""|<b>асадка</b> —
			       | 1) насадка, присадка;
			       | 2) оправка, обрамление;
			       | 3) установка;
			       | 4) <i>уст.</i> ручка <i>(письменная принадлежность</i>)
			       |""".stripMargin).head
			result shouldBe Entry("асадка", Seq(
				"насадка",
				"присадка",
				"оправка",
				"обрамление",
				"установка",
				"ручка"))
		}

		"кіраванне" in {
			val result = EntryParser2.parseLine(
				"""|<b>кіраванне</b> — 1) управление; 2) руководство; правление 3) <i>лінгв.</i> управление
		           |""".stripMargin).head

			result shouldBe Entry("кіраванне", Seq(
				"управление",
				"руководство",
				"правление"))
		}

		"ух" in {
			val result = EntryParser2.parseLine(
	            """|<b>ух</b> <i>межд.</i><br>
				   |<b>1.</b> <i>(при выражении восхищения, удивления)</i> ух<br>
				   |<b>2.</b> <i>(при выражении чувства усталости и других чувств)</i> ух<br><b>ух, как жарко</b>
				   | ух, як горача<br>
				   |<b>3.</b> <i>(при выражении резкого низкого звука от удара, выстрела)</i> ух<br>
				   |<b>ух! - раздался глухой удар</b> ух! - раздаўся глухі ўдар
		           |""".stripMargin).head

			result shouldBe Entry("ух", Seq("ух"))
		}

		"debug" in {
			val line =
				"""|<b>3.</b> <i>(при выражении резкого низкого звука от удара, выстрела)</i> ух <br>
 				   |<b>ух! - раздался глухой удар</b> ух! - раздаўся глухі ўдар
				   |""".stripMargin

			parseAll(variantB, line) match {
				case Success(matched, input) =>
					println(matched)
				case NoSuccess(msg, next) =>
					fail(msg)
			}
		}
}
