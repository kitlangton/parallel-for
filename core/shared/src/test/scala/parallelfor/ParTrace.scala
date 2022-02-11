package parallelfor

import zio.Chunk

import scala.language.implicitConversions

sealed trait ParTrace extends Product with Serializable {
  self =>
  def zipPar(that: ParTrace): ParTrace =
    (self, that) match {
      case (ParTrace.Value(v1), ParTrace.Value(v2)) =>
        ParTrace.Zipped(Chunk(v1, v2))
      case (ParTrace.Zipped(s1), ParTrace.Value(v1)) =>
        ParTrace.Zipped(s1 :+ v1)
      case (ParTrace.Value(v1), ParTrace.Zipped(s1)) =>
        ParTrace.Zipped(v1 +: s1)
      case (ParTrace.Zipped(s1), ParTrace.Zipped(s2)) =>
        ParTrace.Zipped(s1 ++ s2)
      case (left, right) =>
        throw new IllegalArgumentException(s"Cannot zip $left and $right")
    }

  def >>>(that: ParTrace): ParTrace =
    ParTrace.FlatMap(self, that)
}

object ParTrace {

  def apply(value: Any): ParTrace =
    Value(value)

  def zipped(values: Any*): ParTrace =
    Zipped(Chunk(values: _*))

  implicit def anyToZipTrace(value: Any): ParTrace =
    Value(value)

  final case class Zipped(set: Chunk[Any]) extends ParTrace

  final case class FlatMap(lhs: ParTrace, rhs: ParTrace) extends ParTrace

  final case class Value(value: Any) extends ParTrace

}
