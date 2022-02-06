package parallelfor.internal

import parallelfor.internal.Sequential.PureAssignment

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

class Macro(val c: blackbox.Context) {
  import c.universe._

  def parsePureAssignments(tree: c.Tree, seen: List[String]): List[PureAssignment[c.Tree]] =
    tree match {
      case Function(_, body) =>
        parsePureAssignments(body, seen)
      case Block(stats, _) =>
        stats.foldLeft(List.empty[PureAssignment[c.Tree]]) { case (acc, stat) =>
          acc ++ parsePureAssignments(stat, seen ++ acc.map(_.ident))
        }
      case ValDef(_, TermName(name), _, body) =>
        val used = getUsedArgs(tree, seen)
        List(PureAssignment(name, body, used))
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
      effect: c.Tree
  )(parallelizable: c.Tree): c.Tree = {
    val _ = parallelizable
    def loop(tree: c.Tree, seen: List[String]): Sequential[c.Tree] =
      tree match {
        case q"$expr.map[..$_](${Lambda(argName, pureBody)})(..$_).flatMap[..$_](${Lambda(_, body)})(..$_)" =>
          val assignments            = parsePureAssignments(pureBody, argName :: seen)
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            assignments,
            loop(body, (argName :: assignments.map(_.ident)) ++ seen)
          )

        case q"$expr.map[..$_](${Lambda(argName, pureBody)})(..$_).map[..$_](${Lambda(_, body)})(..$_)" =>
          val assignments            = parsePureAssignments(pureBody, argName :: seen)
          val usedArgs: List[String] = getUsedArgs(expr, seen)
          Sequential.FlatMap(
            expr,
            usedArgs,
            argName,
            assignments,
            loop(body, (argName :: assignments.map(_.ident)) ++ seen)
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

    val sequential: Sequential[c.Tree] = loop(effect, List.empty)

    val (nodes, yieldExpr) = Algorithm.collectNodes(sequential)
    val sorted0            = Algorithm.topSort(nodes)
    val sorted             = Algorithm.compress(sorted0)
    val parallelized       = Algorithm.parallelizeNodes(sorted, yieldExpr)

    val codeTree = compile(parallelized)
    val result = codeTree.fold[c.Tree](identity)(
      ifZipPar = (lhs, rhs) => q"$lhs zipPar $rhs",
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

    val expr = c.untypecheck(clean(result))
//    println(s"sequential:\n${PrettyPrint(sequential)}")
//    println(s"nodes: ${nodes.mkString("\n")}")
//    println(s"sorted: ${sorted0.mkString("\n")}")
//    println(s"compressed: ${sorted.mkString("\n")}")
//    println(s"parallelized:\n${PrettyPrint(parallelized)}")
//    println(s"tree:\n${PrettyPrint(codeTree)}")
//    println(s"result:\n${show(expr)}")
    expr
  }

  private def tupleConstructor(args: List[Tree]) =
    args match {
      case List(head) =>
        head
      case args =>
        Apply(Select(Ident(TermName("scala")), TermName(s"Tuple${args.length}")), args)
    }

  private def makeBinder(name: String) =
    Bind(TermName(name), Ident(termNames.WILDCARD))

  private def functionBody(args: List[String], body: c.Tree) =
    Match(
      EmptyTree,
      List(
        CaseDef(
          q"${tupleConstructor(args.map(makeBinder))}",
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
      case Typed(expr, tpt) =>
        Typed(clean(expr), clean(tpt))
      case Function(args, body) =>
        Function(args, clean(body))
      case other =>
//        println("OH")
//        renderError(other)
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

      case Typed(t1, t2) =>
        getUsedArgs(t1, seen) ++ getUsedArgs(t2, seen)

      case TypeTree() =>
        List.empty

      case Match(lhs, caseDefs) =>
        getUsedArgs(lhs, seen) ++ caseDefs.flatMap(getUsedArgs(_, seen))

      case CaseDef(pat, guard, body) =>
        getUsedArgs(pat, seen) ++ getUsedArgs(guard, seen) ++ getUsedArgs(body, seen)

      case UnApply(fun, args) =>
        getUsedArgs(fun, seen) ++ args.flatMap(getUsedArgs(_, seen))

      case Bind(name, body) =>
        getUsedArgs(body, seen)

      case EmptyTree =>
        List.empty

      case other =>
//        println("NOO")
//        renderError(other)
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
