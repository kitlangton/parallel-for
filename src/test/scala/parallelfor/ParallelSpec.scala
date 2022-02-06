package parallelfor

import parallelfor.test.FreeParallel.effect
import parallelfor.test.Trace.zipped
import parallelfor.test._
import zio.test.{Trace => _, _}

object ParallelSpec extends DefaultRunnableSpec {
  def spec =
    suite("ParallelFor")(
      test("map only") {
        val result =
          par {
            for {
              x <- effect(40)
            } yield x
          }

        assertTrue(
          result.trace == Trace(40),
          result.execute == 40
        )
      },
      test("everything in parallel") {
        val result =
          par {
            for {
              x <- effect(5)
              y <- effect(10)
              z <- effect(20)
            } yield x + y + z
          }

        val expected = zipped(5, 10, 20)

        assertTrue(
          result.trace == expected,
          result.execute == 35
        )
      },
      test("parallel then sequential") {
        val result =
          par {
            for {
              x      <- effect(5)
              y      <- effect(10)
              z      <- effect(15)
              result <- effect(x + y + z)
            } yield result / 2
          }

        val expected = zipped(5, 10, 15) >>> 30

        assertTrue(
          result.trace == expected,
          result.execute == 15
        )
      },
      test("maximum parallelism") {
        val result =
          par {
            for {
              x      <- effect(5)
              y      <- effect(10)
              result <- effect((x + y) * 2)
              r2     <- effect("hello")
            } yield result + r2.length
          }

        val expected = zipped(5, 10, "hello") >>> 30

        assertTrue(
          result.trace == expected,
          result.execute == 35
        )
      },
      test("two stages") {
        val result =
          par {
            for {
              x  <- effect(5)
              y  <- effect(10)
              r1 <- effect(x + y)
              r2 <- effect(x * y)
            } yield r1 + r2
          }

        val expected = zipped(5, 10) >>> zipped(15, 50)

        assertTrue(
          result.trace == expected,
          result.execute == 65
        )
      },
      test("pure assignments") {
        val result =
          par {
            for {
              x   <- effect(40)
              cool = x + 5
            } yield cool
          }

        val expected = Trace(40)

        assertTrue(
          result.trace == expected,
          result.execute == 45
        )
      },
      test("pure assignments with flatMap") {
        val result =
          par {
            for {
              x <- effect(5)
              x2 = x + 5
              y <- effect(10)
              y2 = y + 5
              r <- effect(x2 + y2)
              r2 = r + 5
            } yield r2

          }

        val expected = zipped(5, 10) >>> 25

        assertTrue(
          result.trace == expected,
          result.execute == 30
        )
      },
      test("pattern matching in pure assignments") {
        val result =
          par {
            for {
              string          <- effect("Hello, Bobo!")
              s"Hello, $name!" = string
            } yield name.toUpperCase
          }

        val expected = Trace("Hello, Bobo!")

        assertTrue(
          result.trace == expected,
          result.execute == "BOBO"
        )
      },
      test("pattern matching in pure assignments with flatMap") {
        val result =
          par {
            for {
              string          <- effect("Hello, Bobo!")
              s"Hello, $name!" = string
              r               <- effect(s"Bye, $name!")
              cool            <- effect(123)
            } yield r.toUpperCase + cool
          }

        val expected = zipped("Hello, Bobo!", 123) >>> "Bye, Bobo!"

        assertTrue(
          result.trace == expected,
          result.execute == "BYE, BOBO!123"
        )
      }
    )
}
