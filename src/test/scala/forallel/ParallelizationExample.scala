package forallel

import zio._
import forallel.Parallelize.par

object ParallelizationExample extends ZIOAppDefault {

  val program =
    par {
      for {
        string <- stringZIO
        another = string.toUpperCase
        int    <- intZIO
        cool    = int + 10
        int2   <- intZIO
        result <- consumes(another, cool + int2)
        int3   <- intZIO
      } yield result.take(int3)
    }

  val run =
    program.timed.debug("RESULT")

  def delayedEffect[A](name: String)(a: => A): ZIO[Clock, Nothing, A] =
    ZIO.debug(s"STARTING $name") *>
      ZIO.sleep(2.seconds).as(a).debug(s"COMPLETED $name with result")

  lazy val stringZIO = delayedEffect("stringZIO")("LO")
  lazy val intZIO    = delayedEffect("intZIO")(8)

  def consumes[A](string: String, int: Int) =
    delayedEffect(s"consumes($string, $int)")(string * int)
}
