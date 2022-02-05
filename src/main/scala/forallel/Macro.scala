package forallel

import forallel.internal.{CodeTree, Sequential, compile, parallelizeParsed}
import zio._

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object Parallelize {
  def par[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    macro Macro.parallelizeImpl[R, E, A]
}

class Macro(val c: blackbox.Context) {
  import c.universe._

  def parallelizeImpl[R: c.WeakTypeTag, E: c.WeakTypeTag, A: c.WeakTypeTag](
      zio: c.Tree
  ): c.Tree = {
    def loop(tree: c.Tree, seen: List[String]): Sequential[c.Tree] =
      tree match {
        case Apply(
              Apply(
                TypeApply(Select(expr, TermName("flatMap")), _),
                List(Function(List(ValDef(_, TermName(argName), _, _)), body))
              ),
              _
            ) =>
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            loop(body, argName :: seen)
          )

        case Apply(
              Apply(
                TypeApply(Select(expr, TermName("map")), _),
                List(Function(List(ValDef(_, TermName(argName), _, _)), body))
              ),
              _
            ) =>
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            loop(body, argName :: seen)
          )

        case Match(_, List(CaseDef(_, _, body))) =>
          loop(body, seen)

        case other =>
          Sequential.Raw(other)
      }

    val ir: Sequential[c.Tree]      = loop(zio, List.empty)
    val structure: CodeTree[c.Tree] = compile(parallelizeParsed(ir))
    val result: c.Tree = structure.fold[c.Tree](identity)(
      (lhs, rhs) => q"$lhs zipPar $rhs",
      (lhs, args, rhs) => q"$lhs.map { ${functionBody(args, rhs)} }",
      (lhs, args, rhs) => q"$lhs.flatMap { ${functionBody(args, rhs)} } "
    )

    val expr = result
//    renderError(expr)
    result
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
          clean(body, args)
        )
      )
    )

  private def clean(tree: Tree, names: List[String]): Tree =
    tree match {
      case Ident(TermName(name)) if names.contains(name) =>
        Ident(TermName(name))
      case Ident(TermName(name)) =>
        Ident(TermName(name))
      case Apply(fun, args) =>
        Apply(clean(fun, names), args.map(t => clean(t, names)))
      case Select(qual, name) =>
        Select(clean(qual, names), name)
      case TypeApply(tree, args) =>
        TypeApply(clean(tree, names), args.map(t => clean(t, names)))
      case TypeTree() =>
        TypeTree()
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

      case other =>
        println("OHHH")
        renderError(other)
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
