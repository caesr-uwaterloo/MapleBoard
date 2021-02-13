
package components

import chisel3._
import params.{CoherenceSpec, MemorySystemParams}

class PendingMemoryRequestEntry[S <: Data, M <: Data, B <: Data](
                                                     val m: MemorySystemParams,
                                                     val cohSpec: CoherenceSpec[S, M, B]
                                                     ) extends Bundle {
  val tag = m.cacheParams.genTag
  val state = cohSpec.getGenState
  val busRequestType = cohSpec.getGenBusReqType
  val way = UIntHolding(m.cacheParams.nWays)
  val valid = Bool()
  val issued = Bool()
  val dirty = Bool()
}
