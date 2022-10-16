package parallelfor.interop

import _root_.zio._
import parallelfor.Parallelizable

package object zio {

  implicit val zioPar: Parallelizable[ZIO] =
    new Parallelizable[ZIO] {
      override def zipPar[R, E, A, B](left: ZIO[R, E, A], right: ZIO[R, E, B]): ZIO[R, E, (A, B)] =
        left.zipWithPar(right)((_, _))

      override def flatMap[R, E, A, B](fa: ZIO[R, E, A], f: A => ZIO[R, E, B]): ZIO[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: ZIO[R, E, A], f: A => B): ZIO[R, E, B] =
        fa.map(f)
    }

}
