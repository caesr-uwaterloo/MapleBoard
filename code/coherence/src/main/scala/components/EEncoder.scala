
package components

import chisel3._
import _root_.core.MemoryRequestType
import coherences.CoherenceMessage
import params.MemorySystemParams

object EEncoder {
  class EEncoderIO(val m: MemorySystemParams) extends Bundle {
    val src = Input(ESource())
    val tag = Input(TagCheckResult())
    val core = Input(m.getGenCacheReq)
    val snoop = Input(m.getGenSnoopReq)
    val mem = Input(m.getGenMemRespCommand)
    val event = Output(CoherenceMessage())

    val id = Input(m.getGenRequestorId)
    val isDedicatedWB = Input(Bool())
  }
}

// encode different events
class EEncoder(val m: MemorySystemParams) extends Module {
  val io = IO(new EEncoder.EEncoderIO(m))
  io.event := CoherenceMessage.NONE_CACHE_EVENT
  when(io.src === ESource.core) {
    when(io.tag === TagCheckResult.missFull) {
      io.event := CoherenceMessage.Replacement
    }.otherwise {
      when(io.core.mem_type === MemoryRequestType.read) {
        io.event := CoherenceMessage.Load
      }.elsewhen(io.core.mem_type === MemoryRequestType.write || io.core.is_amo) {
        io.event := CoherenceMessage.Store
      }.otherwise {
        assert(false.B)
      }
    }
  }.elsewhen(io.src === ESource.snoop) {
    when(io.isDedicatedWB) {
      io.event := CoherenceMessage.OwnPutM
    }.elsewhen(io.snoop.requester_id === io.id) {
      // from self
      when(io.snoop.req_type === RequestType.GETM.U) {
        io.event := CoherenceMessage.OwnGetM
      }.elsewhen(io.snoop.req_type === RequestType.GETS.U) {
        io.event := CoherenceMessage.OwnGetS
      }.elsewhen(io.snoop.req_type === RequestType.UPG.U) {
        io.event := CoherenceMessage.OwnUpg
      }.elsewhen(io.snoop.req_type === RequestType.PUTM.U) {
        io.event := CoherenceMessage.OwnPutM
      }.elsewhen(io.snoop.req_type === RequestType.PUTS.U) {
        io.event := CoherenceMessage.OwnPutS
      }.otherwise {
        assert(false.B, "Invalid coherence message (snoop.req_type)")
      }
    }.otherwise {
      // from other
      when(io.snoop.req_type === RequestType.GETM.U) {
        io.event := CoherenceMessage.OtherGetM
      }.elsewhen(io.snoop.req_type === RequestType.GETS.U) {
        io.event := CoherenceMessage.OtherGetS
      }.elsewhen(io.snoop.req_type === RequestType.UPG.U) {
        io.event := CoherenceMessage.OtherUpg
      }.elsewhen(io.snoop.req_type === RequestType.PUTM.U) {
        io.event := CoherenceMessage.OtherPutM
      }.elsewhen(io.snoop.req_type === RequestType.PUTS.U) {
        io.event := CoherenceMessage.OtherPutS
      }.otherwise {
        assert(false.B, "Invalid coherence message (snoop.req_type: %d)", io.snoop.req_type)
      }
    }
  }.elsewhen(io.src === ESource.mem) {
    when(io.mem.ack === 0.U) {
      when(io.mem.is_edata === 1.U) {
        io.event := CoherenceMessage.EData
      }.otherwise {
        io.event := CoherenceMessage.Data
      }
    }.otherwise { // ack is one
      io.event := CoherenceMessage.Ack
    }
  }
}
