package parallelfor.internal

private[parallelfor] object PrettyPrint {
  def labels(product: Product): Iterator[String] = product.productElementNames

  def apply(any: Any): String = any match {
    case array: Array[_] =>
      array.map(PrettyPrint.apply).mkString("Array(", ", ", ")")

    case Some(a) => s"Some(${PrettyPrint(a)})"
    case None    => s"None"
    case Nil     => "Nil"

    case set: Set[_] =>
      set.map(PrettyPrint.apply).mkString(s"${className(set)}(", ", ", ")")

    case iterable: Seq[_] =>
      iterable.map(PrettyPrint.apply).mkString(s"${className(iterable)}(", ", ", ")")

    case map: Map[_, _] =>
      val body = map.map { case (key, value) => s"${PrettyPrint(key)} -> ${PrettyPrint(value)}" }
      s"""Map(
${indent(body.mkString(",\n"))}
)"""

    case product: Product =>
      val name    = product.productPrefix
      val labels0 = labels(product)
      val body = labels0
        .zip(product.productIterator)
        .map { case (key, value) =>
          s"$key = ${PrettyPrint(value)}"
        }
        .toList
        .mkString(",\n")
      val isMultiline  = body.split("\n").length > 1
      val indentedBody = indent(body, if (isMultiline) 2 else 0)
      val spacer       = if (isMultiline) "\n" else ""
      s"""$name($spacer$indentedBody$spacer)"""

    case string: String =>
      val surround = if (string.split("\n").length > 1) "\"\"\"" else "\""
      string.replace("\"", """\"""").mkString(surround, "", surround)

    case null  => "null"
    case other => other.toString
  }

  private def indent(string: String, n: Int = 2): String =
    string.split("\n").map((" " * n) + _).mkString("\n")

  private def className(any: Any): String = any match {
    case _: List[_] => "List"
    case other      => other.getClass.getSimpleName
  }

}
