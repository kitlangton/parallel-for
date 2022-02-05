package forallel

import forallel.internal.forallel.Parallelizable
import forallel.internal.{CodeTree, Parallel, PrettyPrint, Sequential, compile, parallelizeParsed}
import zio._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Parallelize {
  def par[F[-_, +_, +_], R, E, A](zio: F[R, E, A])(implicit parallelizable: Parallelizable[F]): F[R, E, A] =
    macro Macro.parallelizeImpl[F, R, E, A]
}

class Macro(val c: blackbox.Context) {
  import c.universe._

  def parsePureAssignments(tree: c.Tree): List[(String, c.Tree)] =
    tree match {
      case Function(_, body) =>
        parsePureAssignments(body)
      case Block(stats, _) =>
        stats.flatMap(t => parsePureAssignments(t))
      case ValDef(_, TermName(name), _, body) =>
        List(name -> body)
      case other =>
        List.empty
    }

  object Lambda {
    def unapply(tree: c.Tree): Option[(String, c.Tree)] =
      tree match {
        case Function(List(ValDef(_, TermName(name), _, _)), body) =>
          Some(name -> body)
        case _ =>
          None
      }
  }

  def makeValDefs(pure: List[(String, c.Tree)]): List[ValDef] =
    pure.map { case (name, tree) =>
      ValDef(NoMods, TermName(name), TypeTree(), tree)
    }

  def parallelizeImpl[F[-_, +_, +_], R, E, A](
      zio: c.Tree
  )(parallelizable: c.Tree): c.Tree = {
    def loop(tree: c.Tree, seen: List[String]): Sequential[c.Tree] =
      tree match {
        case q"$expr.map[..$_](${Lambda(argName, pureBody)})(..$_).flatMap[..$_](${Lambda(_, body)})(..$_)" =>
          val assignments            = parsePureAssignments(pureBody)
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            assignments,
            loop(body, (argName :: assignments.map(_._1)) ++ seen)
          )

        case Apply(
              Apply(
                TypeApply(Select(expr, TermName("flatMap")), _),
                List(Lambda(argName, body))
              ),
              _
            ) =>
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            List.empty,
            loop(body, argName :: seen)
          )

        case Apply(
              Apply(
                TypeApply(Select(expr, TermName("map")), _),
                List(Lambda(argName, body))
              ),
              _
            ) =>
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            List.empty,
            loop(body, argName :: seen)
          )

        case Match(_, List(CaseDef(_, _, body))) =>
          loop(body, seen)

        case other =>
          Sequential.Raw(other)
      }

    val sequential: Sequential[c.Tree] = loop(zio, List.empty)
    val parallelized: Parallel[c.Tree] = parallelizeParsed(sequential)
    val structure: CodeTree[c.Tree]    = compile(parallelized)
    val result: c.Tree = structure.fold[c.Tree](identity)(
      ifZipPar = (lhs, rhs) => q"$lhs zipPar2 $rhs",
      ifMap = (lhs, args, pure, rhs) => q"""
$lhs.map { 
  ${functionBody(args, Block(makeValDefs(pure), rhs))} 
}
         """,
      ifFlatMap = (lhs, args, pure, rhs) => q"""
$lhs.flatMap { 
  ${functionBody(args, Block(makeValDefs(pure), rhs))} 
}
         """
    )

    val expr = clean(result)
//    println(s"sequential:\n${PrettyPrint(sequential)}")
//    println(s"parallelized:\n${PrettyPrint(parallelized)}")
//    println(s"tree:\n${PrettyPrint(structure)}")
//    println(s"result:\n${show(expr)}")
    expr
  }

  private def tupleConstructor(n: Int) =
    Select(Ident(TermName("scala")), TermName(s"Tuple$n"))

  private def makeBinder(name: String) =
    Bind(TermName(name), Ident(termNames.WILDCARD))

  private def functionBody(args: List[String], body: c.Tree) =
    Match(
      EmptyTree,
      List(
        CaseDef(
          q"${tupleConstructor(args.length)}(..${args.map(makeBinder)})",
          EmptyTree,
          clean(body)
        )
      )
    )

  private def clean(tree: Tree): Tree =
    tree match {
      case Ident(TermName(name)) =>
        Ident(TermName(name))
      case Apply(fun, args) =>
        Apply(clean(fun), args.map(t => clean(t)))
      case Select(tree, name) =>
        Select(clean(tree), name)
      case TypeApply(tree, args) =>
        TypeApply(clean(tree), args.map(t => clean(t)))
      case TypeTree() =>
        TypeTree()
      case Block(stats, body) =>
        Block(stats.map(t => clean(t)), clean(body))
      case ValDef(mods, name, tpt, rhs) =>
        ValDef(mods, name, tpt, clean(rhs))
      case This(name) =>
        This(name)
      case Literal(value) =>
        Literal(value)
      case Match(selector, cases) =>
        val cleanedCases = cases.map { case CaseDef(pat, guard, body) =>
          CaseDef(pat, guard, clean(body))
        }
        Match(clean(selector), cleanedCases)
      case EmptyTree =>
        EmptyTree
      case other =>
        other
    }

  private def getUsedArgs(tree: Tree, seen: List[String]): List[String] =
    tree match {
      case Ident(name) =>
        if (seen.contains(name.toString)) List(name.toString)
        else List.empty

      case Select(tree, _) =>
        getUsedArgs(tree, seen)

      case Apply(tree, args) =>
        getUsedArgs(tree, seen) ++ args.flatMap(getUsedArgs(_, seen))

      case TypeApply(tree, _) =>
        getUsedArgs(tree, seen)

      case Literal(_) =>
        List.empty

      case Function(args, body) =>
        getUsedArgs(body, seen.filterNot(s => args.map(_.name.decodedName.toString).contains(s)))

      case ValDef(_, _, _, body) =>
        getUsedArgs(body, seen)

      case Block(stats, expr) =>
        stats.flatMap(getUsedArgs(_, seen)) ++ getUsedArgs(expr, seen)

      case This(_) =>
        List.empty

      case _ =>
        List.empty
    }

  private def renderError(tree: c.Tree): Nothing = {
    val message =
      s"""
         |===============
         |${show(tree)}
         |===============
         |${showRaw(tree)}
         |===============
         |""".stripMargin

    c.abort(
      c.enclosingPosition,
      message
    )
  }
}
