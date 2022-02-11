package parallelfor

import parallelfor.Parallelizable
import zio.Zippable

import scala.language.implicitConversions

sealed trait FreeParallel[-R, +E, +A] extends Product with Serializable { self =>

  def execute: A = traceImpl._2

  def trace: ParTrace = traceImpl._1

  def traceImpl: (ParTrace, A)

  def zipPar[R1 <: R, E1 >: E, B](that: FreeParallel[R1, E1, B])(implicit
      zippable: Zippable[A, B]
  ): FreeParallel[R1, E1, zippable.Out] =
    FreeParallel.ZipPar(self, that).map { case (a, b) => zippable.zip(a, b) }

  def flatMap[R1 <: R, E1 >: E, B](f: A => FreeParallel[R1, E1, B]): FreeParallel[R1, E1, B] =
    FreeParallel.FlatMap(this, f)

  def map[B](f: A => B): FreeParallel[R, E, B] =
    FreeParallel.Map(this, f)
}

object FreeParallel {
  def effect[A](a: A): FreeParallel[Any, Nothing, A] =
    FreeParallel.Succeed(a)

  final case class ZipPar[-R, +E, +A, +B](left: FreeParallel[R, E, A], right: FreeParallel[R, E, B])
      extends FreeParallel[R, E, (A, B)] {
    override def traceImpl: (ParTrace, (A, B)) = {
      val (lhsTrace, lhsValue) = left.traceImpl
      val (rhsTrace, rhsValue) = right.traceImpl
      val trace                = lhsTrace zipPar rhsTrace
      (trace, (lhsValue, rhsValue))
    }
  }

  final case class FlatMap[-R, +E, A, +B](fa: FreeParallel[R, E, A], f: A => FreeParallel[R, E, B])
      extends FreeParallel[R, E, B] {

    override def traceImpl: (ParTrace, B) = {
      val (faTrace, faValue) = fa.traceImpl
      val (fbTrace, fbValue) = f(faValue).traceImpl
      val trace              = ParTrace.FlatMap(faTrace, fbTrace)
      (trace, fbValue)
    }
  }

  final case class Map[-R, +E, A, +B](fa: FreeParallel[R, E, A], f: A => B) extends FreeParallel[R, E, B] {

    override def traceImpl: (ParTrace, B) = {
      val (faTrace, faValue) = fa.traceImpl
      (faTrace, f(faValue))
    }
  }

  final case class Succeed[A](value: A) extends FreeParallel[Any, Nothing, A] {

    override def traceImpl: (ParTrace, A) =
      (ParTrace.Value(value), value)
  }

  implicit val parallelizable: Parallelizable[FreeParallel] =
    new Parallelizable[FreeParallel] {

      override def flatMap[R, E, A, B](
          fa: FreeParallel[R, E, A],
          f: A => FreeParallel[R, E, B]
      ): FreeParallel[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: FreeParallel[R, E, A], f: A => B): FreeParallel[R, E, B] =
        fa.map(f)

      override def zipPar[R, E, A, B](
          left: FreeParallel[R, E, A],
          right: FreeParallel[R, E, B]
      ): FreeParallel[R, E, (A, B)] =
        left.zipPar(right)
    }
}
