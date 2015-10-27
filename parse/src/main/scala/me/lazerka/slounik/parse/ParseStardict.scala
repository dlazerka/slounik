package me.lazerka.slounik.parse

import java.io.ByteArrayInputStream
import java.nio.{BufferUnderflowException, ByteBuffer}
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.StandardOpenOption.{CREATE_NEW, WRITE}
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
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

	val lemmaPattern = "[А-Яа-яЎўІіЁё]|([А-Яа-яЎўІіЁё][а-яўіё' -]*[а-яўіё'!-])".r
	val lemmaDeclarationPattern = s"$lemmaPattern( \\(?$lemmaPattern(, $lemmaPattern)*\\)?)?".r

	/**
	 * Usage: ParseStardict &lt;langFrom&gt; &lt;langTo&gt; &lt;directory&gt;
	 */
	def main(args: Array[String]) = {
		val stopwatch = Stopwatch.createStarted()
		assert(args.length == 3)
		assert(args(0).matches("[a-z][a-z]"))
		assert(args(1).matches("[a-z][a-z]"))

		val fromLang = args(0)
		val toLang = args(1)
		val langsSorted = Array(fromLang, toLang).sorted.reduce(_ + _)

		val path = FileSystems.getDefault.getPath(args(2))

		assert(Files.isDirectory(path))
		Files.walkFileTree(path, new SimpleFileVisitor[Path] {
			override def visitFile(file: Path, attrs: BasicFileAttributes) = {
				super.visitFile(file, attrs)

				if (file.toString.endsWith(".dict.dz")) {
					processDict(file)
				}

				FileVisitResult.CONTINUE
			}
		})

		logger.info("Parsed all in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))

		def processDict(dictFile: Path) {
			logger.info("Parsing dict {}", dictFile)
			val stopwatch = Stopwatch.createStarted()
			val dictCode = dictFile.getFileName.toString.replace(".dict.dz", "")

			val dictContent: ByteBuffer = readDictFile(dictFile)

			val idxFilePath = dictFile.getParent.resolve(dictCode + ".idx")
			assert(Files.isReadable(idxFilePath), idxFilePath)
			logger.info("Parsing idx {}", idxFilePath.toAbsolutePath)
			val idx = ByteBuffer.wrap(Files.readAllBytes(idxFilePath))
				.asReadOnlyBuffer()

			val resultFile = dictFile.getParent.resolve(dictCode + ".slounik")
			if (Files.exists(resultFile)) {
				logger.info("File {} exists, skipping", resultFile.toAbsolutePath)
				return
			}

			val lines = mutable.HashMap.empty[Array[String], String]
			while (idx.hasRemaining) {
				// Reading lemma
				val lemmaBuffer = idx.slice()
				while (idx.get != 0) lemmaBuffer.get
				lemmaBuffer.flip()
				val lemmaBytes = new Array[Byte](lemmaBuffer.limit())
				lemmaBuffer.get(lemmaBytes)
				val lemma = new String(lemmaBytes, UTF_8)

				// Reading translation
				val dictOffset = idx.getInt
				val dictSize = idx.getInt
				dictContent.position(dictOffset)
				val lineBytes = new Array[Byte](dictSize)
				try {
					dictContent.get(lineBytes)
				} catch {
					case e: BufferUnderflowException => throw new Exception(
						s"EOF while reading lemma $lemma, offset/size must be $dictOffset/$dictSize")
				}
				val line = new String(lineBytes, UTF_8)

				val lemmas = lemma
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
				logger.warn("Nothing parsed from {}", dictFile.toAbsolutePath)
				return
			}
			val result: String = parsed
					.map(el => makeOutputLine(el._1, el._2._1, el._2._2))
					.reduce((line1, line2) => line1 + '\n' + line2)

			logger.info("Converted to string in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))
			stopwatch.reset().start()


			val fileChannel = FileChannel.open(resultFile, WRITE, CREATE_NEW)
			fileChannel.write(ByteBuffer.wrap(result.getBytes(UTF_8)))
			fileChannel.close()
			logger.info("Written to {} in {}ms", resultFile.toAbsolutePath, stopwatch.elapsed(TimeUnit.MILLISECONDS))
		}
	}

	def readDictFile(file: Path): ByteBuffer = {
		assert(Files.isReadable(file), file.toAbsolutePath)

		val stream = new ByteArrayInputStream(Files.readAllBytes(file))
		val zis = new GZIPInputStream(stream)
		try {
			ByteBuffer.wrap(IOUtils.readFully(zis, -1, true)).asReadOnlyBuffer()
		} finally {
			zis.close()
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
