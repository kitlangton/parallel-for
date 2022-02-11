import parallelfor.internal.Macro

import scala.language.experimental.macros

package object parallelfor {
  def par[F[-_, +_, +_], R, E, A](effect: F[R, E, A])(implicit parallelizable: Parallelizable[F]): F[R, E, A] =
    macro Macro.parallelizeImpl[F, R, E, A]

  def par1[F[-_, +_, +_], G[+_], A](effect: G[A])(implicit
      parallelizable: Parallelizable[F]
  ): F[Any, Nothing, A] =
    macro Macro.parallelizeImpl[F, Any, Nothing, A]
}
