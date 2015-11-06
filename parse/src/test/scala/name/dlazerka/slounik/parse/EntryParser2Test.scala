package name.dlazerka.slounik.parse

import EntryParser2._
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

	"абагравальны" in {
		val result = EntryParser2.parseLine(
			"""|<b>абагравальны</b> — обогревательный; отопительный (<i>сезон</i>)
			   |""".stripMargin).head

		result shouldBe Entry("абагравальны", Seq("обогревательный", "отопительный"))
	}

//	"трое" in {
//		val result = EntryParser2.parseLine(
//			"""|<b>трое</b> <i>(с сущ. муж.)</i> тры, <i>род.</i> трох;
//			   | <i>(с сущ. муж. и жен., вместе взятыми, с сущ. общего рода, с сущ., употребляющимися
//			   | только во мн., с сущ., обозначающими детей и детёнышей, с личными мест. мн.)</i> трое,
//			   | <i>род.</i> траіх<br>
//			   |<b>трое товарищей</b> тры таварышы<br>
//			   |<b>их было трое - двое мужчин и одна женщина</b> іх было трое - два мужчыны і адна жанчына<br>
//			   |<b>у них трое детей</b> у іх трое дзяцей<br>
//			   |<b>трое котят</b> трое кацянят<br>
//			   |<b>трое суток</b> трое сутак
//			   |""".stripMargin)
//
//		result shouldBe Seq("тры") // TODO
//	}
//
//	"урок" in {
//		val result = EntryParser2.parseLine(
//			"""|<b>урок</b> <i>в разн. знач.</i> урок, -ка <i>муж.</i><br>
//		       |<b>урок белорусского языка</b> урок беларускай мовы<br>
//		       |<b>это послужит ему уроком</b> гэта паслужыць (будзе) яму урокам<br>
//		       |<b>брать уроки (чего-либо у кого-либо)</b> браць урокі (чаго-небудзь у каго-небудзь)<br>
//		       |<b>давать уроки (где-либо, кому-либо)</b> даваць урокі (дзе-небудзь, каму-небудзь)
//		       |""".stripMargin)
//		result shouldBe Seq("урок") // TODO
//	}
//

	"debug" in {
		val line =
			"""<i>(с сущ. муж.)</i> тры"""

		parseAll(hint ~ word, line) match {
			case Success(matched, input) =>
				println(matched)
			case NoSuccess(msg, input) =>
				Console.out.print("Failure @ `")
				Console.out.print(line.slice(input.offset - 10, input.offset))
				Console.err.print(line.slice(input.offset, input.offset + 1))
				Console.out.print(line.slice(input.offset, input.offset + 10))
				Console.out.println("`")
				fail(msg)
		}
	}
}
