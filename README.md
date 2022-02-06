# Parallel For

[![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

> **Automatically parallelize your for comprehensions at compile time.**

## Example

Wrapping a for-comprehension in `par` will automatically parallelize it at compile time. 

```scala
  val program =
    par {
      for {
        users  <- loadUsers
        files  <- loadFiles
        config <- loadConfig
        _      <- log("Processing result")
        result <- process(users, files, config)
      } yield result
    }
```

The `par` macro will rewrite the above program to—*essentially*—the following:

```scala
  val program =
    for {
      (users, files, config)  <- loadUsers zipPar loadFiles zipPar loadConfig
      (_, result)             <- log("Processing result") zipPar process(users, files, config)
    } yield result
```

***Neato!***

## TODO

- [x] Work with ZIO and ZManaged
- [x] Maximize parallelization with topological sorting
- [x] Get it to work with val assignments inside of for-comprehensions
- [ ] Generalize to work with any zippable structure (zio, cats, monix, etc.)
- [ ] Fix withFilter interaction
- [ ] Test the heck out of it
- [ ] Cross-building for JVM, JS, and Native
- [ ] Get it to work with Scala 3
- [ ] Set up publishing


[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/oss.sonatype.org/io.github.kitlangton/parallel-for_2.13.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/io.github.kitlangton/parallel-for_2.13.svg "Sonatype Snapshots"
[Link-SonatypeSnapshots]: https://oss.sonatype.org/content/repositories/snapshots/io/github/kitlangton/parallel-for_2.13/ "Sonatype Snapshots"
[Link-SonatypeReleases]: https://oss.sonatype.org/content/repositories/releases/io/github/kitlangton/parallel-for_2.13/ "Sonatype Releases"
