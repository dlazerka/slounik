/*
 *    Copyright 2015 Dzmitry Lazerka
 *
 *    This file is part of Slounik.
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package name.dlazerka.slounik.tools.build

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
  private val workingDir = System.getProperty("user.dir")

  def main(args: Array[String]) {
    val workingFile = new File(workingDir)
    println("Traversing " + workingFile.getAbsolutePath)
    traverse(workingFile)
  }

  def traverse(dir: File): Unit = {
    val nodes = dir.listFiles()

    nodes.filter(node => node.isFile && node.getName.endsWith(".html"))
      .foreach(processHtml)

    nodes.filter(_.isDirectory)
      .foreach(traverse)
  }

  def processHtml(htmlFile: File): Unit = {
    assume(htmlFile.length() < Integer.MAX_VALUE)
    assume(htmlFile.canRead)
    val source = Source.fromFile(htmlFile, "UTF-8", htmlFile.length().toInt)
    val content = source.mkString
    source.close()

    var newContent =
      """(src=)"([^"]+)"\s+predeploy="([^"]+)""""
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
      val dir = if (path.startsWith("/")) workingDir else htmlFile.getParent
      val srcFile = new File(dir, path)
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
