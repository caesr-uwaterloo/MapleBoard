
import chisel3._
import coherences.PipelinedBareMemorySubsystem
import chiseltest._
import components._
import _root_.core.{AMOOP, MemoryRequestType}
import chisel3.experimental.BundleLiterals._

package object testutil {

  type DUT = PipelinedBareMemorySubsystem[_, _, _]
  def setUpPipelinedBareMemorySubsystem(c: DUT): Unit = {
    // Setup so that the initialization is done properly
    val masterCount = c.io.core.request_channel.size
    for {i <- 0 until masterCount} {
      c.io.core.request_channel(i).initSource().setSourceClock(c.clock)
      c.io.core.response_channel(i).initSink().setSinkClock(c.clock)
    }
    c.clock.step(512)
  }
  def constructRequest(c: DUT,
                       address: Int,
                       data: BigInt,
                       memType: MemoryRequestType.Type,
                       amoOp: AMOOP.Type = AMOOP.none,
                       length: Int = 3,
                       use_strb: Boolean = false,
                       strb: BigInt = BigInt(0)
                      ): CacheReq = {
    assert(length <= 3)
    val isAMO = amoOp.litOption() match {
      case Some(v) => if(v == AMOOP.none.litValue()) { false } else { true }
      case _ => false
    }
    val newMemType = if(
      memType.litValue()  == MemoryRequestType.lr.litValue() || memType.litValue() == MemoryRequestType.sc.litValue()
    ) {
      MemoryRequestType.amo
    } else {
      memType
    }
    chiselTypeOf(c.io.core.request_channel(0).bits).Lit(
      _.address -> address.U,
      _.amo_alu_op -> amoOp,
      _.data -> data.U(64.W),
      _.length -> length.U,
      _.mem_type -> newMemType,
      _.is_amo -> isAMO.B,
      _.flush  -> false.B,
      _.llcc_flush -> false.B,
      _.aq -> 0.U,
      _.rl -> 0.U,
      _.use_wstrb -> use_strb.B,
      _.wstrb -> strb.U
    )

  }

}
