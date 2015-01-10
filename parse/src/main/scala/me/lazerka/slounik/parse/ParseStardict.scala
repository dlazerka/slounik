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
 * This code is kinda dirty, but that's OK.
 *
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

	val lemmaPattern = "[А-Яа-яЎўІіЁё]|([А-Яа-яЎўІіЁё][а-яўіё' -]*[а-яўіё'!-])".r
	val lemmaDeclarationPattern = s"$lemmaPattern( \\(?$lemmaPattern(, $lemmaPattern)*\\)?)?".r

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

		val fromLang = args(0)
		val toLang = args(1)
		val langsSorted = Array(fromLang, toLang).sorted.reduce(_ + _)

		val dir = new File(args(2))
		assert(dir.isDirectory)
		traverse(dir)

		def traverse(dir: File): Unit = {
			val nodes = dir.listFiles()

			nodes.filter(node => node.isFile && node.getName.endsWith(".dict.dz"))
					.map(processDict)

			nodes.filter(_.isDirectory)
					.map(traverse)
		}
		logger.info("Parsed all in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))

		def processDict(dictFile: File) {
			logger.info("Parsing {}", dictFile.getAbsolutePath)
			val stopwatch = Stopwatch.createStarted()
			val dictCode = dictFile.getName.replace(".dict.dz", "")

			val dictContent: Array[Byte] = readDictFile(dictFile)

			val idxFilePath = dictFile.getPath.replace(".dict.dz", ".idx")
			val idx = readIdxFile(idxFilePath)

			val resultFilePath = dictFile.getPath.replace(".dict.dz", ".slounik")
			val resultFile = new File(resultFilePath)
			if (resultFile.exists()) {
				logger.info("File {} exists, skipping", resultFile.getAbsolutePath)
				return
			}

			val lines = mutable.HashMap.empty[Array[String], String]
			var i = 0
			while (i < idx.size) {
				val lemmaBytes = idx.slice(i, idx.indexOf('\u0000', i))
				i += lemmaBytes.length + 1

				val dict_offset = readInt(idx.slice(i, i + 4))
				i += 4
				val dict_size = readInt(idx.slice(i, i + 4))
				i += 4
				val lineBytes = dictContent.slice(dict_offset, dict_offset + dict_size)
				val line = new String(lineBytes, utf8)

				val lemmaString = new String(lemmaBytes, utf8)
				val lemmas = lemmaString
						.split('(')
						.flatMap(_.split(','))
						.map(_.replace(')', ' ').trim)
						.filter(lemma => {
					if (lemmaPattern.pattern.matcher(lemma).matches()) {
						true
					} else {
						println(s"Skipped lemma: $lemma")
						false
					}
				})
				lines.put(lemmas, line)
			}
			logger.info("Read {} lines in {}ms", lines.size, stopwatch.elapsed(TimeUnit.MILLISECONDS))

			def makeOutputLine(lemma: String, translations: Array[String], line: String): String = {
				try {
					assume(!line.contains('↵'), line) // lines separator
					val line2 = line.replace('\n', '↵')
					assume(!lemma.contains('|'), line) // Separates parts of key
					assume(!translations.exists(_.contains('|')), translations) // Separates translations from dictCode
					assume(!translations.exists(_.contains('&')), translations) // Separates translations from each other

					val to: String = translations.reduce(_ + '&' + _)

					val key: String = s"$langsSorted|$lemma|$fromLang|$to|$dictCode"
					assume(key.length < 500, line) // due to GAE restriction on key length
					s"$key:$line2"
				} catch {
					case e: Exception =>
						logger.error(s"Unable to make output line of: $line")
						throw e
				}
			}

			stopwatch.reset().start()

			val parsed = lines
					.par // With par it's 3x-10x faster
					.mapValues(parseLine)
					.filter(_._2._1.nonEmpty)
					.flatMap(line => line._1.map(lemma => (lemma, line._2))) // flatten keys
			if (parsed.isEmpty) {
				logger.warn("Nothing parsed from {}", dictFile.getAbsolutePath)
				return
			}
			val result = parsed
					.map(el => makeOutputLine(el._1, el._2._1, el._2._2))
					.reduce((line1, line2) => line1 + '\n' + line2)

			logger.info("Converted to string in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))
			stopwatch.reset().start()

			assert(!resultFile.exists(), s"Output file already exists: $resultFile")
			val fw = new FileWriter(resultFile)
			try {
				fw.write(result)
			} finally {
				fw.close()
			}
			logger.info("Written to {} in {}ms", resultFile.getName, stopwatch.elapsed(TimeUnit.MILLISECONDS))
		}
	}

	def readDictFile(file: File): Array[Byte] = {
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
