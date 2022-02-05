package forallel

import zio._
import forallel.Parallelize.par

object ParallelizationExample extends ZIOAppDefault {
  import forallel.internal.forallel.Parallelizable._

  val program =
    par {
      for {
        string <- stringZIO
        another = string.toUpperCase
        int    <- intZIO
        cool    = int + 11
        int2   <- intZIO
        result <- consumes(another, cool + int2)
        int3   <- intZIO
      } yield result.take(int3)
    }

  val run =
    program.timed.useNow.debug("RESULT")

  def delayedEffect[A](name: String)(a: => A): ZManaged[Clock, Nothing, A] =
    (ZIO.debug(s"STARTING $name") *>
      ZIO.sleep(2.seconds).as(a).debug(s"COMPLETED $name with result")).toManaged_

  lazy val stringZIO: ZManaged[Clock, NumberFormatException, String] =
    delayedEffect("stringZIO")("LO")

  lazy val intZIO: ZManaged[Clock with Console, NoSuchElementException, Int] =
    delayedEffect("intZIO")(8)

  def consumes[A](string: String, int: Int) =
    delayedEffect(s"consumes($string, $int)")(string * int)
}
