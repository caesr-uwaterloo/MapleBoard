
package core

import chisel3.experimental.ChiselEnum

object ALUIn1Bypass extends ChiselEnum {
  val nobypass, mx, wx = Value
}
object ALUIn2Bypass extends ChiselEnum {
  val nobypass, mx, wx = Value
}

object MemoryBypass extends ChiselEnum {
  val nobypass, wm = Value
}

class BypassControl {
  val aluIn1Bypass = ALUIn1Bypass()
  val aluIn2Bypass = ALUIn2Bypass()
  val memBypass = MemoryBypass()
}
