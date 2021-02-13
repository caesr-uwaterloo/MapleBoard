
package components

import chisel3.experimental.ChiselEnum

object BusRequestType extends ChiselEnum {
  val NONE_EVENT, GetS, GetM, PutM, Upg, MemData, MemAck, Inv, PutS, GetSE = Value
}
