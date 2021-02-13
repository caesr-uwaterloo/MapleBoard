
package components

import chisel3._
import chisel3.util._
import coherence.internal.{AsAutoEnum, AutoEnum}
import params.{L1CacheParams, MemorySystemParams, SimpleCacheParams}
import coherences.{PMESI => CoherenceState}
import _root_.core.MemoryRequestType
import _root_.utils.EnumMuxLookup

@AsAutoEnum
trait CacheEventBase extends AutoEnum {
  val NONE_CACHE_EVENT, LOAD, STORE, AMO, OWN_GETS, OWN_GETM, OTHER_GETS, OTHER_GETM, OWN_PUTM,
  OTHER_PUTM, OWN_UPG, OTHER_UPG, DATA, ACK, FLUSH, LLCC_FLUSH, EDATA, REPLACEMENT, OWN_PUTS, TIMEOUT : Int

}
object CacheEventBase extends CacheEventBase

trait CacheEvent extends CacheEventBase {
  override def getWidth: Int = 5
}
object CacheEvent extends CacheEventBase

@AsAutoEnum
trait CriticalityDiffBase extends AutoEnum {
  val HICRIT, LOCRIT, SAMECRIT: Int
}
object CriticalityDiffBase extends CriticalityDiffBase

trait CriticalityDiff extends CriticalityDiffBase {
  override def getWidth: Int = 3
}
object CriticalityDiff extends CriticalityDiff


@AsAutoEnum
trait EventSource extends AutoEnum { val NON, CACHE, SNOOP, MEMORY: Int }
object EventSource extends EventSource

//////////////////////////////////////////////////////////////////
// event encoder
//////////////////////////////////////////////////////////////////
class EventEncoder(private val depth: Int, private val addrWidth: Int,
                   private val lineWidth: Int,
                   private val masterCount: Int, // current ID
                   private val cacheParams: L1CacheParams,
                   private val genCacheReq: CacheReq,
                   private val genMemResp: MemResp,
                   private val genSnoopReq: SnoopReq,
                   private val memorySystemParams: MemorySystemParams) extends Module {
  val io = IO(new Bundle {
    // input list
    val cc_state  = Input(UInt(CacheControllerState.getWidth.W))
    val is_there_pending_req = Input(Bool())
    val cachereq_data_reg = Input(genCacheReq)
    val memresp_fire = Input(Bool())
    val memresp_data_i = Input(genMemResp)
    val snoop_req_fire = Input(Bool())
    val snoop_req_data_i = Input(genSnoopReq)
    val cachereq_fire = Input(Bool())
    val cachereq_valid = Input(Bool())
    // this is different from cachereq_data_reg
    // this is the interface, not the registered cache request
    val cachereq_data_i = Input(genCacheReq)
    val id = Input(UInt((log2Ceil(masterCount) + 1).W))
    val tag_match = Input(Bool())
    val line_state = Input(UInt(CoherenceState.getWidth.W))
    val this_crit = Input(UInt(3.W))
    val that_crit = Input(UInt(3.W))

    val event_valid = Output(Bool())
    val can_valid = Output(Bool())
    val cache_event = Output(UInt(CacheEvent.getWidth.W))
    val event_src = Output(UInt(EventSource.getWidth.W))
    val memresp_en = Output(Bool())
    val snoop_req_en = Output(Bool())
    val cachereq_en = Output(Bool())
    val line_addr = Output(UInt(log2Ceil(depth).W))
    val address = Output(UInt(cacheParams.addrWidth.W))
    val crit_diff = Output(UInt(3.W))
  })
  io.crit_diff := memorySystemParams.criticalityComparison(
    io.this_crit,
    io.that_crit
  )


  /* Helper functions */
  def issueEvent(event: UInt): Unit = {
    io.event_valid := true.B
    io.cache_event := event
  }

  def setAddress(address: UInt): Unit = {
    io.line_addr := cacheParams.getLineAddress(address)
    io.address := address
  }

  // internal signals
  // default value
  io.event_valid := false.B
  io.can_valid := false.B
  io.cache_event := CacheEvent.NONE_CACHE_EVENT.U
  io.event_src := EventSource.NON.U

  io.memresp_en := false.B
  io.snoop_req_en := false.B
  io.cachereq_en := false.B

  io.line_addr := 0.U
  io.address := 0.U

  val need_replacement = ( for{ wbStates <- CoherenceState.getReplacementStates }
    yield wbStates.U === io.line_state ).reduce( (a: Bool, b: Bool) => {
    a || b
  })

  when(io.cc_state === CacheControllerState.IDLE_2 && io.is_there_pending_req) {
    // In this case, the data has already been fetched, it must be a match // not necessarily...
    io.event_src := EventSource.MEMORY.U
    setAddress(io.cachereq_data_reg.address)
    io.can_valid := true.B
    when(io.tag_match || io.line_state === CoherenceState.I.U) { // either the data is back
      when(io.cachereq_data_reg.is_amo) {
        issueEvent(CacheEvent.STORE.U)
      }.otherwise {
        issueEvent(EnumMuxLookup(io.cachereq_data_reg.mem_type, CacheEvent.LOAD.U,
          Seq(MemoryRequestType.read -> CacheEvent.LOAD.U,
            MemoryRequestType.write -> CacheEvent.STORE.U)))
      }
    }
  }.elsewhen(io.memresp_fire) {
    io.memresp_en := true.B
    io.event_src := EventSource.MEMORY.U
    io.event_valid := true.B
    io.can_valid := true.B
    setAddress(io.cachereq_data_reg.address)
    when(io.tag_match) {
      when(!io.memresp_data_i.ack) {
        io.cache_event := CacheEvent.ACK.U
      }.elsewhen(io.memresp_data_i.is_edata.toBool()) {
        // Change to EDATA for PMESI
        io.cache_event := CacheEvent.EDATA.U
      }.otherwise {
        io.cache_event := CacheEvent.DATA.U
      }
    }.otherwise {
      printf("[EventEncoder] receiving UNKNOWN data from bus, this is a failure\n")
    }
  }.elsewhen(io.snoop_req_fire) {
    io.snoop_req_en := true.B
    io.event_src := EventSource.SNOOP.U
    io.can_valid := true.B
    setAddress(io.snoop_req_data_i.address)
    when(io.tag_match) {
      when(io.snoop_req_data_i.requester_id === io.id) { // OWN EVENTs
        io.event_valid := true.B
        io.cache_event := MuxLookup(io.snoop_req_data_i.req_type, CacheEvent.NONE_CACHE_EVENT.U,
          Array(
            RequestType.GETS.U -> CacheEvent.OWN_GETS.U,
            RequestType.GETM.U -> CacheEvent.OWN_GETM.U,
            RequestType.UPG.U -> CacheEvent.OWN_UPG.U,
            RequestType.PUTM.U -> CacheEvent.OWN_PUTM.U,
            RequestType.PUTS.U -> CacheEvent.OWN_PUTS.U
          )
        )
      }.otherwise { // OTHER EVENTs
        io.event_valid := true.B
        io.cache_event := MuxLookup(io.snoop_req_data_i.req_type, CacheEvent.NONE_CACHE_EVENT.U,
          Array(
            RequestType.GETS.U -> CacheEvent.OTHER_GETS.U,
            RequestType.GETM.U -> CacheEvent.OTHER_GETM.U,
            RequestType.UPG.U -> CacheEvent.OTHER_UPG.U,
            RequestType.PUTM.U -> CacheEvent.OTHER_PUTM.U,
          )
        )
      }
    }
    // otherwise, the line is irrelevant, but it is not an error
  }.elsewhen(io.cachereq_valid && io.cc_state === CacheControllerState.IDLE) {
    io.cachereq_en := io.cachereq_fire
    io.event_src := EventSource.CACHE.U
    io.event_valid := io.cachereq_fire
    io.can_valid := true.B
    setAddress(io.cachereq_data_i.address)
    when(need_replacement && !io.tag_match) {
      io.cache_event := CacheEvent.REPLACEMENT.U
    }.otherwise {
      when(io.cachereq_data_i.is_amo) {
        io.cache_event := CacheEvent.STORE.U
      }.otherwise {
        io.cache_event := EnumMuxLookup(io.cachereq_data_i.mem_type,
          CacheEvent.LOAD.U,
          Seq(MemoryRequestType.read -> CacheEvent.LOAD.U,
            MemoryRequestType.write -> CacheEvent.STORE.U))
      }
    }
  }.otherwise {
  }


}
