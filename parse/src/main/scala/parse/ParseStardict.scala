package parse

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets

import org.slf4j.{Logger, LoggerFactory}
import sun.misc.IOUtils

import scala.collection.mutable
import scala.util.matching.Regex

/**
  * Processes all .html files:
  * Searches for strings like
  * {{{
  * &lt;script src="lib/jquery.js" predeploy="//googleapis.cdn.com/jquery.min.js"&gt;
  * }}}
  * and replaces `src` with `predeploy`.
  *
  * @author Dzmitry Lazerka
  */
object ParseStardict {
	val logger: Logger = LoggerFactory.getLogger(this.getClass)
	val utf8 = StandardCharsets.UTF_8

	private val WORD_CAP = "[А-ЯЎІЁ][а-яўіё'-]+"
	private val WORD_NOCASE = "[0-9A-Za-zА-ЯЎІЁа-яўіё'!.-]+"

	private val MULTIWORD_COMMAS = "(?:" + WORD_NOCASE + "(?:[ ,]+))+" + WORD_NOCASE
	private val MULTIWORD = "(?:" + WORD_NOCASE + "(?:[ ]+))+" + WORD_NOCASE
	private val DROP = Array(
		"""\([^\)]+?\)""",
		"""<i>род\\.</i> [^ <]+""",
		"""<b>\s*""" + MULTIWORD_COMMAS + """\s*</b>.*?(?:<br>|$)""",
		"<b>.*?</b>", "<i>.*?</i>", " -[^ ,;<]+",
		"""(?s)\(.*?\)""",
		"(?s)<a .*?</a>",
		" +-+ +",
		MULTIWORD,
		"<br>",
		"""[а-я0-9]\)""",
		"[0-9,;:◊!?.«»°—]+",
		"[0-9°]+",
		WORD_CAP
	)
	private val DROP_PATTERNS = DROP.map(_.r())


	def main(args: Array[String]) = {
		assert(args.length == 2)
		assert(args(0).endsWith(".dict.dz"))
		assert(args(1).endsWith(".idx"))

		val dict = readFile(args(0))
		val idxIterator = readFile(args(1)).iterator

		val lines = mutable.HashMap.empty[String, String]
		while (idxIterator.hasNext) {
			val lemmaBytes = idxIterator.takeWhile(_ != 0).toArray
			val lemma = new String(lemmaBytes, utf8)

			val dict_offset = readInt(idxIterator)
			val dict_size = readInt(idxIterator)
			val lineBytes = dict.slice(dict_offset, dict_offset + dict_size).toArray
			lines.put(lemma, new String(lineBytes, utf8))
		}

		val parsed = lines
				.par // todo test whether matters
				.mapValues(parseLine)
		parsed
	}

	/** Reads 4 bytes and converts them to int. */
	def readInt(iter: Iterator[Byte]): Int = {
		try {
			iter.take(4).foldLeft(0)((a, b) => (a << 8) | (b & 0x7f) | (b & 0x80))
		} catch {
			case e: NoSuchElementException => throw new RuntimeException("EOF while integer expected", e)
		}
	}

	def readFile(fileName: String): Array[Byte] = {
		val file = new File(fileName)
		assert(file.canRead)
		logger.info("Parsing {}", file.getAbsolutePath)

		val is = new FileInputStream(file)
		try {
			IOUtils.readFully(is, file.length().toInt, true)
		} finally {
			is.close()
		}
	}

	val underline = "<u>|</u>".r
	var spaces = "\\s+".r
	def parseLine(line: String): Array[String] = {
		val cleared1 = underline.replaceAllIn(line, "")
		val cleared2 = DROP_PATTERNS
				.foldLeft(cleared1) { (c: String, p: Regex) => p.replaceAllIn(c, " ")}
				.trim
		val lemmas = spaces.split(cleared2)
				.filter(!_.isEmpty)
		lemmas.foreach(lemma => assert(lemma.matches("[А-Яа-яЎўІіЁё][а-яўіё'-]*")))

		lemmas
	}
}
