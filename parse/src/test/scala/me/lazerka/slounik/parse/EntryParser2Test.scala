package me.lazerka.slounik.parse

import org.scalatest.{Matchers, FlatSpec}

/**
 * @author Dzmitry Lazerka
 */
class EntryParser2Test extends FlatSpec with Matchers {
	"parseLine" should "parse simple lines" in {
		val line = "<b>встрепать</b> — ускудлаціць, узлахмаціць"

		val result = EntryParser2.parseLine(line)
		result.isDefined shouldBe true
		result.get shouldBe Entry(Lemma("встрепать"), Seq(Lemma("ускудлаціць"), Lemma("узлахмаціць")))
	}

	"parseLine" should "parse папячы" in {
		val line = """|<b>папячы</b> —
		            | 1) испечь, изжарить (<i>долго, неоднократно</i>));
		            | 2) обжечь, опалить (<i>огнём, солнцем</i>);
		            | 3) попечь, пожарить (<i>некоторое время</i>)
		            |""".stripMargin

		val result = EntryParser2.parseLine(line)
		result.isDefined shouldBe true
		result.get shouldBe Entry(Lemma("папячы"), Seq(
			Lemma("испечь"),
			Lemma("изжарить"),
			Lemma("обжечь"),
			Lemma("опалить"),
			Lemma("попечь"),
			Lemma("пожарить")))
	}
}
