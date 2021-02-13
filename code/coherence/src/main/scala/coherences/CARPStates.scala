
package coherences

import chisel3.util._
import chisel3._
import chisel3.experimental._

object CARPStates extends ChiselEnum {
  val I, S, M,
  IS_AD, IS_D, IS_DI, IS_UI,
  IM_AD, IM_D,
  IM_DI, IM_DS, IM_DSI, IM_DUI, IM_UI, IM_DUS, IM_US,
  IM_DS_E,
  MI_A, MS_A, MS_A_E, MI_A_E,
  SM_W, IM_W,
  SM_W_A, SM_WS, SM_WI = Value

  val M_ = List(MI_A, MS_A, MS_A_E, MI_A_E)
  // these states are conceptually I from the perspective of the private cache
  val I_ = List( IM_AD, IS_AD, IS_D, IM_DI, IM_DS)
}
