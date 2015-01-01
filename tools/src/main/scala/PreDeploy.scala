import java.io._
import java.util.zip.CRC32

import sun.misc.IOUtils

import scala.io.Source

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
object PreDeploy {
	def main(args: Array[String]) {
		val workingDir = new File(System.getProperty("user.dir"))
		println("Traversing " + workingDir.getAbsolutePath)
		traverse(workingDir)
	}

	def traverse(dir: File): Unit = {
		val nodes = dir.listFiles()

		nodes.filter(node => node.isFile && node.getName.endsWith(".html"))
			.map(processHtml)

		nodes.filter(_.isDirectory)
			.map(traverse)
	}

	def processHtml(htmlFile: File): Unit = {
		assume(htmlFile.length() < Integer.MAX_VALUE)
		assume(htmlFile.canRead)
		val source = Source.fromFile(htmlFile, "UTF-8", htmlFile.length().toInt)
		val content = source.mkString
		source.close()

		var newContent = """(src=)"([^"]+)"\s+predeploy="([^"]+)""""
				.r
				.replaceAllIn(content, """$1"$3"""")

		newContent = processEtags(newContent, htmlFile)

		if (newContent.equals(content)) {
			println("Nothing replaced in " + htmlFile.getAbsolutePath)
			return
		}

		val writer = new FileWriter(htmlFile)
		try {
			writer.write(newContent)
		} finally {
			writer.close()
		}

		println("Processed " + htmlFile.getAbsolutePath)
	}

	def processEtags(content: String, htmlFile: File): String = {
		val crc32 = new CRC32()

		val regexp = """"([^"]+)\?crc=[^"]*"""".r
		regexp.replaceAllIn(content, m => {
			val path = m.group(1)
			val srcFile = new File(htmlFile.getParent, path)
			val srcIs = new FileInputStream(srcFile)

			val bytes = try {
				IOUtils.readFully(srcIs, srcFile.length().toInt, true)
			} finally {
				srcIs.close()
			}

			crc32.reset()
			crc32.update(bytes)
			val crc = crc32.getValue // In range [0, 2^32].

			'"' + path + "?crc=" + crc.toHexString + '"'
		})
	}
}
