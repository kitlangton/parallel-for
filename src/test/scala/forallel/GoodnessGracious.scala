package forallel

import zio._
import forallel.Parallelize.par

object GoodnessGracious extends ZIOAppDefault {

  val program =
    par {
      for {
        string <- stringZIO
        int    <- intZIO
        int2   <- intZIO
//        cool    = int + int2
        result <- consumes(string, int + int2)
        int3   <- intZIO
      } yield result.take(int3)
    }

//  stringZIO.zipPar(intZIO).flatMap { case (string, int) =>
//    intZIO
//      .map { (int2: Int) =>
//        val cool: Int = int.+(int2)
//        (int2, cool)
//      }
//      .zipPar(consumes(string, cool))
//      .zipPar(intZIO)
//      .map { case (x$1, result, int3) =>
//        scala.Predef.augmentString(result).take(int3)
//      }
//  }

  val nice =
    par {
      for {
        string <- stringZIO
        int    <- intZIO
        int2   <- intZIO
        result <- consumes(string, int + int2)
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
