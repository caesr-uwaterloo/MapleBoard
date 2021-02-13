
package components

import chisel3._
import chisel3.experimental.BundleLiterals._
import coherences.RelativeCriticality

class CoherenceQuery[M <: Data, S <: Data](
                      private val genM: () => M,
                      private val genS: () => S
                    ) extends Bundle {
  val state = genS()
  val message = genM()
  val relCrit = RelativeCriticality() // <--- Note we don't use this in normal protocols

  override def cloneType: CoherenceQuery.this.type = new CoherenceQuery(genM, genS).asInstanceOf[this.type]
}

object CoherenceQuery {
  def apply[M <: Data, S <: Data](message: M, state: S): CoherenceQuery[M, S] =
    new CoherenceQuery(() => chiselTypeOf(message), () => chiselTypeOf(state)).Lit(
      _.message -> message,
      _.state -> state,
      _.relCrit -> RelativeCriticality.SameCrit
    )
  def apply[M <: Data, S <: Data](message: M, state: S, rel: RelativeCriticality.Type): CoherenceQuery[M, S] =
    new CoherenceQuery(() => chiselTypeOf(message), () => chiselTypeOf(state)).Lit(
      _.message -> message,
      _.state -> state,
      _.relCrit -> rel
    )
}
