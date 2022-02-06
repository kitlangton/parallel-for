package parallelfor

import zio._

object ParallelizationExample extends ZIOAppDefault {

  val program =
    par {
      for {
        string         <- stringZIO
        cool            = string.toLowerCase
        s"lo and $word" = cool
        int            <- intZIO
        fancy           = word + int
      } yield cool + word + int
    }

  val run =
    program.timed.debug("RESULT")

  ZIO
    .ZIOWithFilterOps[zio.Clock, NoSuchElementException, String](stringZIO)
    .withFilter {
      case s"cool$string" => true
      case _              => false
    }
    .map[String] { case s"cool$string" =>
      string
    }

  def delayedEffect[A](name: String)(a: => A) =
    ZIO.debug(s"STARTING $name") *>
      ZIO.sleep(2.seconds).as(a).debug(s"COMPLETED $name with result")

  lazy val stringZIO =
    delayedEffect("stringZIO")("LO AND DANCE")

  lazy val intZIO =
    delayedEffect("intZIO")(8)

  def consumes[A](string: String, int: Int) =
    delayedEffect(s"consumes($string, $int)")(string * int)
}
