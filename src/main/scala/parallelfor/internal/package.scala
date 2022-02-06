package parallelfor

import parallelfor.internal.Sequential
import parallelfor.internal.Parallel

// https://contributors.scala-lang.org/t/for-syntax-for-parallel-computations-afor-applicative-for-comprehension/4474/19
// https://gitlab.haskell.org/ghc/ghc/-/wikis/applicative-do
package object internal {

  def parallelizeParsed[A](parsed: Sequential[A]): Parallel[A] = {
    def loop(
        sequential: Sequential[A],
        effects: List[(String, A)],
        pure: List[(String, A)]
    ): Parallel[A] =
      sequential match {
        case Sequential.FlatMap(lhs, usedArgs, bodyArg, pure0, body) =>
          val seenArgs = pure.map(_._1).toSet ++ effects.map(_._1).toSet

          if (usedArgs.exists(seenArgs)) {
            Parallel.Parallelized(
              effects = effects,
              pure = pure,
              body = loop(sequential, List.empty, pure0.map(p => (p.ident, p.expr)))
            )
          } else {
            val newEffects = (bodyArg, lhs) :: effects
            loop(body, newEffects, pure0.map(p => (p.ident, p.expr)) ++ pure)
          }
        case Sequential.Raw(expr) =>
          if (effects.nonEmpty || pure.nonEmpty)
            Parallel.Parallelized(effects, pure, loop(sequential, List.empty, List.empty))
          else
            Parallel.Raw(expr)
      }

    loop(parsed, List.empty, List.empty)
  }

  def compile[A](par: Parallel[A]): CodeTree[A] =
    par match {
      case Parallel.Parallelized(effects0, pureAssignments, body) =>
        val (args, effects) = effects0.unzip
        val effect          = effects.map(CodeTree(_)).reduce(_ zipPar _)
        body match {
          case Parallel.Parallelized(_, _, _) =>
            CodeTree.FlatMap(effect, args, pureAssignments, compile(body))
          case Parallel.Raw(_) =>
            CodeTree.Map(effect, args, pureAssignments, compile(body))
        }
      case Parallel.Raw(expr) =>
        CodeTree.Value(expr)
    }

}
