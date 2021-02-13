
package components

import chisel3.experimental._

/**
  * Event source
  */
object ESource extends ChiselEnum {
  val core, snoop, mem = Value
}
