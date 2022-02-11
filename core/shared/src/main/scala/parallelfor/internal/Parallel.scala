package parallelfor.internal

private[parallelfor] sealed trait Parallel[+A] extends Product with Serializable

private[parallelfor] object Parallel {
  final case class Parallelized[A](
      effects: List[(String, A)],
      pure: List[(String, A)],
      body: Parallel[A]
  ) extends Parallel[A]

  final case class Raw[A](expr: A) extends Parallel[A]
}
