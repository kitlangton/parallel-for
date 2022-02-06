package parallelfor

import _root_.zio._
import parallelfor.interop.zio._

object ParallelZioSpec extends ZIOAppDefault {

  val example1 =
    par {
      for {
        string <- stringZIO
      } yield string

    }

  val example2 =
    par {
      for {
        _    <- stringZIO
        _    <- stringZIO
        _    <- stringZIO
        int3 <- intZIO
      } yield int3
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
        string         <- stringZIO
        s"LO AND $cool" = string
        int            <- intZIO
        result         <- consumes(string, int)
        int2           <- intZIO
        nice            = result + int2
      } yield cool + nice + 10
    }

  val example7 =
    par {
      for {
        r <- ZIO.succeed("Hello, Bobo!")
      } yield r
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
