
package components

import chisel3._
import chisel3.util._

object UIntHolding {
  def apply(maximal: Int): UInt = {
    if(maximal > 1) {
      UInt(log2Ceil(maximal).W)
    } else if(maximal == 1) {
      UInt(1.W)
    } else {
      throw new RuntimeException("The maximal number UInt holds must be positive")
    }
  }
}
