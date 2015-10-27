package me.lazerka.slounik.parse

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets._
import java.nio.file.Path
import java.nio.file.StandardOpenOption._
import java.util.concurrent.TimeUnit

import com.google.common.base.Stopwatch
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.parallel.ParMap

/**
 * @author Dzmitry Lazerka
 */
class Writer(outFile: Path, langsSorted: String, fromLang: String, dictCode: String) {
	val logger: Logger = LoggerFactory.getLogger(this.getClass)

	def write(parsed: ParMap[String, (Array[String], String)]) = {
		val fileChannel = FileChannel.open(outFile, WRITE, CREATE)
		val stopwatch = Stopwatch.createStarted()

		try {
			val result: String = parsed
					.map(el => makeOutputLine(el._1, el._2._1, el._2._2))
					.reduce((line1, line2) => line1 + '\n' + line2)

			logger.info("Converted to string in {}ms", stopwatch.elapsed(TimeUnit.MILLISECONDS))
			stopwatch.reset().start()

			fileChannel.write(ByteBuffer.wrap(result.getBytes(UTF_8)))
		} finally {
			fileChannel.close()
		}

		logger.info("Written to {} in {}ms", outFile.toAbsolutePath, stopwatch.elapsed(TimeUnit.MILLISECONDS))
	}

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
}
