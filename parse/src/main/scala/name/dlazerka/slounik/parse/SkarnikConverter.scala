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

package name.dlazerka.slounik.parse

import java.nio.charset.StandardCharsets
import java.nio.file._
import java.util.concurrent.TimeUnit._

import com.google.common.base.Stopwatch

/**
 * @author Dzmitry Lazerka
 */
object SkarnikConverter  {
	/**
	 * Usage: "SkarnikConverter `langFrom` `langTo` `file.dsl`".
	 */
	def main(args: Array[String]) = {
		assert(args.length == 3)
		assert(args(0).matches("[a-z][a-z]"))
		assert(args(1).matches("[a-z][a-z]"))

		val fromLang = args(0)
		val toLang = args(1)
		val langsSorted = Array(fromLang, toLang).sorted.mkString
//		val converter = new StardictConverter(langsSorted, fromLang, toLang)

		val path = FileSystems.getDefault.getPath(args(2))

		assert(path.toString endsWith ".dsl")
		val dictCode = path.getFileName.toString.dropRight(".dsl".length)

		println(s"Parsing ${path.toAbsolutePath}")

		val stopwatch = Stopwatch.createStarted()

		val bytes = Files.readAllBytes(path)
		val contents = new String(bytes, StandardCharsets.UTF_16)
		println(s"Read in ${stopwatch.elapsed(MILLISECONDS)}ms")

		val entries = SkarnikParser.parse(contents)

		val outPath = path.getParent.resolve(dictCode + ".slounik")
		val writer = new Writer(langsSorted, fromLang, dictCode)
		writer.write(entries.toVector.map(e => (e, "")), outPath)

		println(s"Total ${stopwatch.elapsed(MILLISECONDS)}ms")
	}
}
