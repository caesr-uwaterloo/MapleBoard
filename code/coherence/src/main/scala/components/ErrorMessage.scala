
package components

import chisel3._
import chisel3.util.Decoupled

class ErrorMessage extends Bundle {
  val valid = Bool()
  val src = UInt(EventSource.getWidth.W)
  val msg = UInt(32.W)

  override def cloneType: this.type = new ErrorMessage().asInstanceOf[this.type]
}
