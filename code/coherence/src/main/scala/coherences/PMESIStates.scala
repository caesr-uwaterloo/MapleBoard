
package coherences

import chisel3.experimental.ChiselEnum

object PMESIStates extends ChiselEnum {
  val I, S, E, M, IS_AD, IS_D, IS_DI, IS_UI,
  IM_AD, IM_D, IM_A,
  IM_DI, IM_DSI, IM_DS,
  MI_WB, MS_WB,
  MI_A, MS_A,
  SM_W, SM_W_A, SM_WS, SM_WI,
  EI_A, ES_A,
  SI_A = Value

  val M_ = List(MI_A, MI_WB, MS_A, MS_WB)
  // these states are conceptually I from the perspective of the private cache
  val I_ = List(IM_A, IM_AD, IS_AD, IS_D, IM_DI, IM_DS)
}
