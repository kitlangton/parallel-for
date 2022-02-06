package parallelfor.internal

import parallelfor.internal.Sequential.PureAssignment
import zio._

import scala.annotation.tailrec

sealed trait ExprType[+A] {
  def value: A
}

object ExprType {
  case class Pure[A](value: A)   extends ExprType[A]
  case class Effect[A](value: A) extends ExprType[A]
}

final case class Ident(name: String)
final case class Node[+A](references: List[Ident], expr: ExprType[A], output: Ident)
final case class Graph[+A](nodes: List[Node[A]])
case class Labeled[+A](ident: Ident, expr: ExprType[A])

object Algorithm extends scala.App {

  val effect =
    for {
      int    <- ZIO.succeed(1)
      string <- ZIO.succeed("Hello")
      hello   = string.length + int
      both   <- ZIO.succeed(hello)
      int2   <- ZIO.succeed(10)
    } yield int2 + both

  val nodes =
    List(
      Node(List(Ident("string"), Ident("int")), ExprType.Pure("string.length + int"), Ident("hello")),
      Node(List.empty, ExprType.Effect("ZIO.succeed(1)"), Ident("int")),
      Node(List.empty, ExprType.Effect("ZIO.succeed(\"Hello\")"), Ident("string")),
      Node(List(Ident("hello")), ExprType.Effect("ZIO.succeed(int + string.length)"), Ident("both")),
      Node(List.empty, ExprType.Effect("ZIO.succeed(10)"), Ident("int2"))
    )

  def compileNodes[A](sequential: Sequential[A]): (List[Node[A]], A) = {
    @tailrec
    def loop(sequential: Sequential[A], acc: List[Node[A]]): (List[Node[A]], A) =
      sequential match {
        case Sequential.FlatMap(lhs, usedArgs, bodyArg, pureAssignments, body) =>
          val effectNode = Node(usedArgs.map(Ident(_)), ExprType.Effect(lhs), Ident(bodyArg))

          val pureNodes = pureAssignments.map { //
            case PureAssignment(ident, expr, usedArgs) =>
              Node(usedArgs.map(Ident(_)), ExprType.Pure(expr), Ident(ident))
          }

          loop(
            body,
            effectNode :: pureNodes ++ acc
          )

        case Sequential.Raw(expr) =>
          acc -> expr
      }

    loop(sequential, List.empty)
  }

  def parallelizeNodes[A](nodes: List[Set[Labeled[A]]], yieldExpr: A): Parallel[A] =
    nodes match {
      case set :: tail =>
        val effects =
          set.collect { case Labeled(ident, ExprType.Effect(expr)) =>
            ident.name -> expr
          }.toList

        val pure = set.collect { case Labeled(ident, ExprType.Pure(expr)) =>
          ident.name -> expr
        }.toList

        Parallel.Parallelized(
          effects,
          pure,
          parallelizeNodes(tail, yieldExpr)
        )

      case Nil =>
        Parallel.Raw(yieldExpr)
    }

  // Set[(Ident, A])]
  //
  // b   e
  // a c d
  // 0 1 2

  // collapse adjacent sets if the first set contains only pure effects
  def compress[A](nodes: List[Set[Labeled[A]]]): List[Set[Labeled[A]]] = {
    @tailrec
    def loop(nodes: List[Set[Labeled[A]]], acc: List[Set[Labeled[A]]]): List[Set[Labeled[A]]] =
      nodes match {
        case a :: b :: tail =>
          val bIsPure = b.forall {
            case Labeled(_, ExprType.Pure(_)) => true
            case _                            => false
          }

          if (bIsPure) loop(tail, (a ++ b) :: acc)
          else loop(b :: tail, a :: acc)

        case a :: tail =>
          loop(tail, a :: acc)

        case Nil =>
          acc
      }

    loop(nodes, List.empty).reverse
  }

  def topSort[A](nodes: List[Node[A]]): List[Set[Labeled[A]]] =
    if (nodes.isEmpty) List.empty
    else {
      val nodesWithNoDependencies = nodes.filter(_.references.isEmpty)
      val remaining: List[Node[A]] = nodes.filterNot(_.references.isEmpty).map { case Node(references, expr, output) =>
        // remove all references to nodesWithNoDependencies
        val filteredReferences = references.filterNot(nodesWithNoDependencies.map(_.output).contains)
        Node(filteredReferences, expr, output)
      }
      val labeledSet: Set[Labeled[A]] =
        nodesWithNoDependencies.map { case Node(_, expr, output) =>
          Labeled(output, expr)
        }.toSet
      labeledSet :: topSort(remaining)
    }

  // println(topSort(nodes).mkString("\n"))

}
