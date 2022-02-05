# Parallel For

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

- [ ] Generalize to work with any zippable structure (zio, cats, monix, etc.)
- [ ] Get it to work with val assignments inside of for-comprehensions
- [ ] Test the heck out of it
