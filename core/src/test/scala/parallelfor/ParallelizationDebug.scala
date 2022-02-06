package parallelfor

//import parallelfor.Parallelizable._
import zio._

object ParallelizationDebug extends ZIOAppDefault {

  implicit val zioParallelizable =
    new Parallelizable[ZIO] {
      override def zipPar[R, E, A, B](left: ZIO[R, E, A], right: ZIO[R, E, B]): ZIO[R, E, (A, B)] =
        left.zipPar(right)

      override def flatMap[R, E, A, B](fa: ZIO[R, E, A], f: A => ZIO[R, E, B]): ZIO[R, E, B] =
        fa.flatMap(f)

      override def map[R, E, A, B](fa: ZIO[R, E, A], f: A => B): ZIO[R, E, B] =
        fa.map(f)
    }

  val program =
    par {
      for {
        word <- stringZIO
        _    <- stringZIO
        int  <- intZIO
      } yield word + int
    }

  val run =
    program.timed.debug("RESULT")

  val nice = ZIO
    .ZIOWithFilterOps(stringZIO)
    .withFilter(
      (
          (check$ifrefutable$1: String) =>
            (check$ifrefutable$1: String @unchecked) match {
              case s"lo and$word" => true
              case _              => false
            }
      )
    )
    .flatMap(
      (
          (x$1: String) =>
            (x$1: String @unchecked) match {
              case s"lo and $word" =>
                intZIO.map[String](((int: Int) => word.+(int)))
            }
      )
    )

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
