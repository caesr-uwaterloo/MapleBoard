
package components

import chisel3._
import chisel3.experimental._
import params.{CoherenceSpec, MemorySystemParams}

object TagCheckResult extends ChiselEnum {
  val hit, hitPendingMem, missVacant, missFull = Value
}
class TagCheckResult[S <: Data, M <: Data, B <: Data](val m: MemorySystemParams, val cohSpec: CoherenceSpec[S, M, B])
  extends Bundle {
  val result = TagCheckResult()
  val way = UIntHolding(m.cacheParams.nWays)
  val tagEntry = new TagEntry(m, cohSpec)
  val pendingMemEntry = new PendingMemoryRequestEntry(m, cohSpec)
  val hitPendingMemId = UIntHolding(m.pendingMemSize)
}

