package me.lazerka.slounik.parse

import java.io.{File, FileInputStream, FileWriter}
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

import com.google.common.base.Stopwatch
import org.slf4j.{Logger, LoggerFactory}
import sun.misc.IOUtils

import scala.collection.mutable

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

	val lemmaPattern = "([А-Яа-яЎўІіЁё][а-яўіё'-]*)".r
	val lemmaDoublePattern = s"$lemmaPattern \\($lemmaPattern\\)".r
	val lemmaTriplePattern = s"$lemmaPattern \\($lemmaPattern, $lemmaPattern\\)".r

	def readDictFile(dictFilePath: String): Array[Byte] = {
		val file: File = new File(dictFilePath)
		assert(file.canRead, file.getAbsolutePath)

		val zis = new GZIPInputStream(new FileInputStream(file))
		try {
			IOUtils.readFully(zis, -1, true)
		} finally {
			zis.close()
		}
	}

	def readIdxFile(fileName: String): Array[Byte] = {
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

	/**
	 * Usage: <pre>
	 *     ParseStardict <path to .dict.dz file>
	 * </pre>
	 */
	def main(args: Array[String]) = {
		val stopwatch = Stopwatch.createStarted()
		assert(args.length == 3)
		assert(args(0).matches("[a-z][a-z]"))
		assert(args(1).matches("[a-z][a-z]"))
		assert(args(2).endsWith(".dict.dz"))

		val fromLang = args(0)
		val toLang = args(1)
		val langsSorted = Array(fromLang, toLang).sorted.reduce(_ + _)

		val dictFilePath = args(2)
		val dictContent: Array[Byte] = readDictFile(dictFilePath)

		val idxFilePath = dictFilePath.replace(".dict.dz", ".idx")
		val idx = readIdxFile(idxFilePath)

		val resultFilePath = dictFilePath.replace(".dict.dz", ".slounik")

		val lines = mutable.HashMap.empty[String, String]
		var i = 0
		while (i < idx.size) {
			val lemmaBytes = idx.slice(i, idx.indexOf('\0', i))
			i += lemmaBytes.length + 1

			val dict_offset = readInt(idx.slice(i, i + 4))
			i += 4
			val dict_size = readInt(idx.slice(i, i + 4))
			i += 4
			val lineBytes = dictContent.slice(dict_offset, dict_offset + dict_size)
			val line = new String(lineBytes, utf8)

			val lemma = new String(lemmaBytes, utf8)
			lemma match {
				case lemmaPattern(one) =>
					lines.put(one, line)
				case lemmaDoublePattern(first, second) =>
					lines.put(first, line)
					lines.put(second, line)
				case lemmaTriplePattern(first, second, third) =>
					lines.put(first, line)
					lines.put(second, line)
					lines.put(third, line)
				case _ =>
					logger.warn("Skipped {} at {}", lemma, i)
					i += 8
			}
		}
		logger.info("Read {} lines in {}ms", lines.size, stopwatch.elapsed(TimeUnit.MILLISECONDS))

		def makeOutputLine(from: String, toLemmas: Array[String], line: String): String = {
			try {
				assume(!line.contains('↵'), line)
				val line2 = line.replace('\n', '↵')

				val to: String = toLemmas.reduce(_ + '↔' + _)

				s"$langsSorted|$from|$fromLang→$to:$line2"
			} catch {
				case e: Exception =>
					logger.error(s"Unable to make output line of: $line")
					throw e
			}
		}

		stopwatch.reset().start()

		val result = lines
				.par // With par it's 3x faster
				.mapValues(parseLine)
				.filter(_._2._1.nonEmpty)
				.map(el => makeOutputLine(el._1, el._2._1, el._2._2))
				.reduce((line1, line2) => line1 + '\n' + line2)

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
		logger.info("Written to {} in {}ms", file.getName, stopwatch.elapsed(TimeUnit.MILLISECONDS))
	}


	/** Reads 4 bytes and converts them to int. */
	def readInt(took: Array[Byte]): Int = {
		try {
			// Not using take() as it breaks iterator, see doc.
			took.foldLeft(0)((a, b) => (a << 8) | (b & 0x7f) | (b & 0x80))
		} catch {
			case e: NoSuchElementException => throw new RuntimeException("EOF while integer expected", e)
		}
	}

	val underline = "<u>|</u>".r
	val spaces = "\\s+".r
	val wordCap = "[А-ЯЎІЁ][а-яўіё'-]+"
	val wordNocase = "[0-9A-Za-zА-ЯЎІЁа-яўіё'!.–-]+"
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
		"""[а-я0-9]\)""",
		multiword,
		"<br>",
		"[0-9,;:◊!?.«»°—]+",
		"[0-9°]+",
		wordCap
	)
	val dropPatterns = drop.map(_.r())

	def parseLine(line: String): (Array[String], String) = {
		var cleared = underline.replaceAllIn(line, "")

		// Not foldLeft() for debugging.
		dropPatterns.foreach(regex => {
			cleared = regex.replaceAllIn(cleared, " ")
		})
		val lemmas = spaces.split(cleared.trim)
				.filter(!_.isEmpty)
				.distinct
		if (lemmas.isEmpty) {
			println(s"Nothing parsed from line $line")
		}
		val result = lemmas.filter ({
			case lemmaPattern(_*) => true
			case lemma =>
				println(s"Skipped $lemma in $line")
				false
		})
		(result, line)
	}
}
