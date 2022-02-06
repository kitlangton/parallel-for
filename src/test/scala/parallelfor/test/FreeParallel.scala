package parallelfor.test

import parallelfor.Parallelizable
import zio.Zippable

import scala.language.implicitConversions

sealed trait FreeParallel[-R, +E, +A] extends Product with Serializable { self =>

  def execute: A = traceImpl._2

  def trace: Trace = traceImpl._1

  def traceImpl: (Trace, A)

  def flatMap[R1 <: R, E1 >: E, B](f: A => FreeParallel[R1, E1, B])(implicit
      dummyImplicit: DummyImplicit
  ): FreeParallel[R1, E1, B] =
    FreeParallel.FlatMap(this, f)

  def map[R1 <: R, E1 >: E, B](f: A => B)(implicit dummyImplicit: DummyImplicit): FreeParallel[R1, E1, B] =
    FreeParallel.Map(this, f)
}

object FreeParallel {
  def effect[A](a: A): FreeParallel[Any, Nothing, A] =
    FreeParallel.Succeed(a)

  final case class ZipPar[R, E, A, B](left: FreeParallel[R, E, A], right: FreeParallel[R, E, B])
      extends FreeParallel[R, E, (A, B)] {
    override def traceImpl: (Trace, (A, B)) = {
      val (lhsTrace, lhsValue) = left.traceImpl
      val (rhsTrace, rhsValue) = right.traceImpl
      val trace                = lhsTrace zipPar rhsTrace
      (trace, (lhsValue, rhsValue))
    }
  }

  final case class FlatMap[R, E, A, B](fa: FreeParallel[R, E, A], f: A => FreeParallel[R, E, B])
      extends FreeParallel[R, E, B] {

    override def traceImpl: (Trace, B) = {
      val (faTrace, faValue) = fa.traceImpl
      val (fbTrace, fbValue) = f(faValue).traceImpl
      val trace              = Trace.FlatMap(faTrace, fbTrace)
      (trace, fbValue)
    }
  }

  final case class Map[R, E, A, B](fa: FreeParallel[R, E, A], f: A => B) extends FreeParallel[R, E, B] {

    override def traceImpl: (Trace, B) = {
      val (faTrace, faValue) = fa.traceImpl
      (faTrace, f(faValue))
    }
  }

  final case class Succeed[A](value: A) extends FreeParallel[Any, Nothing, A] {

    override def traceImpl: (Trace, A) =
      (Trace.Value(value), value)
  }

  implicit val parallelizable: Parallelizable[FreeParallel] =
    new Parallelizable[FreeParallel] {

      override def zipPar[R, E, A, B](left: FreeParallel[R, E, A], right: FreeParallel[R, E, B])(implicit
          zippable: Zippable[A, B]
      ): FreeParallel[R, E, zippable.Out] =
        ZipPar(left, right).map { case (a, b) => zippable.zip(a, b) }

      override def flatMap[R, E, A, B](
          fa: FreeParallel[R, E, A],
          f: A => FreeParallel[R, E, B]
      ): FreeParallel[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: FreeParallel[R, E, A], f: A => B): FreeParallel[R, E, B] =
        fa.map(f)
    }
}
