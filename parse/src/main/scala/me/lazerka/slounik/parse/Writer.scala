package me.lazerka.slounik.parse

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets._
import java.nio.file.Path
import java.nio.file.StandardOpenOption._
import java.util.concurrent.TimeUnit

import com.google.common.base.Stopwatch

/**
 * @author Dzmitry Lazerka
 */
class Writer(langsSorted: String, fromLang: String, dictCode: String) {
	def write(parsed: Vector[(Entry, String)], outFile: Path) = {
		val fileChannel = FileChannel.open(outFile, WRITE, TRUNCATE_EXISTING, CREATE)
		val stopwatch = Stopwatch.createStarted()

		try {
			val result = parsed
					.seq
					.map(el => makeOutputLine(el._1, el._2))
					.sorted
					.mkString("\n")

			println(s"Converted to string in ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
			stopwatch.reset().start()

			fileChannel.write(ByteBuffer.wrap(result.getBytes(UTF_8)))
		} finally {
			fileChannel.close()
		}

		println(s"Written to ${outFile.toAbsolutePath} in ${stopwatch.elapsed(TimeUnit.MILLISECONDS)}ms")
	}

	def makeOutputLine(entry: Entry, line: String): String = {
		try {
			assume(!line.contains('↵'), line) // lines separator
			val line2 = line.replace('\n', '↵')
			assume(!entry.lemma.contains('|'), line) // Separates parts of key
			assume(!entry.translations.exists(_.contains('|')), entry) // Separates translations from dictCode
			assume(!entry.translations.exists(_.contains('&')), entry) // Separates translations from each other

			val to: String = entry.translations.mkString("&")

			val key: String = s"$langsSorted|${entry.lemma}|$fromLang|$to|$dictCode"
			assume(key.length < 500, line) // due to GAE restriction on key length
			s"$key:$line2"
		} catch {
			case e: Exception =>
				sys.error(s"Unable to make output line of: $line")
				throw e
		}
	}
}
