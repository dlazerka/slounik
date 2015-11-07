package name.dlazerka.slounik.parse

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes
import java.nio.{BufferUnderflowException, ByteBuffer}
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.zip.GZIPInputStream

import com.google.common.base.Stopwatch
import sun.misc.IOUtils

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
object StardictConverter {
	/**
	 * Usage: StardictConverter &lt;langFrom&gt; &lt;langTo&gt; &lt;directory&gt;
	 */
	def main(args: Array[String]) = {
		assert(args.length == 3)
		assert(args(0).matches("[a-z][a-z]"))
		assert(args(1).matches("[a-z][a-z]"))

		val fromLang = args(0)
		val toLang = args(1)
		val langsSorted = Array(fromLang, toLang).sorted.mkString
		val converter = new StardictConverter(langsSorted, fromLang, toLang)

		val path = FileSystems.getDefault.getPath(args(2))

		val stopwatch = Stopwatch.createStarted()

		assert(Files.isDirectory(path), path.toString)
		Files.walkFileTree(path, new SimpleFileVisitor[Path] {
			override def visitFile(file: Path, attrs: BasicFileAttributes) = {
				super.visitFile(file, attrs)

				if (file.toString.endsWith(".dict.dz")) {
					converter.processDict(file)
				}

				FileVisitResult.CONTINUE
			}
		})

		println(s"Total ${stopwatch.elapsed(MILLISECONDS)}ms")
	}
}

class StardictConverter(langsSorted: String, fromLang: String, toLang: String) {
	def processDict(dictFile: Path) {
		println(s"Parsing dict $dictFile")
		val stopwatch = Stopwatch.createStarted()
		val dictCode = dictFile.getFileName.toString.dropRight(".dict.dz".length)

		val dictContent: ByteBuffer = readDictFile(dictFile)

		val idxFilePath = dictFile.getParent.resolve(dictCode + ".idx")
		assert(Files.isReadable(idxFilePath), idxFilePath)
		println(s"Parsing idx ${idxFilePath.toAbsolutePath}")
		val idx = ByteBuffer.wrap(Files.readAllBytes(idxFilePath))
				.asReadOnlyBuffer()

		val linesBuilder = Vector.newBuilder[(String, String)]
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

			linesBuilder += ((lemma, line))
		}
		val lines = linesBuilder.result()
		println(s"Read ${lines.size} lines in ${stopwatch.elapsed(MILLISECONDS)}ms")

		val parsed = lines
				.map(pair => (StardictParserRegex.parseLine(pair._2), pair._2))
				.flatMap(pair => pair._1.map(entry => (entry, pair._2)))
		if (parsed.isEmpty) {
			println(s"Nothing parsed from ${dictFile.toAbsolutePath}")
			return
		} else {
			println(s"Parsed ${parsed.size} entries")
		}

		val outFile = dictFile.getParent.resolve(dictCode + ".slounik")
		val writer = new Writer(langsSorted, fromLang, dictCode)

		writer.write(parsed, outFile)
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
