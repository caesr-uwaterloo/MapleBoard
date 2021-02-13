
package components

import chisel3._
import chisel3.util._
import coherence.internal.AutoEnum
import types._
import params.{MemorySystemParams, SimpleCacheParams}

import language.dynamics
import scala.collection.mutable
import coherences.PMESI
import components.{CacheControllerState => CCS}
// import dbgutil.exposeTop

import _root_.core.AMOOP


object AMOALUOP extends Dynamic {
  private val aluop = mutable.HashMap[String, UInt] (
  "AMO_LR"    -> "b00010".U(getWidth.W),
  "AMO_SC"    -> "b00011".U(getWidth.W),
  "AMO_SWAP"  -> "b00001".U(getWidth.W),
  "AMO_ADD"   -> "b00000".U(getWidth.W),
  "AMO_XOR"   -> "b00100".U(getWidth.W),
  "AMO_AND"   -> "b01100".U(getWidth.W),
  "AMO_OR"    -> "b01000".U(getWidth.W),
  "AMO_MIN"   -> "b10000".U(getWidth.W),
  "AMO_MAX"   -> "b10100".U(getWidth.W),
  "AMO_MINU"  -> "b11000".U(getWidth.W),
  "AMO_MAXU"  -> "b11100".U(getWidth.W)
  )

  def selectDynamic(name: String): UInt = aluop(name)
  def getWidth: Int = 5

}

class CacheControllerStateMachine (private val masterCount: Int,
                                   private val dataWidth: Int,
                                   private val cacheParams: SimpleCacheParams,
                                   private val genCacheReq: CacheReq,
                                   private val genMemReqCommand: MemReqCommand,
                                   private val genSnoopResp: SnoopResp,
                                   private val genCacheResp: CacheResp,
                                   private val memorySystemParams: MemorySystemParams,
                                   private val CoherenceState: AutoEnum) extends Module {
  val io = IO(new Bundle {
    // Input
    val id = Input(UInt((log2Ceil(masterCount) + 1).W))
    val event_valid = Input(Bool())
    val cache_ctr_next_state = Input(UInt(CacheControllerState.getWidth.W))
    val cache_line_next_dirty = Input(Bool())
    val cache_line_next_valid = Input(Bool())
    val cache_line_broadcast_type = Input(UInt(RequestType.getWidth.W))
    val wb_full_o = Input(Bool())
    val wb_lo_crit_full_o = Input(Bool())
    val tag_array_read_data_reg = Input(UInt(cacheParams.tagWidth.W))
    val data_array_read_data_reg = Input(UInt(cacheParams.lineWidth.W))
    val cachereq_data_reg = Input(genCacheReq)
    val cachereq_data_i = Input(genCacheReq)
    val req_full_o = Input(Bool())
    val cache_resp_full_o = Input(Bool())
    val cache_resp_data = Input(UInt(dataWidth.W))
    val amo_result = Input(UInt(dataWidth.W))
    val snoop_resp_full_o = Input(Bool())
    val line_addr_reg = Input(UInt(cacheParams.lineAddrWidth.W))
    val line_addr = Input(UInt(cacheParams.lineAddrWidth.W))
    val cache_line_next_state = Input(UInt(CoherenceState.getWidth.W))
    // val data_array_debug_read_addr_i = Input(UInt(cacheParams.lineAddrWidth.W))

    // Output
    val wb_wr_en_i = Output(Bool())
    val wb_wr_en_lo_crit_i = Output(Bool())
    val req_wr_en_i = Output(Bool())
    val snoop_resp_wr_en_i = Output(Bool())
    val cache_resp_wr_en_i = Output(Bool())

    val req_wr_data_i = Output(genMemReqCommand)
    val wb_wr_data_i = Output(memorySystemParams.getGenMemReqCommand)
    val wb_wr_data_lo_crit_i = Output(memorySystemParams.getGenMemReqCommand)
    val snoop_resp_wr_data_i = Output(genSnoopResp)
    val cache_resp_wr_data_i = Output(genCacheResp)

    val data_array_write_en_reg = Output(Bool())

    val cc_state = Output(UInt(CacheControllerState.getWidth.W))
    // val cache_ctr_next_state_reg = Output(UInt(CacheControllerState.getWidth.W))
    val cache_line_next_dirty_reg = Output(Bool())
    val cache_line_next_valid_reg = Output(Bool())

    val cache_line_broadcast_type_reg = Output(UInt(RequestType.getWidth.W))
    val cache_state_line_addr = Output(UInt(CoherenceState.getWidth.W))
    val cache_state_cachereq_data_i_address = Output(UInt(CoherenceState.getWidth.W))
    val cache_valid_cachereq_data_i_address = Output(UInt(1.W))
    val cache_valid_line_addr = Output(UInt(1.W))
    val cache_dirty_line_addr = Output(UInt(1.W))

    // val cache_state_data_array_debug_read_addr_i = Output(UInt(CoherenceState.getWidth.W))
    // val cache_valid_data_array_debug_read_addr_i = Output(UInt(1.W))
    // val cache_dirty_data_array_debug_read_addr_i = Output(UInt(1.W))

    val dataq_enq = Decoupled(UInt(memorySystemParams.busDataWidth.W))
    val dataq_enq_lo_crit = Decoupled(UInt(memorySystemParams.busDataWidth.W))
  })

  val beats_per_line: Int = memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth

  val wb_wr_en_i = RegInit(false.B)
  val wb_wr_en_lo_crit_i = RegInit(false.B)
  val req_wr_en_i = RegInit(false.B)
  val snoop_resp_wr_en_i = RegInit(false.B)
  val cache_resp_wr_en_i = RegInit(false.B)
  val req_wr_data_i = Reg(memorySystemParams.getGenMemReqCommand)
  val wb_wr_data_i = Reg(memorySystemParams.getGenMemReqCommand)
  val wb_wr_data_lo_crit_i = Reg(memorySystemParams.getGenMemReqCommand)
  val snoop_resp_wr_data_i = Reg(genSnoopResp)
  val cache_resp_wr_data_i = Reg(genCacheResp)

  val data_array_write_en_reg = RegInit(false.B)

  val cc_state = RegInit(CacheControllerState.IDLE.litValue().U(CacheControllerState.getWidth.W))
  // exposeTop(cc_state)
  val cache_ctr_next_state_reg = RegInit(CacheControllerState.IDLE.litValue().U(CacheControllerState.getWidth.W))
  val cache_line_next_dirty_reg = RegInit(false.B)
  val cache_line_next_valid_reg = RegInit(false.B)
  val cache_line_next_state_reg = RegInit(0.U(CoherenceState.getWidth.W))

  val cache_valid = RegInit(VecInit(Seq.fill(cacheParams.nSets)(false.B)))
  val cache_dirty = RegInit(VecInit(Seq.fill(cacheParams.nSets)(false.B)))
  val cache_state = RegInit(VecInit(Seq.fill(cacheParams.nSets)(0.U(CoherenceState.getWidth.W))))

  val cache_line_broadcast_type_reg = Reg(UInt(RequestType.getWidth.W))

  val wb_queue_data_counter = RegInit(0.U((log2Ceil(beats_per_line) + 1).W))
  val wb_queue_data_valid = RegInit(0.U(1.W))
  val wb_queue_data = WireInit(0.U(memorySystemParams.busDataWidth.W))
  val wb_queue_push_data_done = (io.dataq_enq.fire() && wb_queue_data_counter === (beats_per_line - 1).U) ||
    cache_line_broadcast_type_reg =/= RequestType.PUTM.U

  val wb_queue_data_lo_crit_counter = RegInit(0.U((log2Ceil(beats_per_line) + 1).W))
  val wb_queue_data_lo_crit_valid = RegInit(0.U(1.W))
  val wb_queue_data_lo_crit = WireInit(0.U(memorySystemParams.busDataWidth.W))
  val wb_queue_push_data_lo_crit_done = (io.dataq_enq_lo_crit.fire() && wb_queue_data_lo_crit_counter === (beats_per_line - 1).U) ||
    cache_line_broadcast_type_reg =/= RequestType.PUTM.U

  // inferring to mux
  val data_array_read_data_word_wire = Wire(Vec(beats_per_line, UInt(memorySystemParams.busDataWidth.W)))
  data_array_read_data_word_wire := io.data_array_read_data_reg.asTypeOf(Vec(beats_per_line, UInt(memorySystemParams.busDataWidth.W)))
  wb_queue_data := data_array_read_data_word_wire(wb_queue_data_counter)
  wb_queue_data_lo_crit := data_array_read_data_word_wire(wb_queue_data_lo_crit_counter)
  when(io.dataq_enq.fire()) {
    // printf(p"[CC${io.id}] pushing to llcc original data: ${Hexadecimal(io.data_array_read_data_reg)}\n")
  }

  io.cache_state_line_addr := cache_state(io.line_addr)
  io.cache_state_cachereq_data_i_address := cache_state(io.cachereq_data_i.address(
    cacheParams.lineOffsetWidth + cacheParams.lineAddrWidth - 1, cacheParams.lineOffsetWidth))
  io.cache_valid_cachereq_data_i_address := cache_valid(cacheParams.getLineAddress(io.cachereq_data_i.address))
  io.cache_valid_line_addr := cache_valid(io.line_addr)
  io.cache_dirty_line_addr := cache_dirty(io.line_addr)

  // Connection
  io.wb_wr_en_i := wb_wr_en_i
  io.wb_wr_en_lo_crit_i := wb_wr_en_lo_crit_i
  io.req_wr_en_i := req_wr_en_i
  io.snoop_resp_wr_en_i := snoop_resp_wr_en_i
  io.cache_resp_wr_en_i := cache_resp_wr_en_i
  io.req_wr_data_i := req_wr_data_i
  io.snoop_resp_wr_data_i := snoop_resp_wr_data_i
  io.cache_resp_wr_data_i := cache_resp_wr_data_i
  io.data_array_write_en_reg := data_array_write_en_reg
  io.cc_state  := cc_state
  // io.cache_ctr_next_state_reg := cache_ctr_next_state_reg
  io.cache_line_next_dirty_reg := cache_line_next_dirty_reg
  io.cache_line_next_valid_reg := cache_line_next_valid_reg
  io.cache_line_broadcast_type_reg := cache_line_broadcast_type_reg
  io.wb_wr_data_i := wb_wr_data_i
  io.wb_wr_data_lo_crit_i := wb_wr_data_lo_crit_i
  // io.cache_state_data_array_debug_read_addr_i := cache_state(io.data_array_debug_read_addr_i)
  // io.cache_valid_data_array_debug_read_addr_i := cache_valid(io.data_array_debug_read_addr_i)
  // io.cache_dirty_data_array_debug_read_addr_i := cache_dirty(io.data_array_debug_read_addr_i)
  io.dataq_enq.valid := wb_queue_data_valid
  io.dataq_enq.bits := wb_queue_data
  io.dataq_enq_lo_crit.valid := wb_queue_data_lo_crit_valid
  io.dataq_enq_lo_crit.bits := wb_queue_data

  when(io.dataq_enq.fire()) {
    // printf(p"[CC ${io.id}] writeback to WBQ data interface ${Hexadecimal(io.dataq_enq.bits)}\n")
  }
  when(io.dataq_enq_lo_crit.fire()) {
    // printf(p"[CC ${io.id}] writeback to WBQ (LO CRIT) data interface ${Hexadecimal(io.dataq_enq.bits)}\n")
  }

  // default cases
  wb_wr_en_i := false.B
  wb_wr_en_lo_crit_i := false.B
  req_wr_en_i := false.B
  snoop_resp_wr_en_i := false.B
  cache_resp_wr_en_i := false.B

  req_wr_data_i := 0.U.asTypeOf(genMemReqCommand)
  snoop_resp_wr_data_i := 0.U.asTypeOf(genSnoopResp)
  cache_resp_wr_data_i := 0.U.asTypeOf(genCacheResp)

  data_array_write_en_reg := false.B
  wb_queue_data_valid := 0.U
  wb_queue_data_lo_crit_valid := 0.U

  def updateCacheStateFromReg(): Unit = {
    cache_state(io.line_addr_reg) := cache_line_next_state_reg
    cache_dirty(io.line_addr_reg) := cache_line_next_dirty_reg
    cache_valid(io.line_addr_reg) := cache_line_next_valid_reg

    //when(io.id === 0.U || io.id === 1.U) {
      printf(p"[CC${io.id}] State[${io.line_addr_reg}] ")
      for{i <- 0 until CoherenceState.getStateList.length} {
        when(i.U === cache_state(io.line_addr_reg)) {
          val st = CoherenceState.getStateList(i)
          printf(p"$st -> ")
        }
      }
      for{i <- 0 until CoherenceState.getStateList.length} {
        when(i.U === cache_line_next_state_reg) {
          val st = CoherenceState.getStateList(i)
          printf(p" -> $st")
        }
      }
      printf("\n")
    /*
    printf(p"[CC${io.id}] next_valid: ${cache_line_next_valid_reg}\n")
    printf(p"[CC${io.id}] next_dirty: ${cache_line_next_dirty_reg}\n")
     */
    //}

  }

  def updateNextReg(): Unit = {
    cache_line_next_state_reg := io.cache_line_next_state
    cache_line_next_dirty_reg := io.cache_line_next_dirty
    cache_line_next_valid_reg := io.cache_line_next_valid
    cache_line_broadcast_type_reg := io.cache_line_broadcast_type
  }

  def addToWBQueue(): Unit = {
    wb_wr_en_i := true.B
    wb_wr_data_i.address := Cat(io.tag_array_read_data_reg, 0.U(cacheParams.lineOffsetWidth.W))
    //wb_wr_data_i.data := io.data_array_read_data_reg
    wb_wr_data_i.req_type := cache_line_broadcast_type_reg
    wb_wr_data_i.requester_id := io.id
    wb_wr_data_i.req_wb := true.B
    wb_wr_data_i.dirty := cache_dirty(io.line_addr_reg)
    wb_wr_data_i.criticality := memorySystemParams.coreIdToCriticality(io.id)
    printf(p"[CC${io.id}] addToWBQueue() address: ${Hexadecimal(Cat(io.tag_array_read_data_reg, 0.U(cacheParams.lineOffsetWidth.W)))}\n")

  }
  def addToWBLoCritQueue(): Unit = {
    wb_wr_en_lo_crit_i := true.B
    wb_wr_data_lo_crit_i.address := Cat(io.tag_array_read_data_reg, 0.U(cacheParams.lineOffsetWidth.W))
    //wb_wr_data_lo_crit_i.data := io.data_array_read_data_reg
    wb_wr_data_lo_crit_i.req_type := cache_line_broadcast_type_reg
    wb_wr_data_lo_crit_i.requester_id := io.id
    wb_wr_data_lo_crit_i.req_wb := true.B
    wb_wr_data_lo_crit_i.dirty := cache_dirty(io.line_addr_reg)
    wb_wr_data_lo_crit_i.criticality := memorySystemParams.coreIdToCriticality(io.id)
    printf(p"[CC${io.id}] addToWBLoCritQueue() address: ${Hexadecimal(Cat(io.tag_array_read_data_reg, 0.U(cacheParams.lineOffsetWidth.W)))}\n")

  }

  def pushDataToWBQueue(): Unit = {
    wb_queue_data_valid := 1.U
    when(io.dataq_enq.fire()) {
      wb_queue_data_counter := wb_queue_data_counter + 1.U
    }
    when(wb_queue_push_data_done) {
      wb_queue_data_valid := 0.U
    }
  }
  def pushDataToWBLoCritQueue(): Unit = {
    wb_queue_data_lo_crit_valid := 1.U
    when(io.dataq_enq_lo_crit.fire()) {
      wb_queue_data_lo_crit_counter := wb_queue_data_lo_crit_counter + 1.U
    }
    when(wb_queue_push_data_lo_crit_done) {
      wb_queue_data_lo_crit_valid := 0.U
    }
  }
  when(cc_state === CCS.PUSH_DATA_TO_WB_QUEUE) {
    // machine
    // otherwise we dont push data
    when(cache_line_broadcast_type_reg === RequestType.PUTM.U) {
      pushDataToWBQueue()
    }
  }
  when(cc_state === CCS.PUSH_DATA_TO_LO_CRIT_WB_QUEUE) {
    // machine
    // otherwise we dont push data
    when(cache_line_broadcast_type_reg === RequestType.PUTM.U) {
      pushDataToWBLoCritQueue()
    }
  }

  def addToReqQueue(): Unit = {
    req_wr_en_i := true.B
    req_wr_data_i.address := Cat(io.cachereq_data_reg.address(cacheParams.addrWidth - 1,
      cacheParams.lineOffsetWidth), 0.U(cacheParams.lineOffsetWidth.W))
    // req_wr_data_i.data := 0.U(cacheParams.lineWidth.W)
    req_wr_data_i.req_type := cache_line_broadcast_type_reg
    req_wr_data_i.requester_id := io.id
    req_wr_data_i.req_wb := false.B
    req_wr_data_i.criticality := memorySystemParams.coreIdToCriticality(io.id)
  }

  /** register a response, code allows us to rewrite the written data */
  def addToRespQueue(code: => Unit = {}): Unit = {
    cache_resp_wr_en_i := true.B
    cache_resp_wr_data_i.data := io.cache_resp_data
    cache_resp_wr_data_i.address := io.cachereq_data_reg.address
    cache_resp_wr_data_i.length := io.cachereq_data_reg.length
    cache_resp_wr_data_i.mem_type := io.cachereq_data_reg.mem_type

    code
  }

  def addToSnoopRespQueue(): Unit = {
    snoop_resp_wr_en_i := true.B
    snoop_resp_wr_data_i.ack := true.B
  }

  lazy val amo_res = Mux(io.cachereq_data_reg.is_amo,
    Mux(io.cachereq_data_reg.amo_alu_op === AMOOP.sc, io.amo_result, io.cache_resp_data ),
    0.U(dataWidth.W))

  def on(cond: Bool, next: UInt)(action: => Unit): () => Unit = () => {
    when(cond) {
      cc_state := next
      action
    }
  }

  def goto(next: UInt)(action: => Unit): () => Unit = () => {
    cc_state := next
    action
  }

  def raiseError(): () => Unit = () => {
    printf(p"[CC${io.id}] Coherence Error Encountered --- SWMR invariant violated\n")
    assert(false.B)
    ()
  }

  def actionList: Map[UInt, () => Unit] = Map(
    CCS.IDLE                    -> on(io.event_valid, io.cache_ctr_next_state) { updateNextReg() },
    CCS.ADD_TO_WB_QUEUE         -> on(!io.wb_full_o , CCS.PUSH_DATA_TO_WB_QUEUE) {
      addToWBQueue()
      wb_queue_data_counter := 0.U
      when(cache_line_broadcast_type_reg === RequestType.PUTM.U) {
        wb_queue_data_valid := 1.U
      }
    },
    CCS.ADD_TO_LO_CRIT_WB_QUEUE         -> on(!io.wb_lo_crit_full_o , CCS.PUSH_DATA_TO_LO_CRIT_WB_QUEUE) {
      addToWBLoCritQueue()
      wb_queue_data_lo_crit_counter := 0.U
      when(cache_line_broadcast_type_reg === RequestType.PUTM.U) {
        wb_queue_data_lo_crit_valid := 1.U
      }
    },
    CCS.PUSH_DATA_TO_WB_QUEUE   -> on(wb_queue_push_data_done, CCS.UPDATE_CACHE) { },
    CCS.PUSH_DATA_TO_LO_CRIT_WB_QUEUE   -> on(wb_queue_push_data_lo_crit_done, CCS.UPDATE_CACHE) { },
    CCS.ADD_TO_REQ_QUEUE        -> on(!io.req_full_o, CCS.ADD_TO_TAG_ARRAY) { addToReqQueue() },
    CCS.ADD_TO_CACHE_RESP_QUEUE -> on(!io.cache_resp_full_o, CCS.UPDATE_CACHE) { addToRespQueue() },
    CCS.ADD_TO_CACHE_RESP_QUEUE_STORE -> on(!io.cache_resp_full_o, CCS.WRITE_TO_CACHE)(addToRespQueue { cache_resp_wr_data_i.data := amo_res }),
    CCS.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q -> on(!io.cache_resp_full_o, CCS.READ_DATA_ARRAY)({
      updateCacheStateFromReg()
      addToRespQueue {
        cache_resp_wr_data_i.data := amo_res
      }
    }
    ),
    CCS.WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q_LO_CRIT -> on(!io.cache_resp_full_o, CCS.READ_DATA_ARRAY_LO_CRIT)({
      updateCacheStateFromReg()
      addToRespQueue {
        cache_resp_wr_data_i.data := amo_res
      }
    }),
    CCS.READ_DATA_ARRAY_LO_CRIT -> goto(CCS.ADD_TO_LO_CRIT_WB_QUEUE) {},
    CCS.READ_DATA_ARRAY         -> goto(CCS.ADD_TO_WB_QUEUE) {},
    CCS.ADD_TO_CACHE_SNOOP_RESP_QUEUE -> on(!io.snoop_resp_full_o, CCS.UPDATE_CACHE) { addToSnoopRespQueue() },
    CCS.UPDATE_CACHE            -> goto(CCS.IDLE) { updateCacheStateFromReg() },
    CCS.ADD_TO_TAG_ARRAY        -> goto(CCS.IDLE) { updateCacheStateFromReg() },
    CCS.REFILL_CACHE            -> goto(CCS.IDLE_2) { updateCacheStateFromReg() },
    CCS.IDLE_2                  -> goto(io.cache_ctr_next_state) { updateNextReg() },
    CCS.WRITE_TO_CACHE          -> goto(CCS.IDLE) { updateCacheStateFromReg() },
    CCS.REMOVE_FROM_CACHE       -> goto(CCS.IDLE_2) { updateCacheStateFromReg() },
    // For CARP
    CCS.REISSUE                 -> goto(CCS.ADD_TO_REQ_QUEUE) {  },
    CCS.IMPOSSIBLE              -> raiseError()
  )
  val printIdle = RegInit(true.B)

  def embedActions: Unit = {
    for {(k, v) <- actionList} {
      val str = CacheControllerState.sList(k.litValue().toInt)
      when(cc_state === k) {
        when(cc_state === CCS.IDLE && printIdle) {
          printf(p"[CC${io.id}] $str\n")
          printIdle := false.B
        }.elsewhen(cc_state =/= CCS.IDLE) {
          printf(p"[CC${io.id}] $str\n")
          printIdle := true.B
        }
        v()
      }
    }
  }

  // non_reset
  embedActions
}
