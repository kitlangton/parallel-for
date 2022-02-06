package parallelfor

import zio.{ZIO, ZManaged, Zippable}

trait Parallelizable[F[-_, +_, +_]] {
  def zipPar[R, E, A, B](left: F[R, E, A], right: F[R, E, B])(implicit zippable: Zippable[A, B]): F[R, E, zippable.Out]
  def flatMap[R, E, A, B](fa: F[R, E, A], f: A => F[R, E, B]): F[R, E, B]
  def map[R, E, A, B](fa: F[R, E, A], f: A => B): F[R, E, B]
}

object Parallelizable {

  implicit final class ParallelizableOps[F[-_, +_, +_], -R, +E, +A](private val self: F[R, E, A])(implicit
      parallelizable: Parallelizable[F]
  ) {
    def zipPar[R1 <: R, E1 >: E, B](that: F[R1, E1, B])(implicit zippable: Zippable[A, B]): F[R1, E1, zippable.Out] =
      parallelizable.zipPar(self, that)

    def flatMap[R1 <: R, E1 >: E, B](f: A => F[R1, E1, B]): F[R1, E1, B] =
      parallelizable.flatMap(self, f)

    def map[B](f: A => B): F[R, E, B] =
      parallelizable.map(self, f)
  }

  implicit val zioPar: Parallelizable[ZIO] =
    new Parallelizable[ZIO] {
      override def zipPar[R, E, A, B](left: ZIO[R, E, A], right: ZIO[R, E, B])(implicit
          zippable: Zippable[A, B]
      ): ZIO[R, E, zippable.Out] =
        left.zipPar(right)

      override def flatMap[R, E, A, B](fa: ZIO[R, E, A], f: A => ZIO[R, E, B]): ZIO[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: ZIO[R, E, A], f: A => B): ZIO[R, E, B] =
        fa.map(f)
    }

  implicit val zManagedPar: Parallelizable[ZManaged] =
    new Parallelizable[ZManaged] {
      override def zipPar[R, E, A, B](left: ZManaged[R, E, A], right: ZManaged[R, E, B])(implicit
          zippable: Zippable[A, B]
      ): ZManaged[R, E, zippable.Out] =
        left.zipPar(right)

      override def flatMap[R, E, A, B](fa: ZManaged[R, E, A], f: A => ZManaged[R, E, B]): ZManaged[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: ZManaged[R, E, A], f: A => B): ZManaged[R, E, B] =
        fa.map(f)
    }
}
