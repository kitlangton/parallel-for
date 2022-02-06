package parallelfor.internal

import parallelfor.internal.Sequential.PureAssignment

import scala.annotation.tailrec

sealed trait ExprType[+A] {
  def isPure: Boolean = this.isInstanceOf[ExprType.Pure[_]]

  def value: A
}

object ExprType {
  case class Pure[A](value: A)   extends ExprType[A]
  case class Effect[A](value: A) extends ExprType[A]
}

final case class Ident(name: String) extends AnyVal
final case class Node[+A](references: List[Ident], expr: ExprType[A], output: Ident)
final case class Labeled[+A](ident: Ident, expr: ExprType[A])

object Algorithm extends scala.App {

  def collectNodes[A](sequential: Sequential[A]): (List[Node[A]], A) = {
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

  def parallelizeNodes[A](nodes: List[List[Labeled[A]]], yieldExpr: A): Parallel[A] =
    nodes match {
      case set :: tail =>
        val effects =
          set.collect { case Labeled(ident, ExprType.Effect(expr)) =>
            ident.name -> expr
          }

        val pure = set.collect { case Labeled(ident, ExprType.Pure(expr)) =>
          ident.name -> expr
        }

        Parallel.Parallelized(
          effects,
          pure,
          parallelizeNodes(tail, yieldExpr)
        )

      case Nil =>
        Parallel.Raw(yieldExpr)
    }

  // collapse adjacent stages of all pure nodes
  def compress[A](nodes: List[List[Labeled[A]]]): List[List[Labeled[A]]] = {
    @tailrec
    def loop(nodes: List[List[Labeled[A]]], acc: List[List[Labeled[A]]]): List[List[Labeled[A]]] =
      nodes match {
        case a :: b :: tail =>
          if (b.forall(_.expr.isPure))
            loop((a ++ b) :: tail, acc)
          else
            loop(b :: tail, a :: acc)

        case a :: tail =>
          loop(tail, a :: acc)

        case Nil =>
          acc
      }

    loop(nodes, List.empty).reverse
  }

  def topSort[A](nodes: List[Node[A]]): List[List[Labeled[A]]] = {
    @tailrec
    def loop(nodes: List[Node[A]], acc: List[List[Labeled[A]]]): List[List[Labeled[A]]] =
      if (nodes.isEmpty) acc
      else {
        val (ready, pending) = nodes.partition(_.references.isEmpty)
        val remaining =
          pending.map { case Node(references, expr, output) =>
            // remove all references to nodesWithNoDependencies
            val filteredReferences = references.filterNot(ready.map(_.output).contains)
            Node(filteredReferences, expr, output)
          }
        val labeled = ready.map { case Node(_, expr, output) => Labeled(output, expr) }
        loop(remaining, labeled :: acc)
      }

    loop(nodes, List.empty).reverse
  }

  // println(topSort(nodes).mkString("\n"))

}
