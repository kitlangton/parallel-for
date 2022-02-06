package parallelfor.internal

sealed trait CodeTree[+A] extends Product with Serializable { self =>

  def zipPar[A1 >: A](that: CodeTree[A1]): CodeTree[A1] =
    CodeTree.ZipPar(self, that)

  def fold[Z](
      ifValue: A => Z
  )(
      ifZipPar: (Z, Z) => Z,
      ifMap: (Z, List[String], List[(String, A)], Z) => Z,
      ifFlatMap: (Z, List[String], List[(String, A)], Z) => Z
  ): Z =
    self match {
      case CodeTree.FlatMap(lhs, args, pureAssignments, body) =>
        ifFlatMap(
          lhs.fold(ifValue)(ifZipPar, ifMap, ifFlatMap),
          args,
          pureAssignments,
          body.fold(ifValue)(ifZipPar, ifMap, ifFlatMap)
        )
      case CodeTree.Map(lhs, args, pureAssignments, body) =>
        ifMap(
          lhs.fold(ifValue)(ifZipPar, ifMap, ifFlatMap),
          args,
          pureAssignments,
          body.fold(ifValue)(ifZipPar, ifMap, ifFlatMap)
        )
      case CodeTree.ZipPar(lhs, rhs) =>
        ifZipPar(lhs.fold(ifValue)(ifZipPar, ifMap, ifFlatMap), rhs.fold(ifValue)(ifZipPar, ifMap, ifFlatMap))
      case CodeTree.Value(value) =>
        ifValue(value)
    }
}

object CodeTree {
  def apply[A](value: A): CodeTree[A] = Value(value)

  final case class FlatMap[A](
      lhs: CodeTree[A],
      args: List[String],
      pureAssignments: List[(String, A)],
      body: CodeTree[A]
  ) extends CodeTree[A]

  final case class Map[A](
      lhs: CodeTree[A],
      args: List[String],
      pureAssignments: List[(String, A)],
      body: CodeTree[A]
  ) extends CodeTree[A]

  final case class ZipPar[A](lhs: CodeTree[A], rhs: CodeTree[A]) extends CodeTree[A]

  final case class Value[A](value: A) extends CodeTree[A]
}
