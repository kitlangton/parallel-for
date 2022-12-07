# Parallel For

[![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

> **Automatically parallelize your for comprehensions at compile time.**

```sbt
// build.sbt
libraryDependencies ++= Seq(
  "io.github.kitlangton" %% "parallel-for" % "0.0.6",
  "io.github.kitlangton" %% "parallel-for-zio" % "0.0.6", // for ZIO 2
  "io.github.kitlangton" %% "parallel-for-zio1" % "0.0.6", // for ZIO 1
  "io.github.kitlangton" %% "parallel-for-zio-query" % "0.0.6", // for ZIO QUERY (for ZIO 2)
  "io.github.kitlangton" %% "parallel-for-cats-effect-3" % "0.0.6", // for Cats Effect 3
)
```

## Example

Wrapping a for-comprehension in `par` will automatically parallelize it at compile time. 

```scala
import parallelfor._
import parallelfor.interop.zio._ // Change this line depending on your effect system
import zio._

val program =
  par {
    for {
      users  <- loadUsers
      files  <- loadFiles
      config <- loadConfig
      result <- process(users, files, config)
      _      <- fireTheMissiles
    } yield result
  }
```

The `par` macro will rewrite the above program to—*essentially*—the following:

```scala
val program =
  for {
    (users, files, config, _) <- loadUsers zipPar loadFiles zipPar loadConfig zipPar fireTheMissiles
    result                    <- process(users, files, config)
  } yield result
```

***Neato!***


Similar project: [VirtusLab/avocADO] (only for Scala 3)

## TODO

- [x] Work with ZIO and ZManaged
- [x] Maximize parallelization with topological sorting
- [x] Get it to work with val assignments inside of for-comprehensions
- [x] Set up publishing
- [x] Generalize to work with any zippable structure (implement against `Parallelizable` type-class)
- [ ] Fix withFilter interaction
- [ ] Cross-build for JS and Native
- [ ] Cross-build against 2.11/2.12
- [ ] Get it to work with Scala 3

[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.kitlangton/parallel-for_2.13.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/io.github.kitlangton/parallel-for_2.13.svg "Sonatype Snapshots"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/kitlangton/parallel-for_2.13/ "Sonatype Snapshots"
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/io/github/kitlangton/parallel-for_2.13/ "Sonatype Releases"
[VirtusLab/avocADO]: https://github.com/VirtusLab/avocADO
