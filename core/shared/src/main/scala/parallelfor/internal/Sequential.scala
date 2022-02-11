package parallelfor.internal

private[parallelfor] sealed trait Sequential[+A]

private[parallelfor] object Sequential {

  final case class PureAssignment[+A](ident: String, expr: A, usedArgs: List[String])

  final case class FlatMap[A](
      lhs: A,
      usedArgs: List[String],
      bodyArg: String,
      pureAssignments: List[PureAssignment[A]],
      body: Sequential[A]
  ) extends Sequential[A]

  final case class Raw[A](expr: A) extends Sequential[A]
}
