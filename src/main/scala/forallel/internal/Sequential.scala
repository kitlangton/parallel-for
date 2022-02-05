package forallel.internal

sealed trait Sequential[+A]

object Sequential {
  final case class FlatMap[A](lhs: A, usedArgs: List[String], bodyArg: String, body: Sequential[A])
      extends Sequential[A]

  final case class Raw[A](expr: A) extends Sequential[A]
}
