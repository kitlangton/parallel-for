package parallelfor.interop

import _root_.cats.effect.IO
import parallelfor.{Parallelizable, Parallelizable1}

package object cats {

  implicit val ioPar1: Parallelizable1[IO] = new Parallelizable1[IO] {
    override def zipPar[A, B](left: IO[A], right: IO[B]): IO[(A, B)] = left.both(right)
    override def flatMap[A, B](fa: IO[A], f: A => IO[B]): IO[B]      = fa.flatMap(f)
    override def map[A, B](fa: IO[A], f: A => B): IO[B]              = fa.map(f)
  }

  type IOP[-_, +_, +A] = IO[A]

  implicit def convertIO(implicit p1: Parallelizable1[IO]): Parallelizable[IOP] = new Parallelizable[IOP] {
    override def zipPar[R, E, A, B](left: IOP[R, E, A], right: IOP[R, E, B]): IOP[R, E, (A, B)] = p1.zipPar(left, right)
    override def flatMap[R, E, A, B](fa: IOP[R, E, A], f: A => IOP[R, E, B]): IOP[R, E, B]      = p1.flatMap(fa, f)
    override def map[R, E, A, B](fa: IOP[R, E, A], f: A => B): IOP[R, E, B]                     = p1.map(fa, f)
  }

}
