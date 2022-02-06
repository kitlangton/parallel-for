package parallelfor

import zio._

object ParallelizationExample extends ZIOAppDefault {

  val program =
    par {
      for {
        string <- stringZIO
        fancy   = { println("WOW"); string }
        result <- consumes(fancy, 10)
        int3   <- intZIO
        lovely  = { println(s"hello $result"); int3 }
      } yield result.take(lovely)
    }

  val run =
    program.timed.useNow.debug("RESULT")

  def delayedEffect[A](name: String)(a: => A): ZManaged[Clock, Nothing, A] =
    (ZIO.debug(s"STARTING $name") *>
      ZIO.sleep(2.seconds).as(a).debug(s"COMPLETED $name with result")).toManaged

  lazy val stringZIO: ZManaged[Clock, NumberFormatException, String] =
    delayedEffect("stringZIO")("LO")

  lazy val intZIO: ZManaged[Clock with Console, NoSuchElementException, Int] =
    delayedEffect("intZIO")(8)

  def consumes[A](string: String, int: Int) =
    delayedEffect(s"consumes($string, $int)")(string * int)
}
