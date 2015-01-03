package me.lazerka.slounik.parse

import java.io.{File, FileInputStream, FileWriter}
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

import com.google.common.base.Stopwatch
import org.slf4j.{Logger, LoggerFactory}
import sun.misc.IOUtils

import scala.collection.mutable
import scala.collection.parallel.{ParIterable, ParMap}
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

	val lemmaPattern = "[А-Яа-яЎўІіЁё][а-яўіё'-]*".r

	/**
	 * Usage: ParseStardict <path to .dict.dz file>
	 * @param args
	 */
	def main(args: Array[String]) = {
		val stopwatch = Stopwatch.createStarted()
		assert(args.length == 1)
		assert(args(0).endsWith(".dict.dz"))

		val dictFilePath = args(0)
		val idxFilePath = dictFilePath.replace(".dict.dz", ".idx")
		val resultFilePath = dictFilePath.replace(".dict.dz", ".slounik")

		unzip here
		val dict = readFile(dictFilePath)
		val idx = readFile(idxFilePath)
		val idxIterator = idx.iterator

		val lines = mutable.HashMap.empty[String, String]
		while (idxIterator.hasNext) {
			val lemmaBytes = idxIterator.takeWhile(_ != 0).toArray
			val lemma = new String(lemmaBytes, utf8)
			assert(lemmaPattern.pattern.matcher(lemma).matches())

			idxIterator.next()
			idxIterator.next()
			idxIterator.next()
			val dict_offset = readInt(idxIterator)
			val dict_size = readInt(idxIterator)
			val lineBytes = dict.slice(dict_offset, dict_offset + dict_size).toArray
			lines.put(lemma, new String(lineBytes, utf8))
		}
		logger.info("Read {} lines in {}ms", lines.size, stopwatch.elapsed(TimeUnit.MILLISECONDS))
		stopwatch.reset().start()

		val parsed: ParMap[String, Array[String]] = lines
				.par // todo test whether par matters
				.mapValues(parseLine)
		val strings: ParIterable[String] = parsed
				.mapValues(_.reduce((a, b) => a + '\t' + b))
				.map(el => el._1 + ',' + el._2)
		val result = strings
				.reduce((line1, line2) => line1 + '↔' + line2 + '\n')

		logger.info("Converted to string in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))
		stopwatch.reset().start()

		val file = new File(resultFilePath)
		assert(!file.exists())
		val fw = new FileWriter(file)
		try {
			fw.write(result)
		} finally {
			fw.close()
		}
		logger.info("Written to {} in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))
	}

	/** Reads 4 bytes and converts them to int. */
	def readInt(iter: Iterator[Byte]): Int = {
		try {
			val took = iter.take(4).toArray
			took.foldLeft(0)((a, b) => (a << 8) | (b & 0x7f) | (b & 0x80))
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
	val spaces = "\\s+".r
	val wordCap = "[А-ЯЎІЁ][а-яўіё'-]+"
	val wordNocase = "[0-9A-Za-zА-ЯЎІЁа-яўіё'!.-]+"
	val multiWordCommas = "(?:" + wordNocase + "(?:[ ,]+))+" + wordNocase
	val multiword = "(?:" + wordNocase + "(?:[ ]+))+" + wordNocase
	val drop = Array(
		"""\([^\)]+?\)""",
		"""<i>род\.</i> [^ <]+""",
		"""<b>\s*""" + multiWordCommas + """\s*</b>.*?(?:<br>|$)""",
		"<b>.*?</b>",
		"<i>.*?</i>",
		" -[^ ,;<]+",
		"""(?s)\(.*?\)""",
		"(?s)<a .*?</a>",
		" +-+ +",
		multiword,
		"<br>",
		"""[а-я0-9]\)""",
		"[0-9,;:◊!?.«»°—]+",
		"[0-9°]+",
		wordCap
	)
	val dropPatterns = drop.map(_.r())

	def parseLine(line: String): Array[String] = {
		val cleared1 = underline.replaceAllIn(line, "")
		val cleared2 = dropPatterns
				.foldLeft(cleared1) { (c: String, p: Regex) => p.replaceAllIn(c, " ")}
				.trim
		val lemmas = spaces.split(cleared2)
				.filter(!_.isEmpty)
				.distinct
		lemmas.foreach(lemma => assert(lemmaPattern.pattern.matcher(lemma).matches()))

		lemmas
	}
}
