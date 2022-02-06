package parallelfor

trait Parallelizable[F[-_, +_, +_]] {
  def zipPar[R, E, A, B](left: F[R, E, A], right: F[R, E, B]): F[R, E, (A, B)]
  def flatMap[R, E, A, B](fa: F[R, E, A], f: A => F[R, E, B]): F[R, E, B]
  def map[R, E, A, B](fa: F[R, E, A], f: A => B): F[R, E, B]
}

object Parallelizable {

  implicit final class ParallelizableOps[F[-_, +_, +_], -R, +E, +A](private val self: F[R, E, A])(implicit
      parallelizable: Parallelizable[F]
  ) {
    def zipPar[R1 <: R, E1 >: E, B](that: F[R1, E1, B]): F[R1, E1, (A, B)] =
      parallelizable.zipPar(self, that)

    def flatMap[R1 <: R, E1 >: E, B](f: A => F[R1, E1, B]): F[R1, E1, B] =
      parallelizable.flatMap(self, f)

    def map[B](f: A => B): F[R, E, B] =
      parallelizable.map(self, f)
  }

}
