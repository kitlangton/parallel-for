package parallelfor.interop

import parallelfor.Parallelizable
import zio.query.ZQuery

package object zioquery {
  implicit val zioPar: Parallelizable[ZQuery] =
    new Parallelizable[ZQuery] {
      override def zipPar[R, E, A, B](left: ZQuery[R, E, A], right: ZQuery[R, E, B]): ZQuery[R, E, (A, B)] =
        left.zipWithPar(right)((_, _))

      override def flatMap[R, E, A, B](fa: ZQuery[R, E, A], f: A => ZQuery[R, E, B]): ZQuery[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: ZQuery[R, E, A], f: A => B): ZQuery[R, E, B] =
        fa.map(f)
    }
}
