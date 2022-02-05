package forallel.internal

sealed trait Sequential[+A]

object Sequential {
  // lhs(string, int) flatMap { bodyArg =>
  //   body
  // }
  final case class FlatMap[A](
      lhs: A,
      usedArgs: List[String],
      bodyArg: String,
      pureAssignments: List[(String, A)],
      body: Sequential[A]
  ) extends Sequential[A]

  final case class Raw[A](expr: A) extends Sequential[A]
}
