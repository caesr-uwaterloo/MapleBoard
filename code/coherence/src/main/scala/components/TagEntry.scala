
package components

import chisel3._
import params.{CoherenceSpec, MemorySystemParams}

class TagEntry(val m: MemorySystemParams, val cohSpec: CoherenceSpec[_ <: Data, _ <: Data, _ <: Data]) extends Bundle {
  val tag = UInt(m.cacheParams.tagWidth.W)
  val state = cohSpec.getGenState
  val dirty = Bool()
}
