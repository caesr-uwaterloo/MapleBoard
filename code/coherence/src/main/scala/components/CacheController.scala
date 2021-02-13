
package components

import chisel3._
import chisel3.SyncReadMem
import chisel3.util._
import params.{MemorySystemParams, SimpleCacheParams}
import coherence.internal.{AsAutoEnum, AutoEnum}
import coherences.{PMESI => CoherenceState}
import _root_.core.AMOOP
import _root_.core.{AMOALU => SimpleAMOALU}
import param.CoreParam
// import dbgutil.exposeTop

import language.dynamics
import scala.collection.mutable



object CacheControllerState extends Dynamic {

  private val states = new mutable.HashMap[String, UInt]
  val sList: List[String] = "IDLE" ::
  "MEANING_LESS_STATE" ::
  "ADD_TO_WB_QUEUE" ::
  "ADD_TO_REQ_QUEUE" ::
  "ADD_TO_CACHE_RESP_QUEUE" ::
  "ADD_TO_CACHE_RESP_QUEUE_STORE" ::
  "ADD_TO_CACHE_RESP_AND_WB_QUEUE" ::
  "ADD_TO_CACHE_SNOOP_RESP_QUEUE" ::
  "ADD_TO_TAG_ARRAY" ::
  "UPDATE_CACHE" ::
  "REFILL_CACHE" ::
  "IDLE_2" ::
  "PUSH_DATA_TO_WB_QUEUE" ::
  "WRITE_TO_CACHE" ::
  "ADD_TO_MEMORY_QUEUE" ::
  "ADD_TO_PR_LOOKUP_TABLE" ::
  "ADD_TO_MEM_RESP" ::
  "WAKEUP_DEP" ::
  "ADD_TO_MEM_RESP_2" ::
  "WRITE_BACK" ::
  "REMOVE_FROM_CACHE" ::
  "CACHE_FLUSH" ::
  "CACHE_FLUSH_UPDATE" ::
  "CACHE_FLUSH_WAIT" ::
  "ADD_FLUSH_TO_WB_QUEUE" ::
  "LLCC_FLUSH_DONE" ::
  "WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q" ::
  "READ_DATA_ARRAY" ::
  "LOOKUP_TABLE_REQ_WR_WAIT" ::
  "LOOKUP_TABLE_REQ_RD_WAIT" ::
  "ADD_TO_MEMORY_QUEUE_2" ::
  "IMPOSSIBLE" ::
  "PUSH_DATA_TO_LO_CRIT_WB_QUEUE" ::
  "ADD_TO_LO_CRIT_WB_QUEUE" ::
  "WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q_LO_CRIT" ::
  "READ_DATA_ARRAY_LO_CRIT" ::
  "REISSUE" :: Nil

  for((x, i) <- sList.zipWithIndex) {
    states(x) = i.U(width = getWidth.W)
  }

  def selectDynamic(name: String): UInt = states(name)
  def getWidth: Int = log2Ceil(sList.length)
  def getName(v: Int): String = sList(v)
}




@AsAutoEnum
trait RequestTypeBase extends AutoEnum {
  val NONE_EVENT, GETS, GETM, PUTM, UPG, MEMORY_DATA, MEMORY_ACK, INVALIDATE, PUTS: Int
}
object RequestTypeBase extends RequestTypeBase

trait RequestType extends RequestTypeBase {
  override def getWidth: Int = 5
}
object RequestType extends RequestType

class CacheController(coreParam: CoreParam, memorySystemParams: MemorySystemParams, id: Int) extends Module {
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams
  val depth: Int = cacheParams.nSets
  val lineSize: Int = cacheParams.lineBytes
  val addrWidth : Int = memorySystemParams.addrWidth
  val masterCount: Int = memorySystemParams.masterCount
  val dataWidth: Int = memorySystemParams.dataWidth
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val genCacheReq: CacheReq = memorySystemParams.getGenCacheReq
  val genCacheResp: CacheResp= memorySystemParams.getGenCacheResp
  val genControllerReq: MemReq = memorySystemParams.getGenMemReq
  val genControllerReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
  val genControllerResp: MemResp = memorySystemParams.getGenMemResp
  val genSnoopReq: SnoopReq = memorySystemParams.getGenSnoopReq
  val genSnoopResp: SnoopResp = memorySystemParams.getGenSnoopResp
  val genDebugCacheLine: DebugCacheline = memorySystemParams.getGenDebugCacheline
  val wordWidth = coreParam.isaParam.XLEN

  val nrtCores = 1

  def _constructStrobe(addr: UInt, len: UInt): UInt = {
    val bits = WireInit(0.U((cacheParams.lineWidth / 8).W))
    val shamtWidth = log2Ceil(cacheParams.lineWidth / 8)
    switch(len) {
      // not using for-loop for clarity
      is(0.U) { bits := "b1".U }
      is(1.U) { bits := "b11".U }
      is(2.U) { bits := "b1111".U }
      is(3.U) { bits := "b11111111".U }
    }
    (bits << addr(shamtWidth - 1, 0)).asUInt()
  }

  def _constructResponse(addr: UInt, data: UInt): UInt = {
    val shamtWidth = log2Ceil(wordWidth / 8)

    // printf(p"[CC ${io.id}] addr ${Hexadecimal(addr)}, data ${Hexadecimal(data)}, shamt: ${Hexadecimal(Cat(addr(shamtWidth - 1, 0), "b000".U(3.W)))}\n")
    (data >> Cat(addr(shamtWidth - 1, 0), "b000".U(3.W)) ).asUInt()
  }

  def _constructData(addr: UInt, data: UInt): UInt = {
    val shamtWidth = log2Ceil(wordWidth / 8)
    val d = WireInit(0.U(wordWidth.W))

    // printf(p"[CC ${io.id}] addr ${Hexadecimal(addr)}, data ${Hexadecimal(data)}, shamt: ${Hexadecimal(Cat(addr(shamtWidth - 1, 0), "b000".U(3.W)))}\n")
    d := (data << Cat(addr(shamtWidth - 1, 0), "b000".U(3.W)) ).asUInt()
    d
  }

  def _floorAligned(addr: UInt, alignmentBit: Int): UInt = {
    val width = addr.getWidth
    Cat(addr(width - 1, alignmentBit), 0.U(alignmentBit.W))
  }


  val (lrReqCounter, lrReqCounterWrap) = Counter(true.B, memorySystemParams.slotWidth * memorySystemParams.masterCount)
  val (lrRequestor, lrRequestorWrap) = Counter(lrReqCounterWrap, memorySystemParams.masterCount / 2)

  val io = IO(new Bundle {
    // from core to cc
    val core = new Bundle {
      val request_channel = Flipped(Decoupled(genCacheReq))
      val response_channel = Decoupled(genCacheResp)
    }
    // from cc to bus
    val bus = new Bundle {
      val request_channel = Decoupled(genControllerReqCommand)
      val response_channel = Flipped(Decoupled(genControllerResp))
      val dataq = Decoupled(UInt(busDataWidth.W))
    }

    val snoop = new Bundle {
      val request_channel = Flipped(Decoupled(genSnoopReq))
      val response_channel = Decoupled(genSnoopResp)
    }
    val id = Input(UInt((log2Ceil(masterCount) + 1).W))
  })

  val address_counter = RegInit(0.U)
  val en_counter = RegInit(false.B)
  val overtime_counter = RegInit(0.U(128.W))
  when(io.core.request_channel.fire()) {
    en_counter := true.B
    address_counter := io.core.request_channel.bits.address
  }.elsewhen(io.core.response_channel.fire()) {
    en_counter := false.B
    overtime_counter := 0.U
  }.otherwise {
    when(en_counter) {
      overtime_counter := overtime_counter + 1.U
    }
  }

  val thisCrit = memorySystemParams.coreIdToCriticality(io.id)
  val thatCrit = Wire(UInt(3.W))
  // 3 * n ^ 2 * slotwidth
  // too long for a request...
  when(overtime_counter >= (3 * memorySystemParams.masterCount * memorySystemParams.masterCount * memorySystemParams.slotWidth).U) {
    when(!memorySystemParams.isLowCrit(thisCrit)) {
      assert(false.B, "not response found: cache %d, address: %x", io.id, address_counter)
    }
  }


  val canLR = ((lrRequestor << 1.U).asUInt + 1.U) === io.id

  val event_encoder = Module(new EventEncoder(cacheParams.nSets, addrWidth, cacheParams.lineWidth, masterCount,
    cacheParams, genCacheReq, genControllerResp, genSnoopReq, memorySystemParams))
  val coherence_table = Module(
    if(memorySystemParams.withCriticality && memorySystemParams.isLowCrit(memorySystemParams.coreIdToCriticality(id))) {
      memorySystemParams.lowCritCoherenceTable()
    } else {
      memorySystemParams.coherenceTable()
    }
  )

  val state_machine = Module(new CacheControllerStateMachine(masterCount, dataWidth, cacheParams,
    genCacheReq, genControllerReqCommand, genSnoopResp, genCacheResp, memorySystemParams, coherence_table.CohS))
  val tag_array_cache_inst = Module(new TagArray(cacheParams))
  val data_array_cache_inst = Module(new DataArray(cacheParams))
  val data_array = SyncReadMem(cacheParams.nSets, Vec(cacheParams.lineWidth / 8, UInt(8.W)))
  val event_valid = WireInit(event_encoder.io.event_valid)
  val event = WireInit(event_encoder.io.cache_event)

  // registers related to state machine are absorbed into the CacheControllerStateMachine
  // these are mostly data path registers
  val lrsc_valid = RegInit(0.U)
  val lrsc_address = RegInit(0.U(addrWidth.W))
  val lrsc_counter = RegInit(0.U(dataWidth.W))
  val address_reg = Reg(UInt(dataWidth.W))
  val line_addr_reg = Reg(UInt(cacheParams.lineAddrWidth.W))
  val cachereq_data_reg = Reg(genCacheReq)
  val hit_wb_q = RegInit(false.B)
  val hit_wb_q_lo_crit = RegInit(false.B)
  val memresp_data_reg = Reg(genControllerResp)
  val snoop_req_data_reg = Reg(genSnoopReq)
  val is_there_pending_req = RegInit(false.B)
  val tag_array_read_data_reg = RegInit(0.U(cacheParams.tagWidth.W))
  val data_array_read_data_registered = RegInit(0.U(cacheParams.lineWidth.W))
  val data_array_read_en_reg = RegInit(false.B)
  val cache_line_valid_reg = RegInit(false.B)
  val cache_line_dirty_reg = RegInit(false.B)
  val data_array_write_byte_en_helper = VecInit.tabulate(cacheParams.lineBytes)(_ => 0.U(1.W))
  val req_fifo = Module(new FIFOv2(masterCount, genControllerReqCommand.getWidthM, 0))
  // val wb_fifo_inst = Module(new FIFOv2(cacheParams.lineAddrWidth, genControllerReq.getWidthM, 0))
  val wb_fifo_inst_new = Module(new WriteBackQueue(cacheParams.nSets * 2 , memorySystemParams))
  // some issue with the pointer calculation, can use a bit of margin
  val wb_fifo_inst_lo_crit = Module(new WriteBackQueue(1 << (log2Ceil(nrtCores + 2)), memorySystemParams))
  val cache_resp_fifo_inst = Module(new FIFOv2(masterCount, genCacheResp.getWidthM, 0))
  val snoop_resp_fifo_inst = Module(new FIFOv2(masterCount, genSnoopResp.getWidthM, 0))
  val rr_arbiter_inst = Module(new TwoBitRRArbiter)
  val amo_alu_inst = Module(new AMOALU(coreParam, cacheParams))

  private val cache_line_valid = state_machine.io.cache_valid_line_addr

  private val cache_line_tag_match = (
    ((state_machine.io.cc_state === CacheControllerState.IDLE) ||
      (state_machine.io.cc_state === CacheControllerState.IDLE_2))
      //&& event_encoder.io.event_valid
      && event_encoder.io.can_valid
      && state_machine.io.cache_valid_line_addr.toBool()
      && (
      tag_array_cache_inst.io.read_data === event_encoder.io.address(addrWidth - 1, cacheParams.lineOffsetWidth) ||
      wb_fifo_inst_new.io.peek.found ||
      wb_fifo_inst_lo_crit.io.peek.found
      )
    )
  private val cache_line_dirty = (
    ((state_machine.io.cc_state === CacheControllerState.IDLE) ||
      (state_machine.io.cc_state === CacheControllerState.IDLE_2))
      // && event_encoder.io.event_valid
      && event_encoder.io.can_valid
      && state_machine.io.cache_valid_line_addr.toBool()
      && state_machine.io.cache_dirty_line_addr.toBool()
    )

  // Otherwise, the line could still be in some other states.
  private val cache_line_state = Mux(cache_line_valid.toBool,
    state_machine.io.cache_state_line_addr, CoherenceState.I.U)
  val cl_state = WireInit(cache_line_state)
  // exposeTop(cl_state)
  // io.line_state := cache_line_state

  private val read_word_mux_sel = cachereq_data_reg.address(cacheParams.lineOffsetWidth - 1, 3)
  private val cache_resp_data = Wire(UInt(dataWidth.W))

  private val data_array_read_data_reg = WireInit(0.U(cacheParams.lineWidth.W))

  private val data_array_data_shiftamt = Cat(cachereq_data_reg.address(log2Ceil(dataWidth / 8) - 1, 0), 0.U(3.W))
  private val data_array_write_data_word = (cachereq_data_reg.data << data_array_data_shiftamt) (addrWidth - 1, 0)
  private val read_word = VecInit(Seq.fill(cacheParams.lineWidth / dataWidth)(WireInit(0.U(dataWidth.W))))
  // val data_array_read_data_wire = data_array.read(data_array_cache_inst.io.read_addr, data_array_cache_inst.io.read_en)
  // by default, generate same crit information
  thatCrit := thisCrit
  when(io.snoop.request_channel.fire()) {
    thatCrit := io.snoop.request_channel.bits.criticality
  }

  state_machine.io.id := io.id
  state_machine.io.event_valid := event_encoder.io.event_valid
  state_machine.io.cache_ctr_next_state  := coherence_table.io.cache_ctr_next_state_o
  state_machine.io.cache_line_next_dirty := coherence_table.io.cache_line_next_dirty_o
  state_machine.io.cache_line_next_valid := coherence_table.io.cache_line_next_valid_o
  state_machine.io.cache_line_broadcast_type := coherence_table.io.cache_line_broadcast_type
  state_machine.io.wb_full_o := !wb_fifo_inst_new.io.q.enq.ready
  state_machine.io.wb_lo_crit_full_o := !wb_fifo_inst_lo_crit.io.q.enq.ready
  state_machine.io.tag_array_read_data_reg := tag_array_read_data_reg
  state_machine.io.data_array_read_data_reg := data_array_read_data_reg
  state_machine.io.cachereq_data_reg := cachereq_data_reg
  state_machine.io.cachereq_data_i := io.core.request_channel.bits
  state_machine.io.req_full_o := req_fifo.io.full_o
  state_machine.io.cache_resp_full_o := cache_resp_fifo_inst.io.full_o
  state_machine.io.cache_resp_data := cache_resp_data
  state_machine.io.amo_result := amo_alu_inst.io.out
  state_machine.io.snoop_resp_full_o := snoop_resp_fifo_inst.io.full_o
  state_machine.io.line_addr_reg := line_addr_reg
  state_machine.io.line_addr := event_encoder.io.line_addr
  state_machine.io.cache_line_next_state := coherence_table.io.cache_line_next_state_o
  // state_machine.io.data_array_debug_read_addr_i := io.debug.data_array_debug_read_addr


  event_encoder.io.cc_state := state_machine.io.cc_state
  event_encoder.io.is_there_pending_req := is_there_pending_req
  event_encoder.io.cachereq_data_reg := cachereq_data_reg
  event_encoder.io.memresp_fire := io.bus.response_channel.fire()
  event_encoder.io.memresp_data_i := io.bus.response_channel.bits
  event_encoder.io.snoop_req_fire := io.snoop.request_channel.fire()
  event_encoder.io.snoop_req_data_i := io.snoop.request_channel.bits
  event_encoder.io.cachereq_fire := io.core.request_channel.fire()
  event_encoder.io.cachereq_valid := io.core.request_channel.valid
  event_encoder.io.cachereq_data_i := io.core.request_channel.bits
  event_encoder.io.id := io.id
  event_encoder.io.tag_match := cache_line_tag_match
  event_encoder.io.line_state := cache_line_state
  event_encoder.io.this_crit := thisCrit
  event_encoder.io.that_crit := thatCrit

  coherence_table.io.cache_line_event_i := event_encoder.io.cache_event
  coherence_table.io.cache_ctr_state_i := state_machine.io.cc_state
  coherence_table.io.cache_line_state_i := cache_line_state
  coherence_table.io.cache_line_tag_match_i := cache_line_tag_match
  coherence_table.io.cache_line_dirty_i := cache_line_dirty
  coherence_table.io.cache_line_valid_i := cache_line_valid
  coherence_table.io.crit_diff := event_encoder.io.crit_diff
  when(event_encoder.io.event_valid) {
    printf("[CC%d] Event: ", io.id)
    for { i<- 0 until CacheEvent.getStateList.length } {
      val st = CacheEvent.getStateList(i)
      when(i.U === event_encoder.io.cache_event) {
        printf(s"$st\n")
      }
    }
    when(io.snoop.request_channel.fire()) {
      printf("[CC%d] CritDiff: ", io.id)
      CriticalityDiff.printState(event_encoder.io.crit_diff)
      printf("\n")
    }
  }

  // assign registers depending on the state
  when(event_encoder.io.cachereq_en) {
    cachereq_data_reg := io.core.request_channel.bits
    address_reg := io.core.request_channel.bits.address
    is_there_pending_req := true.B
    hit_wb_q := wb_fifo_inst_new.io.peek.found
    hit_wb_q_lo_crit := wb_fifo_inst_lo_crit.io.peek.found
  }

  when(io.core.response_channel.fire()) {
    is_there_pending_req := false.B
  }


  // memoryresp
  when(event_encoder.io.memresp_en) {
    memresp_data_reg := io.bus.response_channel.bits
    address_reg := cachereq_data_reg.address
  }

  when(event_encoder.io.snoop_req_en) {
    snoop_req_data_reg := io.snoop.request_channel.bits
    address_reg := io.snoop.request_channel.bits.address
  }

  //tag_array
  when(tag_array_cache_inst.io.read_en) {
    tag_array_read_data_reg := tag_array_cache_inst.io.read_data
  }

  //data_array
  when(data_array_read_en_reg) {
    data_array_read_data_registered := /*data_array_read_data_wire.asUInt*/ data_array_cache_inst.io.read_data
  }
  data_array_read_en_reg := data_array_cache_inst.io.read_en

  when(state_machine.io.event_valid) {
    line_addr_reg := event_encoder.io.line_addr
    cache_line_valid_reg := state_machine.io.cache_valid_line_addr
    cache_line_dirty_reg := state_machine.io.cache_dirty_line_addr
  }

  when(state_machine.io.cc_state === CacheControllerState.CACHE_FLUSH && !wb_fifo_inst_new.io.q.enq.ready) {
    assert(false.B)
    line_addr_reg := line_addr_reg + 1.U
  }


  //////////////////////////////////////////////////////////////////
  // internal signals
  //////////////////////////////////////////////////////////////////

  def getTagWidth: Int = addrWidth - log2Ceil(lineSize)

  when(data_array_read_en_reg) {
    data_array_read_data_reg := /*data_array_read_data_wire.asUInt*/  data_array_cache_inst.io.read_data

  }.otherwise {
    data_array_read_data_reg := data_array_read_data_registered
  }

  //////////////////////////////////////////////////////////////////
  // tag array
  //////////////////////////////////////////////////////////////////
  // tag_array_cache_inst.io.clock_b := io.debug.data_array_debug_clock
  // tag_array_cache_inst.io.read_addr_b := io.debug.data_array_debug_read_addr
  // read
  tag_array_cache_inst.io.clock := clock
  tag_array_cache_inst.io.reset := reset
  tag_array_cache_inst.io.read_en := false.B
  tag_array_cache_inst.io.read_addr := 0.U
  when(event_encoder.io.can_valid && state_machine.io.cache_valid_line_addr.toBool()) {
    tag_array_cache_inst.io.read_en := true.B
    tag_array_cache_inst.io.read_addr := state_machine.io.line_addr
  }.elsewhen(state_machine.io.cc_state === CacheControllerState.CACHE_FLUSH_UPDATE) {
    tag_array_cache_inst.io.read_en := true.B
    tag_array_cache_inst.io.read_addr := line_addr_reg
  }
  // write
  tag_array_cache_inst.io.write_en := false.B
  tag_array_cache_inst.io.write_addr := 0.U
  tag_array_cache_inst.io.write_data := 0.U


  when(state_machine.io.cc_state === CacheControllerState.ADD_TO_TAG_ARRAY) {
    tag_array_cache_inst.io.write_en := true.B
    tag_array_cache_inst.io.write_addr := line_addr_reg
    tag_array_cache_inst.io.write_data := cachereq_data_reg.address(addrWidth - 1, cacheParams.lineOffsetWidth)
  }
  //////////////////////////////////////////////////////////////////
  // write back queue
  //////////////////////////////////////////////////////////////////

  wb_fifo_inst_lo_crit.io.q.enq.bits := state_machine.io.wb_wr_data_lo_crit_i
  wb_fifo_inst_lo_crit.io.q.enq.valid := state_machine.io.wb_wr_en_lo_crit_i
  wb_fifo_inst_lo_crit.io.peek.enable := false.B
  wb_fifo_inst_lo_crit.io.peek.remove := false.B
  wb_fifo_inst_lo_crit.io.peek.read_write := 1.U // default to read
  wb_fifo_inst_lo_crit.io.peek.address := 0.U
  wb_fifo_inst_lo_crit.io.peek.length := 0.U
  wb_fifo_inst_lo_crit.io.peek.data_in := 0.U

  wb_fifo_inst_new.io.q.enq.bits := state_machine.io.wb_wr_data_i
  wb_fifo_inst_new.io.q.enq.valid := state_machine.io.wb_wr_en_i
  wb_fifo_inst_new.io.peek.enable := false.B
  wb_fifo_inst_new.io.peek.remove := false.B
  wb_fifo_inst_new.io.peek.read_write := 1.U // default to read
  wb_fifo_inst_new.io.peek.address := 0.U
  wb_fifo_inst_new.io.peek.length := 0.U
  wb_fifo_inst_new.io.peek.data_in := 0.U


  when(event_encoder.io.can_valid && state_machine.io.cache_valid_line_addr.toBool()) {
    wb_fifo_inst_new.io.peek.read_write := 1.U
    wb_fifo_inst_lo_crit.io.peek.read_write := 1.U
    //wb_fifo_inst_new.io.peek.enable := true.B && !is_there_pending_req
    wb_fifo_inst_new.io.peek.address := event_encoder.io.address
    wb_fifo_inst_lo_crit.io.peek.address := event_encoder.io.address
  }
  when(state_machine.io.cc_state === CacheControllerState.WRITE_TO_CACHE ||
    state_machine.io.cc_state === CacheControllerState.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q ||
    state_machine.io.cc_state === CacheControllerState.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q_LO_CRIT) {
    // NOTE: for write into the wbq, if it doesn't match, the write is simple cancelled
    // does this write to the data cache?
    def wrtie_to_wbq(wb_fifo_inst_new: WriteBackQueue): Unit = {
      wb_fifo_inst_new.io.peek.read_write := 0.U
      wb_fifo_inst_new.io.peek.enable := true.B
      wb_fifo_inst_new.io.peek.address := cachereq_data_reg.address
      wb_fifo_inst_new.io.peek.length := cachereq_data_reg.length
      when(cachereq_data_reg.is_amo) {
        when(cachereq_data_reg.amo_alu_op === AMOOP.sc) {
          when(amo_alu_inst.io.out === 0.U) {
            wb_fifo_inst_new.io.peek.data_in := cachereq_data_reg.data
          }.elsewhen(amo_alu_inst.io.out === 1.U) {
            wb_fifo_inst_new.io.peek.enable := false.B
          }.otherwise {
            wb_fifo_inst_new.io.peek.enable := false.B
          }
        } /*.elsewhen(cachereq_data_reg.amo_alu_op === AMOOP.lr) {
        wb_fifo_inst_new.io.peek.read_write := 1.U
      }*/ .otherwise {
          wb_fifo_inst_new.io.peek.data_in := amo_alu_inst.io.out
        }
      }.otherwise {
        wb_fifo_inst_new.io.peek.data_in := cachereq_data_reg.data
      }
    }
    wrtie_to_wbq(wb_fifo_inst_new)
    wrtie_to_wbq(wb_fifo_inst_lo_crit)
  }
  wb_fifo_inst_new.io.dataq.enq <> state_machine.io.dataq_enq
  wb_fifo_inst_lo_crit.io.dataq.enq <> state_machine.io.dataq_enq_lo_crit

  // Arbitration here...
  val regWBDequeued = RegInit(0.U(2.W))
  when(wb_fifo_inst_new.io.q.deq.fire()) {
    regWBDequeued := 0.U
  }.elsewhen(wb_fifo_inst_lo_crit.io.q.deq.fire()) {
    regWBDequeued := 1.U
  }

  wb_fifo_inst_new.io.dataq.deq.ready := false.B
  wb_fifo_inst_lo_crit.io.dataq.deq.ready := false.B
  io.bus.dataq.valid := false.B
  // defaults to be the original fifo inst
  io.bus.dataq.bits := wb_fifo_inst_new.io.dataq.deq.bits
  when(regWBDequeued === 0.U) {
    // printf("[CC%d] Trying to use original q for data wb, valid: %b, ready: %b\n", io.id, io.bus.dataq.valid, io.bus.dataq.ready)
    // If the command header is from regular dequeue
    wb_fifo_inst_new.io.dataq.deq.ready := io.bus.dataq.ready
    io.bus.dataq.bits := wb_fifo_inst_new.io.dataq.deq.bits
    io.bus.dataq.valid := wb_fifo_inst_new.io.dataq.deq.valid
  }.elsewhen(regWBDequeued === 1.U) {
    // If the command header is from low criticality dequeue
    // printf("[CC%d] Trying to use low crit q for data wb, valid: %b, ready: %b\n", io.id, io.bus.dataq.valid, io.bus.dataq.ready)
    wb_fifo_inst_lo_crit.io.dataq.deq.ready := io.bus.dataq.ready
    io.bus.dataq.bits := wb_fifo_inst_lo_crit.io.dataq.deq.bits
    io.bus.dataq.valid := wb_fifo_inst_lo_crit.io.dataq.deq.valid
  }


  //////////////////////////////////////////////////////////////////
  // data array
  //////////////////////////////////////////////////////////////////
  data_array_cache_inst.io.clock := clock
  data_array_cache_inst.io.reset := reset

  data_array_write_byte_en_helper := _constructStrobe(cachereq_data_reg.address, cachereq_data_reg.length).asTypeOf(
    data_array_write_byte_en_helper
  )

  for {i <- 0 until (cacheParams.lineWidth / dataWidth)} {
    // to fix incorrect amo response
    when(state_machine.io.cc_state === CacheControllerState.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q ||
      state_machine.io.cc_state === CacheControllerState.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q_LO_CRIT
    ) {
      // AMO will always return the old data except for the store
      read_word(i) := memresp_data_reg.data((i + 1) * dataWidth - 1, i * dataWidth)
    }.otherwise {
      read_word(i) := data_array_read_data_reg((i + 1) * dataWidth - 1, i * dataWidth)
    }
  }

  cache_resp_data := 0.U
  cache_resp_data := _constructResponse(cachereq_data_reg.address, read_word(read_word_mux_sel))

  // printf(p"[CC ${io.id}] addr ${Hexadecimal(cachereq_data_reg.address)}, read_word ${Hexadecimal(read_word(read_word_mux_sel))}, cache-resp_data ${Hexadecimal(cache_resp_data)}\n")
  // printf(p"[CC ${io.id}] line: ${Hexadecimal(data_array_read_data_reg)}\n")
  protected def getLineAddress(addr: UInt): UInt = {
    addr(cacheParams.lineOffsetWidth + cacheParams.lineAddrWidth - 1, cacheParams.lineOffsetWidth)
  }
  protected def getTag(addr: UInt): UInt = {
    addr(cacheParams.addrWidth - 1, cacheParams.lineOffsetWidth)
  }

  data_array_cache_inst.io.write_en := false.B
  data_array_cache_inst.io.write_addr := 0.U(cacheParams.lineAddrWidth)
  data_array_cache_inst.io.write_byte_en := 0.U(cacheParams.lineBytes)
  data_array_cache_inst.io.write_data := 0.U(cacheParams.lineWidth)
  when(state_machine.io.cc_state === CacheControllerState.WRITE_TO_CACHE ||
    state_machine.io.cc_state === CacheControllerState.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q ||
    state_machine.io.cc_state === CacheControllerState.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q_LO_CRIT
  ) {
    data_array_cache_inst.io.write_en := true.B
    data_array_cache_inst.io.write_addr := getLineAddress(cachereq_data_reg.address)
    when(cachereq_data_reg.is_amo) { //AMO
      when(cachereq_data_reg.amo_alu_op === AMOOP.sc) {
        when(amo_alu_inst.io.out === 0.U) {
          data_array_cache_inst.io.write_data :=
            Cat(Seq.fill(cacheParams.lineWidth / dataWidth)(
              _constructData(cachereq_data_reg.address, cachereq_data_reg.data)
            ))
        }.elsewhen(amo_alu_inst.io.out=== 1.U) {
          data_array_cache_inst.io.write_en := false.B
        }.otherwise {
          data_array_cache_inst.io.write_en := false.B
        }
      }/*.elsewhen(cachereq_data_reg.amo_alu_op === AMOOP.lr) {
        data_array_cache_inst.io.write_en := false.B
      } */.otherwise {
        data_array_cache_inst.io.write_data := Cat(Seq.fill(cacheParams.lineWidth / dataWidth)(
          _constructData(cachereq_data_reg.address, amo_alu_inst.io.out)
        ))
        // printf(p"[CC ${io.id}] amo_res?: ${Hexadecimal(amo_alu_inst.io.out)}\n")
        // printf(p"[CC ${io.id}] amo_op1?: ${Hexadecimal(amo_alu_inst.io.in1)}\n")
        // printf(p"[CC ${io.id}] amo_op2?: ${Hexadecimal(amo_alu_inst.io.in2)}\n")
        // printf(p"[CC ${io.id}] cache_resp_data ?: ${Hexadecimal(cache_resp_data)}\n")
      }
    }.otherwise {
      data_array_cache_inst.io.write_data :=
        Cat(Seq.fill(cacheParams.lineWidth / dataWidth)(data_array_write_data_word)) // no need to construct, hard-coded
    }
    data_array_cache_inst.io.write_byte_en := data_array_write_byte_en_helper.asUInt()
    // printf(p"[CC ${io.id}] req addr: ${Hexadecimal(cachereq_data_reg.address)}\n")
    // printf(p"[CC ${io.id}] write_byte_en: ${Binary(data_array_write_byte_en_helper.asUInt)}\n")
    // printf(p"[CC ${io.id}] write_data: ${Hexadecimal(data_array_cache_inst.io.write_data)}\n")
  }

  when(state_machine.io.cc_state === CacheControllerState.REFILL_CACHE) {
    data_array_cache_inst.io.write_en := true.B
    data_array_cache_inst.io.write_addr := getLineAddress(cachereq_data_reg.address)
    data_array_cache_inst.io.write_data := memresp_data_reg.data
    data_array_cache_inst.io.write_byte_en := Cat(Seq.fill(cacheParams.lineBytes)(1.U(1.W)))
  }


  data_array_cache_inst.io.read_en := false.B
  data_array_cache_inst.io.read_addr := 0.U
  when(event_encoder.io.event_valid) {
    switch(event_encoder.io.event_src) {
      is(EventSource.CACHE.U) {
        data_array_cache_inst.io.read_en := cache_line_valid
        data_array_cache_inst.io.read_addr := getLineAddress(io.core.request_channel.bits.address)
      }
      is(EventSource.MEMORY.U) {
        data_array_cache_inst.io.read_en := true.B
        data_array_cache_inst.io.read_addr := getLineAddress(cachereq_data_reg.address)
      }
      is(EventSource.SNOOP.U) {
        data_array_cache_inst.io.read_en := true.B
        data_array_cache_inst.io.read_addr := getLineAddress(io.snoop.request_channel.bits.address)
      }
    }
  }
  when(data_array_cache_inst.io.write_en) {
    data_array.write(data_array_cache_inst.io.write_addr,
      data_array_cache_inst.io.write_data.asTypeOf(Vec(cacheParams.lineWidth / 8, UInt(8.W))),
      data_array_cache_inst.io.write_byte_en.asTypeOf(Vec(cacheParams.lineWidth / 8, Bool())))
  }

  when(state_machine.io.cc_state === CacheControllerState.CACHE_FLUSH_UPDATE ||
    state_machine.io.cc_state === CacheControllerState.READ_DATA_ARRAY ||
    state_machine.io.cc_state === CacheControllerState.READ_DATA_ARRAY_LO_CRIT) {
    data_array_cache_inst.io.read_en := true.B
    data_array_cache_inst.io.read_addr := line_addr_reg
  }

  //////////////////////////////////////////////////////////////////
  // signal drivers for input ports
  //////////////////////////////////////////////////////////////////

  io.core.request_channel.ready := false.B
  io.snoop.request_channel.ready := false.B
  io.bus.response_channel.ready := false.B

  lazy val waiting_wb = VecInit(coherence_table.getCoherenceWBStates.map(wb_state =>
        state_machine.io.cache_state_cachereq_data_i_address === wb_state.U
    )).asUInt()


  when(state_machine.io.cc_state === CacheControllerState.IDLE) {
    io.bus.response_channel.ready := true.B
    io.snoop.request_channel.ready := !io.bus.response_channel.fire()
    io.core.request_channel.ready := !(waiting_wb.orR() || // NOTE: the waiting_wb here indicates that we do not allow
      // hit on lines pending write back
      io.snoop.request_channel.fire() ||
      is_there_pending_req) &&
      Mux(io.core.request_channel.valid &&
        io.core.request_channel.bits.is_amo &&
        io.core.request_channel.bits.amo_alu_op === AMOOP.lr,
        canLR, true.B)
  }

  /*
  when(!io.core.request_channel.ready && io.core.request_channel.valid) {
    printf(p"[CC${io.id} - FAULT] stopping request: ${io.core.request_channel.bits}: line_state:")
    CoherenceState.printState(state_machine.io.cache_state_cachereq_data_i_address)
    printf(p" m: $cache_line_tag_match ")
    printf(p"wb_waiting? ${waiting_wb.orR()} snoop? ${io.snoop.request_channel.fire()}, pending? $is_there_pending_req")
    printf(p" canLR? ${canLR}")
    printf(p"\n")
  } */

  when( /*state_machine.io.cc_state === CacheControllerState.ADD_TO_CACHE_RESP_QUEUE_STORE &&*/
    io.core.request_channel.fire() &&
    io.core.request_channel.bits.is_amo &&
    io.core.request_channel.bits.amo_alu_op === AMOOP.lr) {
    lrsc_valid := true.B
    lrsc_address := io.core.request_channel.bits.address
    lrsc_counter := 480.U
    // printf(p"[CC${io.id}] LR reserved\n")
    
  }.elsewhen(event_encoder.io.event_valid &&
    (event_encoder.io.cache_event === CacheEvent.OTHER_GETM.U ||
      event_encoder.io.cache_event === CacheEvent.OTHER_GETS.U) &&
    getTag(lrsc_address) === getTag(event_encoder.io.address)) {
    // previous version
    // When there is snoop, we do not invalidate the reservation!
    // lrsc_valid := false.B
    // When there is snoop, we do invalidate the reservation!
    // but we have a whole window to finish our lr/sc sequence
    lrsc_valid := false.B

  }.elsewhen(
    data_array_cache_inst.io.write_en && // when finishing up the current request
    cachereq_data_reg.is_amo &&
    cachereq_data_reg.amo_alu_op === AMOOP.sc) {
    // invalidate the reservation for sc
    lrsc_valid := false.B
    when(lrsc_valid === 1.U) {
      printf(p"[CC ${io.id}] Reservation invalidated due to SC.\n")
    }
  }.elsewhen(!cachereq_data_reg.is_amo.toBool()) {
    lrsc_valid := false.B
    when(lrsc_valid === 1.U) {
      printf(p"[CC ${io.id}] Reservation invalidated due to other insn.\n")
    }
  }.elsewhen(lrsc_counter <= 10.U) {
    // lrsc_valid := false.B
    // when(lrsc_valid === 1.U) {
    //   printf(p"[CC ${io.id}] Reservation invalidated due to timeout.\n")
    // }
  }
  // hold the reservation for a certain amount of time
  when(lrsc_valid === 1.U) {
    lrsc_counter := lrsc_counter - 1.U
  }

  req_fifo.io.clk := clock
  req_fifo.io.rst := reset
  req_fifo.io.wr_data_i := state_machine.io.req_wr_data_i.asUInt
  req_fifo.io.wr_en_i := state_machine.io.req_wr_en_i
  when(req_fifo.io.wr_en_i === 1.U) {
    // printf(p"[REQFIFO] WRITE: ${state_machine.io.req_wr_data_i}\n")
  }
  when(!req_fifo.io.empty_o) {
    // printf(p"[REQFIFO] READ: some request... \n")
  }
  when(wb_fifo_inst_new.io.q.enq.fire() === 1.U) {
    // printf(p"[CC${io.id}] [WBFIFO] WRITE: ${wb_fifo_inst_new.io.q.enq.bits}\n")
  }
  when(wb_fifo_inst_lo_crit.io.q.enq.fire() === 1.U) {
    // printf(p"[CC${io.id}] [WBLOCRITFIFO] WRITE: ${wb_fifo_inst_lo_crit.io.q.enq.bits}\n")
  }
  when(io.bus.dataq.fire()) {
    printf(p"[CC${io.id}] PUSH DATA: ${Hexadecimal(io.bus.dataq.bits)}\n")
  }

  state_machine.io.req_full_o := req_fifo.io.full_o

  cache_resp_fifo_inst.io.clk := clock
  cache_resp_fifo_inst.io.rst := reset
  cache_resp_fifo_inst.io.wr_data_i := state_machine.io.cache_resp_wr_data_i.asUInt()
  cache_resp_fifo_inst.io.wr_en_i := state_machine.io.cache_resp_wr_en_i

  snoop_resp_fifo_inst.io.clk := clock
  snoop_resp_fifo_inst.io.rst := reset
  snoop_resp_fifo_inst.io.wr_data_i := state_machine.io.snoop_resp_wr_data_i.asUInt()
  snoop_resp_fifo_inst.io.wr_en_i := state_machine.io.snoop_resp_wr_en_i

  //val req_i = Cat(!wb_fifo_inst.io.empty_o, !req_fifo.io.empty_o)
  val hasWB = wb_fifo_inst_new.io.q.deq.valid | wb_fifo_inst_lo_crit.io.q.deq.valid
  val req_i = Cat(hasWB, !req_fifo.io.empty_o)
  val ack = io.bus.request_channel.ready
  rr_arbiter_inst.io.clock := clock
  rr_arbiter_inst.io.reset := reset
  rr_arbiter_inst.io.req := req_i
  rr_arbiter_inst.io.ack := ack
  val grant_o = rr_arbiter_inst.io.grant

  // buffer to output ports interface
  io.core.response_channel.valid := ~cache_resp_fifo_inst.io.empty_o
  req_fifo.io.rd_en_i := 0.U
  wb_fifo_inst_new.io.q.deq.ready := 0.U
  wb_fifo_inst_lo_crit.io.q.deq.ready := 0.U
  io.bus.request_channel.valid := 0.U
  io.bus.request_channel.bits := 0.U.asTypeOf(genControllerReqCommand)
  cache_resp_fifo_inst.io.rd_en_i := 0.U
  io.core.response_channel.bits := 0.U.asTypeOf(genCacheResp)
  snoop_resp_fifo_inst.io.rd_en_i := 0.U
  io.snoop.response_channel.valid := false.B
  io.snoop.response_channel.bits := 0.U.asTypeOf(genSnoopResp)

  // printf(p"[CC${io.id}] grant_o: ${grant_o.asUInt} wbfifo has req? ${wb_fifo_inst_new.io.q.deq.valid} reqfifo has req? ${!req_fifo.io.empty_o} lrRequestor: ${lrRequestor * 2.U + 1.U} lrsc_valid: ${lrsc_valid} " +
  // p"${Hexadecimal(lrsc_address)} writeback addr: ${Hexadecimal(wb_fifo_inst_new.io.q.deq.bits.address)} \n")

  // defer write back until the tag is invalid
  io.bus.request_channel.valid := grant_o.orR() && (
    !lrsc_valid ||
      (grant_o === "b10".U(2.W) && hasWB  && // not writing back the reserved address
      !(
        (wb_fifo_inst_new.io.q.deq.valid && getTag(lrsc_address) === getTag(wb_fifo_inst_new.io.q.deq.bits.address)) ||
        (!wb_fifo_inst_new.io.q.deq.valid && wb_fifo_inst_lo_crit.io.q.deq.valid && getTag(lrsc_address) === getTag(wb_fifo_inst_lo_crit.io.q.deq.bits.address))
      )) || grant_o === "b01".U(2.W)
    )
  when(io.bus.request_channel.fire()) {
    switch(grant_o) {
      is("b01".U(2.W)) {
        req_fifo.io.rd_en_i := 1.U
        io.bus.request_channel.bits := req_fifo.io.rd_data_o.asTypeOf(genControllerReqCommand)

      }
      is("b10".U(2.W)) {
        //wb_fifo_inst.io.rd_en_i := 1.U
        //io.bus.request_channel.bits := wb_fifo_inst.io.rd_data_o.asTypeOf(genControllerReq)
        when(wb_fifo_inst_new.io.q.deq.valid) {
          // hi crit fifo gets priority
          wb_fifo_inst_new.io.q.deq.ready := 1.U
          io.bus.request_channel.bits := wb_fifo_inst_new.io.q.deq.bits
        }.otherwise {
          // otherwise, the lo crit is granted
          wb_fifo_inst_lo_crit.io.q.deq.ready := 1.U
          io.bus.request_channel.bits := wb_fifo_inst_lo_crit.io.q.deq.bits
        }

      }

    }
  }

  // Trying to removing whatever inside the lo crit wb buffer
  when(wb_fifo_inst_new.io.q.enq.fire()) {
    wb_fifo_inst_lo_crit.io.peek.enable := true.B
    wb_fifo_inst_lo_crit.io.peek.remove := true.B
    wb_fifo_inst_lo_crit.io.peek.read_write := 0.U
    wb_fifo_inst_lo_crit.io.peek.address := wb_fifo_inst_new.io.q.enq.bits.address
  }
  // Also remove for OtherGetS, OtherGetM
  /*
  val snoop_event_reg = RegInit(0.U(CacheEvent.getWidth.W))
  val is_snoop_update = RegInit(false.B)
  when(io.snoop.request_channel.fire()) {
    snoop_event_reg := event_encoder.io.cache_event
    is_snoop_update := true.B
  }
  when(state_machine.io.cc_state === CacheControllerState.UPDATE_CACHE) {
    when(snoop_event_reg === CacheEvent.OTHER_GETS || snoop_event_reg === CacheEvent.OTHER_GETM) {
      when(memorySystemParams.i)
      wb_fifo_inst_lo_crit.io.peek.enable := true.B
      wb_fifo_inst_lo_crit.io.peek.remove := true.B
      wb_fifo_inst_lo_crit.io.peek.read_write := 0.U
      wb_fifo_inst_lo_crit.io.peek.address := snoop_req_data_reg.address
    }
    is_snoop_update := false.B
  }
   */

  when(io.core.response_channel.fire()) {
    cache_resp_fifo_inst.io.rd_en_i := 1.U
    io.core.response_channel.bits := cache_resp_fifo_inst.io.rd_data_o.asTypeOf(genCacheResp)
  }

  io.snoop.response_channel.valid := ~snoop_resp_fifo_inst.io.empty_o
  when(io.snoop.response_channel.fire()) {
    snoop_resp_fifo_inst.io.rd_en_i := 1.U
    io.snoop.response_channel.bits := snoop_resp_fifo_inst.io.rd_data_o.asTypeOf(genSnoopResp)
  }

  amo_alu_inst.io.amo_alu_op := cachereq_data_reg.amo_alu_op
  amo_alu_inst.io.isW := cachereq_data_reg.length === 2.U
  amo_alu_inst.io.in1 := cache_resp_data
  amo_alu_inst.io.in2 := cachereq_data_reg.data
  amo_alu_inst.io.lrsc_valid := lrsc_valid
  amo_alu_inst.io.lrsc_address := lrsc_address
  amo_alu_inst.io.sc_address := cachereq_data_reg.address

  //when(io.id === 1.U || io.id === 0.U) {
  /*
    when(io.bus.request_channel.fire()) {
      printf(p"[CC${io.id}] ${io.bus.request_channel} data: INVISIBLE\n")
    }
    when(io.bus.response_channel.fire()) {
      printf(p"[CC${io.id}] ${io.bus.response_channel}\n")
    }
    when(io.core.request_channel.fire()) {
      printf(p"[CC${io.id}] ${io.core.request_channel}\n")
    }
    when(io.core.response_channel.fire()) {
      printf(p"[CC${io.id}] ${io.core.response_channel}\n")
    }
   */



  /*
    when(event_encoder.io.event_valid) {
      printf(p"[CC${io.id}] (")
      CacheEvent.printState(event_encoder.io.cache_event)
      printf(", ")
      CoherenceState.printState(cache_line_state)
      printf(p", Match: $cache_line_tag_match)\n")
    }

   */

    // printf(p"[CC${io.id}] pending req?: $is_there_pending_req\n")

  //}


}

