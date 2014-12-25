package name.dlazerka.slounik

class QueryException(message: String) extends Exception(message) {
	def this() = this(null)
}
