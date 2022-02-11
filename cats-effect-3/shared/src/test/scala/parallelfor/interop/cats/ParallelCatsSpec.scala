package parallelfor.interop.cats

import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.effect.std.Console
import parallelfor.interop.cats._
import parallelfor.par1
import weaver.SimpleIOSuite

import scala.concurrent.duration.DurationInt

object ParallelCatsSpec extends SimpleIOSuite {

  def example4(ref: Ref[IO, List[String]]) =
    par1 {
      for {
        _ <- write(ref, "a")
        b <- write(ref, "b")
        _ <- write(ref, b)
        _ <- write(ref, "c")
      } yield ()
    }

  test("b is last") {
    for {
      ref    <- Ref[IO].of(List.empty[String])
      _      <- example4(ref)
      result <- ref.get
    } yield {
      val last6 = result.drop(2)
      expect(result.take(2) == List("end-b", "start-b")) and
        forEach(last6.takeRight(3))(x => expect(x.startsWith("start"))) and
        forEach(last6.take(3))(x => expect(x.startsWith("end")))
    }
  }

  def write(r: Ref[IO, List[String]], s: String): IO[String] =
    for {
      _   <- r.update(_.prepended(s"start-$s"))
      _   <- IO.sleep(1.seconds)
      _   <- r.update(_.prepended(s"end-$s"))
      res <- r.get
    } yield s

}
