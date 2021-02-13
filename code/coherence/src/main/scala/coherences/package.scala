
import chisel3._
import chisel3.util._
import coherence.internal._
import types._

import scala.language.experimental.macros
import chisel3.experimental.ChiselEnum
import components.CoherenceResponse.CoherenceResponseShadow

package object coherences {
  @AsAutoEnum
  trait CoherenceStates extends AutoEnum {
    val I: Int

    def getReplacementStates: List[Int] = List()
    def getWBStates: List[Int] = List()
  }
  object CoherenceStates extends CoherenceStates

  @AsAutoEnum
  trait MSI extends CoherenceStates { val S, M: Int
    override def getReplacementStates: List[Int] = super.getReplacementStates ++ List(S, M)
  }
  object MSI extends MSI

  @AsAutoEnum
  trait PMSI extends MSI{
    val IS_AD, IS_D, IS_DI, IS_UI,
    IM_AD, IM_D, IM_A,
    IM_DI, IM_DSI, IM_DUI, IM_UI, IM_DUS, IM_US,
    MI_WB, MS_WB,
    MI_A, MS_A,
    SM_W, IM_W : Int
    override def getWBStates: List[Int] = List(MI_A, MS_A, SM_W, IM_W)
  }
  object PMSI extends PMSI

  @AsAutoEnum
  trait MESI extends CoherenceStates { val S, E, M: Int
    override def getReplacementStates: List[Int] = super.getReplacementStates ++ List(S, M)
  }
  object MESI extends MESI

  @AsAutoEnum
  trait PMESI extends MESI {
        val IS_AD, IS_D, IS_DI, IS_UI,
    IM_AD, IM_D, IM_A,
    IM_DI, IM_DSI, IM_DUI, IM_UI, IM_DUS, IM_US, // 16
    IM_AD_AMO, IM_D_AMO, IM_DI_AMO, MI_AMO, M_AMO,
    MI_WB, MS_WB, //23
    MI_A, MS_A,
    SM_W, IM_W, //27
    MM,
    IS,
    IM,
    MI,
    II,
    II_D,
    EI_A, ES_A,
    SI_A: Int // 35
    override def getReplacementStates: List[Int] = super.getReplacementStates ++ List(E, MS_WB)

    override def getWBStates: List[Int] = List(MI_A, MS_A, SM_W, IM_W, EI_A, ES_A, SI_A)
  }
  object PMESI extends PMESI

  @AsAutoEnum
  trait CARP extends MSI {
    val IS_AD, IS_D, IS_DI, IS_UI,
    IM_AD, IM_D, IM_A,
    IM_DI, IM_DSI, IM_DUI, IM_UI, IM_DUS, IM_US, // 16
    MI_WB, MS_WB, //23
    MS_WB_E,
    MI_WB_E,
    IM_DUS_E, IM_US_E,
    MI_A, MS_A, MS_A_E,
    SM_W, IM_W: Int // 35
    override def getReplacementStates: List[Int] = super.getReplacementStates ++ List(MS_WB)

    override def getWBStates: List[Int] = List(MI_A, MS_A, SM_W, IM_W, MS_A_E)
  }
  object CARP extends CARP


  @AsAutoEnum
  trait PMESILLC extends AutoEnum {
    val Invalid, Xclusive, Shared: Int
  }
  object PMESILLC extends PMESILLC


  // object PR extends CoherenceDSLComponents[_ <: Data, _ <: Data, _ <: Data] {
  // }

}
