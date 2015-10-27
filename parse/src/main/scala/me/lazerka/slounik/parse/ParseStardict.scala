package me.lazerka.slounik.parse

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.{BufferUnderflowException, ByteBuffer}
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

			val outFile = dictFile.getParent.resolve(dictCode + ".slounik")
			val writer = new Writer(outFile, langsSorted, fromLang, dictCode)

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
					if (EntryParser.lemmaPattern.pattern.matcher(lemma).matches()) {
						true
					} else {
						println(s"Skipped lemma: $lemma")
						false
					}
				})
				lines.put(lemmas, line)
			}
			logger.info("Read {} lines in {}ms", lines.size, stopwatch.elapsed(TimeUnit.MILLISECONDS))

			stopwatch.reset().start()

			val parsed = lines
					.par // With par it's 3x-10x faster
					.mapValues(EntryParser.parseLine)
					.filter(_._2._1.nonEmpty)
					.flatMap(line => line._1.map(lemma => (lemma, line._2))) // flatten keys
			if (parsed.isEmpty) {
				logger.warn("Nothing parsed from {}", dictFile.toAbsolutePath)
				return
			}

			writer.write(parsed)
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
}
