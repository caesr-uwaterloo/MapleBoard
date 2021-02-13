
package components

import chisel3._
import params.MemorySystemParams

class MemoryControllerWBBufferEntry(memorySystemParams: MemorySystemParams) extends Bundle {
  val address = memorySystemParams.cacheParams.genAddress
  val line = memorySystemParams.cacheParams.genCacheLine
  val requestor = UIntHolding(memorySystemParams.masterCount)
  val dirty = Bool()

  override def cloneType: MemoryControllerWBBufferEntry.this.type = new MemoryControllerWBBufferEntry(memorySystemParams).asInstanceOf[this.type]
}
