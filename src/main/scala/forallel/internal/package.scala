package forallel

import forallel.internal.Sequential
import forallel.internal.Parallel

// https://contributors.scala-lang.org/t/for-syntax-for-parallel-computations-afor-applicative-for-comprehension/4474/19
// https://gitlab.haskell.org/ghc/ghc/-/wikis/applicative-do
package object internal {

  def parallelizeParsed[A](parsed: Sequential[A]): Parallel[A] = {
    def loop(parsed: Sequential[A], seen: List[(String, A)]): Parallel[A] =
      parsed match {
        case Sequential.FlatMap(lhs, usedArgs, bodyArg, body) =>
          if (usedArgs.exists(arg => seen.exists(_._1 == arg))) {
            val (args, effects) = seen.reverse.unzip
            Parallel.Parallelized(effects, args, loop(parsed, List.empty))
          } else {
            val newSeen = (bodyArg, lhs) :: seen
            loop(body, newSeen)
          }
        case Sequential.Raw(expr) =>
          if (seen.nonEmpty) {
            val (args, effects) = seen.reverse.unzip
            Parallel.Parallelized(effects, args, loop(parsed, List.empty))
          } else {
            Parallel.Raw(expr)
          }
      }

    loop(parsed, List.empty)
  }

  def compile[A](par: Parallel[A]): CodeTree[A] =
    par match {
      case Parallel.Parallelized(effects, args, body) =>
        val effect = effects.map(CodeTree(_)).reduce(_ zipPar _)
        body match {
          case Parallel.Parallelized(_, _, _) =>
            CodeTree.FlatMap(effect, args, compile(body))
          case Parallel.Raw(_) =>
            CodeTree.Map(effect, args, compile(body))
        }
      case Parallel.Raw(expr) =>
        CodeTree.Value(expr)
    }

}
