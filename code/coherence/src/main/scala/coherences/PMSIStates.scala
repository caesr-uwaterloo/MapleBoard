
package coherences

import chisel3._
import chisel3.experimental._

object PMSIStates extends ChiselEnum {
  val I, S, M, IS_AD, IS_D, IS_DI, IS_UI,
  IM_AD, IM_D, IM_A,
  IM_DI, IM_DSI, IM_DUI, IM_UI, IM_DUS, IM_US, IM_DS,
  MI_WB, MS_WB,
  MI_A, MS_A,
  SM_W, SM_W_A, SM_WS, SM_WI = Value

  val M_ = List(MI_A, MI_WB, MS_A, MS_WB)
  // these states are conceptually I from the perspective of the private cache
  val I_ = List(IM_A, IM_AD, IS_AD, IS_D, IM_DI, IM_DS)
}

