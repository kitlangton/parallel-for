package parallelfor.test

import zio.Chunk

import scala.language.implicitConversions

sealed trait Trace extends Product with Serializable {
  self =>
  def zipPar(that: Trace): Trace =
    (self, that) match {
      case (Trace.Value(v1), Trace.Value(v2)) =>
        Trace.Zipped(Chunk(v1, v2))
      case (Trace.Zipped(s1), Trace.Value(v1)) =>
        Trace.Zipped(s1 :+ v1)
      case (Trace.Value(v1), Trace.Zipped(s1)) =>
        Trace.Zipped(v1 +: s1)
      case (Trace.Zipped(s1), Trace.Zipped(s2)) =>
        Trace.Zipped(s1 ++ s2)
      case (left, right) =>
        throw new IllegalArgumentException(s"Cannot zip $left and $right")
    }

  def >>>(that: Trace): Trace =
    Trace.FlatMap(self, that)
}

object Trace {

  def apply(value: Any): Trace =
    Value(value)

  def zipped(values: Any*): Trace =
    Zipped(Chunk(values: _*))

  implicit def anyToZipTrace(value: Any): Trace =
    Value(value)

  final case class Zipped(set: Chunk[Any]) extends Trace

  final case class FlatMap(lhs: Trace, rhs: Trace) extends Trace

  final case class Value(value: Any) extends Trace

}
