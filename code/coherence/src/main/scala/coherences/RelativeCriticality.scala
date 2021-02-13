
package coherences

import chisel3._
import chisel3.experimental._

object RelativeCriticality extends ChiselEnum {
  val SameCrit, LoCrit, HiCrit = Value
}
