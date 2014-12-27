import java.io._
import java.util.zip.CRC32

import sun.misc.IOUtils

import scala.io.Source

/**
 * Processes all .html files:
 * Searches for strings like
 * {{{
 * &lt;script src="lib/jquery.js" cdn="//googleapis.cdn.com/jquery.min.js"&gt;
 * }}}
 * and replaces `src` with `cdn`.
 *
 * @author Dzmitry Lazerka
 */
object PreDeploy {
	val cdnRegexp = """(src=)"([^"]+)"\s+cdn="([^"]+)"""".r

	def main(args: Array[String]) {
		val workingDir = new File(System.getProperty("user.dir"))
		traverse(workingDir)
	}

	def traverse(dir: File): Unit = {
		val nodes = dir.listFiles()

		var htmls = nodes.filter(node => node.isFile && node.getName.endsWith(".html"))
		htmls.map(processHtml)
		if(htmls.isEmpty) {
			println("No htmls found in " + dir.getAbsolutePath)
		}

		nodes.filter(_.isDirectory)
			.map(traverse)
	}

	def processHtml(htmlFile: File): Unit = {
		assume(htmlFile.length() < Integer.MAX_VALUE)
		assume(htmlFile.canRead)
		val source = Source.fromFile(htmlFile, "UTF-8", htmlFile.length().toInt)
		val content = source.mkString
		source.close()

		var newContent = cdnRegexp.replaceAllIn(content, """$1"$3"""")

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
		val regexp = """(src="([^"]+))\?predeploy-etag"""".r
		val crc32 = new CRC32()

		regexp.replaceAllIn(content, m => {
			val srcFile = new File(htmlFile.getParent, m.group(2))
			val srcIs = new FileInputStream(srcFile)

			val bytes = try {
				IOUtils.readFully(srcIs, srcFile.length().toInt, true)
			} finally {
				srcIs.close()
			}

			crc32.reset()
			crc32.update(bytes)
			val crc = crc32.getValue
			val etag = java.lang.Long.toString(crc, 32)

			m.group(1) + '?' + etag + '"'
		})
	}
}
