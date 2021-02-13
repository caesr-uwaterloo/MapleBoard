
package components

import chisel3._
import params.{CoherenceSpec, MemorySystemParams}

class PipeData[S <: Data, M <: Data, B <: Data](val m: MemorySystemParams, val cohSpec: CoherenceSpec[S, M, B]) extends Bundle {
  val src = ESource()
  val core = m.getGenCacheReq
  val mem = m.getGenMemRespCommand
  val snoop = m.getGenSnoopReq
  val tr = new TagCheckResult(m, cohSpec)
  val address = m.cacheParams.genAddress
  val data = m.cacheParams.genCacheLineBytes
  val coh_resp = new CoherenceResponse(cohSpec.getGenStateF, cohSpec.getGenBusReqTypeF)
  val freeMSHR = UIntHolding(m.MSHRSize)
  val freePendingMem = UIntHolding(m.pendingMemSize)
  val isReplacement = Bool()
  val isDedicatedWB = Bool()
  override def toPrintable: Printable = p"PipeData(SRC=${src}, \n${core}, \n${mem}, \n${tr}, \n${Hexadecimal(address)})"
}
