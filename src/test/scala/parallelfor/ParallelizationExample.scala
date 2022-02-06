package parallelfor

import zio._

import Parallelizable._

object ParallelizationExample extends ZIOAppDefault {

  val example1 =
    par {
      for {
        string <- stringZIO
      } yield string
    }

  val example2 =
    par {
      for {
        string <- stringZIO
        int    <- intZIO
      } yield string + int
    }

  val example3 =
    par {
      for {
        string <- stringZIO
        int    <- intZIO
        result <- consumes(string, int)
      } yield result
    }

  val example4 =
    par {
      for {
        string <- stringZIO
        int    <- intZIO
        result <- consumes(string, int)
        _      <- intZIO
      } yield result
    }

  val example5 =
    par {
      for {
        string <- stringZIO
        int    <- intZIO
        cool    = int + string
        result <- consumes(string, int)
        int2   <- intZIO
        nice    = result + int2
      } yield cool + nice
    }

  val example6 =
    par {
      for {
        string       <- stringZIO
        int          <- intZIO
        s"hool $cool" = int + string
        result       <- consumes(string, int)
        int2         <- intZIO
        nice          = result + int2
      } yield cool + nice + 10
    }

  val run =
    example2.timed.debug("RESULT")

  private def delayedEffect[A](name: String)(a: => A) =
    ZIO.debug(s"STARTING $name") *>
      ZIO.sleep(2.seconds).as(a).debug(s"COMPLETED $name with result")

  private lazy val stringZIO =
    delayedEffect("stringZIO")("LO AND DANCE")

  private lazy val intZIO =
    delayedEffect("intZIO")(8)

  private def consumes[A](string: String, int: Int) =
    delayedEffect(s"consumes($string, $int)")(string * int)
}
