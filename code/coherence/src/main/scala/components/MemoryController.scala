
package components

import chisel3._
import chisel3.util._
// import dbgutil.exposeTop
import coherence.internal.{AsAutoEnum, AutoEnum}
import coherences.PMESILLC
import params.{MemorySystemParams, SimpleCacheParams}

class TableEntry(val idWidth: Int) extends Bundle {
  val req_type = UInt(1.W)
  val requester_id = UInt(idWidth.W)
  val criticality = UInt(3.W)
}

class HashTableEntry(private val stateWidth: Int,
                     private val sharerWidth: Int) extends Bundle {
  // sharer is the bit mask of which core owns/ shares the line
  val state = UInt(stateWidth.W)
  val sharer = UInt(sharerWidth.W)

  override def cloneType: this.type = new HashTableEntry(stateWidth, sharerWidth).asInstanceOf[this.type]

  override def toPrintable: Printable = p"state = ${Binary(state)}, sharer = ${Binary(sharer)}"
}

@AsAutoEnum
trait LLCStatesBase extends AutoEnum {
  val LLC_IDLE, LLC_WAIT, LLC_HASH_READ, LLC_HASH_WRITE, LLC_HASH_REMOVE, LLC_PR_TABLE_READ, LLC_PR_TABLE_WRITE,
  LLC_MEMORY_READ, LLC_MEMORY_WRITE, LLC_RESP_QUEUE_PUSH, LLC_FAILURE, LLC_WAIT_DATA, LLC_PR_TABLE_READ_REQ,
  LLC_PR_TABLE_WRITE_REQ: Int
}
object LLCStatesBase extends LLCStates
trait LLCStates extends LLCStatesBase {
  override def getWidth: Int = 5
}
object LLCStates extends LLCStatesBase

class MemoryController(val idWidth: Int,
                       val memorySystemParams: MemorySystemParams,
                       val llcLineStates: PMESILLC,
                       val genErrorMessage: ErrorMessage) extends Module {
  val masterCount: Int = memorySystemParams.masterCount
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams
  val genMemReq: MemReq = memorySystemParams.getGenMemReq
  val genMemReqCommand: MemReqCommand = memorySystemParams.getGenMemReqCommand
  val genMemResp: MemResp = memorySystemParams.getGenMemResp
  val genMemRespCommand: MemRespCommand = memorySystemParams.getGenMemRespCommand
  val genDramReq: DRAMReq = memorySystemParams.getGenDramReq
  val genDramResp: DRAMResp = memorySystemParams.getGenDramResp
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val beats_per_line: Int = memorySystemParams.cacheParams.lineWidth / memorySystemParams.busDataWidth

  val io = IO(new Bundle {
    val bus = new Bundle {
      val request_channel = Flipped(Decoupled(genMemReqCommand))
      val response_channel = Vec(masterCount, Decoupled(genMemRespCommand))
      val data_channel = Flipped(Vec(masterCount, Decoupled(UInt(busDataWidth.W))))
      val response_data_channel = Vec(masterCount, Decoupled(UInt(busDataWidth.W)))
    }
    val dram = new Bundle {
      val request_channel = Decoupled(genDramReq)
      val response_channel = Flipped(Decoupled(genDramResp))
    }
    val err = Output(genErrorMessage)
  })


  val genHashTableEntry = new HashTableEntry(llcLineStates.getWidth, masterCount)
  val hash_table_write_request = Wire(genHashTableEntry)

  val hash_table = Module(new HashTable(masterCount * cacheParams.nSets * cacheParams.nWays * 2,
    UInt(cacheParams.tagWidth.W),
    UInt(genHashTableEntry.getWidth.W)))

  val coherenceTable = Module(memorySystemParams.llcCoherenceTable())

  val pending_shares = RegInit(0.U(masterCount.W))

  val memreq_data_reg = Reg(genMemReqCommand)
  val memory_response_reg = Reg(genDramResp)
  val resp_state_reg = Reg(new Bundle {
    val state = UInt(genHashTableEntry.getWidth.W)
    val ack = UInt(1.W)
    // val sharers = UInt(masterCount.W)
  })
  val llc_state = RegInit(LLCStates.LLC_IDLE.U(LLCStates.getWidth.W))
  // exposeTop(llc_state)
  // val pr_lut = Module(new PendingRequestLUT(cacheParams.tagWidth, 2 * masterCount, idWidth))
  val pr_lut_new = Module(new PRLUT(cacheParams.tagWidth, 2 * masterCount, idWidth, memorySystemParams))

  val priority_encoder = Module(new PriorityEncoder(masterCount, lsb_priority="HIGH"))

  val timeout = RegInit(0.U(32.W))
  val mem_resp_wr_data_i = Reg(Vec(masterCount, genMemResp))
  val mem_resp_wr_en_i = RegInit(VecInit.tabulate(masterCount)(_ => 0.U(1.W)))
  val mem_resp_rd_data_o = Wire(Vec(masterCount, genMemRespCommand))
  val mem_resp_rd_en_i  = Wire(Vec(masterCount, UInt(1.W)))
  val mem_resp_full_o = Wire(Vec(masterCount, UInt(1.W)))
  val mem_resp_empty_o  = Wire(Vec(masterCount, UInt(1.W)))

  val memresp_owner = Cat(for{ i <- (0 until masterCount).reverse } yield {
    io.bus.response_channel(i).valid && io.bus.response_channel(i).ready
  })

  val match_found = priority_encoder.io.output_valid
  val memresp_owner_encoded = priority_encoder.io.output_encoded
  val satisfy_pending_read = RegInit(false.B)
  val satisfy_pending_getm = RegInit(false.B)

  priority_encoder.io.input_unencoded := memresp_owner




  val resp_buffers = for { i <- 0 until masterCount } yield {
    val resp_buffer = Module(new ResponseQueue(1 << log2Ceil(masterCount), memorySystemParams))
    resp_buffer.io.q.enq.bits := mem_resp_wr_data_i(i)
    resp_buffer.io.q.enq.valid := mem_resp_wr_en_i(i)
    mem_resp_rd_data_o(i) := resp_buffer.io.q.deq.bits
    resp_buffer.io.q.deq.ready := mem_resp_rd_en_i(i)
    mem_resp_full_o(i) := !resp_buffer.io.q.enq.ready
    mem_resp_empty_o(i) := !resp_buffer.io.q.deq.valid
    resp_buffer.io.dataq_enq := mem_resp_wr_data_i(i).data

    io.bus.response_data_channel(i) <> resp_buffer.io.dataq
    when(resp_buffer.io.dataq.fire()) {
      printf("[LLC] The response DATA for CC %d is readout !!!!\n", i.U)
    }
    resp_buffer
  }

  private val logger = RegInit(0.U.asTypeOf(genErrorMessage))
  io.err <> logger

  private def log(msg: UInt): Unit = {
    when(!logger.valid) {
      logger.valid := true.B
      logger.src := EventSource.MEMORY.U
      logger.msg := msg
    }
  }

  protected def getTagAddress(addr: UInt): UInt = {
    addr(cacheParams.addrWidth - 1, cacheParams.lineOffsetWidth)
  }

  def llcWaitTimeout: Int = 300

  val data_reg = Reg(Vec(beats_per_line, UInt(busDataWidth.W)))
  val data_channel_counter = RegInit(0.U((log2Ceil(beats_per_line) + 1).W))
  val need_data = llc_state =/= LLCStates.LLC_IDLE.U && memreq_data_reg.req_wb === 1.U
  val data_done = data_channel_counter === beats_per_line.U


  when(io.bus.request_channel.fire()) { memreq_data_reg := io.bus.request_channel.bits }
  when(io.dram.response_channel.fire()) { memory_response_reg := io.dram.response_channel.bits }


  // Driving hash table request signals
  hash_table.io.request.bits.key := 0.U.asTypeOf(hash_table.io.request.bits.key)
  hash_table.io.request.bits.value := 0.U.asTypeOf(hash_table.io.request.bits.value)
  hash_table.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_READ
  hash_table.io.request.valid := 0.U
  hash_table_write_request := 0.U.asTypeOf(genHashTableEntry)
  coherenceTable.io.currentEvent := memreq_data_reg.req_type
  coherenceTable.io.currentState  := resp_state_reg.state.asTypeOf(genHashTableEntry).state
  coherenceTable.io.currentSharers:=  resp_state_reg.state.asTypeOf(genHashTableEntry).sharer
  coherenceTable.io.satisfiedPendingModified := satisfy_pending_getm
  coherenceTable.io.satisfiedSharers := pending_shares
  coherenceTable.io.requestor := memreq_data_reg.requester_id

  when(llc_state === LLCStates.LLC_HASH_READ.U) {
    hash_table.io.request.bits.key := getTagAddress(memreq_data_reg.address)
    hash_table.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_READ
    hash_table.io.request.valid := 1.U
  }.elsewhen(llc_state === LLCStates.LLC_HASH_WRITE.U) {
    hash_table.io.request.bits.key := getTagAddress(memreq_data_reg.address)
    /*
    when(memreq_data_reg.req_type === RequestType.GETM.U) {
      //hash_table.io.request.bits.value := llcLineStates.Xclusive.U
      hash_table_write_request.state := llcLineStates.Xclusive.U
      hash_table_write_request.sharer := (1.U << memreq_data_reg.requester_id).asUInt
      assert(coherenceTable.io.nextState === llcLineStates.Xclusive.U)
      assert(coherenceTable.io.nextSharers === (1.U << memreq_data_reg.requester_id).asUInt)
    }.elsewhen (memreq_data_reg.req_type === RequestType.GETS.U) {
      // the line was in the hash table
      when(resp_state_reg.ack === 1.U) {
        hash_table_write_request.state  := llcLineStates.Shared.U
        hash_table_write_request.sharer :=
          resp_state_reg.state.asTypeOf(genHashTableEntry).sharer | (1.U << memreq_data_reg.requester_id).asUInt
        assert(coherenceTable.io.nextState === llcLineStates.Shared.U)
        assert(coherenceTable.io.nextSharers ===
          (resp_state_reg.state.asTypeOf(genHashTableEntry).sharer | (1.U << memreq_data_reg.requester_id).asUInt).asUInt
        )
      }.otherwise {
        hash_table_write_request.state  := llcLineStates.Xclusive.U
        hash_table_write_request.sharer := (1.U << memreq_data_reg.requester_id).asUInt
        assert(coherenceTable.io.nextState === llcLineStates.Xclusive.U)
        assert(coherenceTable.io.nextSharers === (1.U << memreq_data_reg.requester_id).asUInt)
      }
    }.elsewhen(memreq_data_reg.req_type === RequestType.UPG.U)  {
        hash_table_write_request.state  := llcLineStates.Xclusive.U
        hash_table_write_request.sharer := (1.U << memreq_data_reg.requester_id).asUInt
        assert(coherenceTable.io.nextState === llcLineStates.Xclusive.U)
        assert(coherenceTable.io.nextSharers === (1.U << memreq_data_reg.requester_id).asUInt)
    }.elsewhen(memreq_data_reg.req_type === RequestType.PUTM.U && satisfy_pending_getm) {
      // takes precedence over pending read
      hash_table_write_request.state  := llcLineStates.Xclusive.U
      hash_table_write_request.sharer := pending_shares
      assert(coherenceTable.io.nextState === llcLineStates.Xclusive.U)
      assert(coherenceTable.io.nextSharers === pending_shares)
    }.elsewhen(memreq_data_reg.req_type === RequestType.PUTM.U && satisfy_pending_read) {
      // in private cache: M->S or E->S
      hash_table_write_request.state  := llcLineStates.Shared.U
      hash_table_write_request.sharer := resp_state_reg.state.asTypeOf(genHashTableEntry).sharer | pending_shares
      assert(coherenceTable.io.nextState === llcLineStates.Shared.U)
      assert(coherenceTable.io.nextSharers === (resp_state_reg.state.asTypeOf(genHashTableEntry).sharer | pending_shares).asUInt)
    }.elsewhen(memreq_data_reg.req_type === RequestType.PUTS.U) {
      // remove the sharer
      hash_table_write_request.state  := llcLineStates.Shared.U
      hash_table_write_request.sharer :=
        resp_state_reg.state.asTypeOf(genHashTableEntry).sharer & (~(1.U << memreq_data_reg.requester_id)).asUInt
      assert(coherenceTable.io.nextState === llcLineStates.Shared.U)
      assert(coherenceTable.io.nextSharers ===
        (resp_state_reg.state.asTypeOf(genHashTableEntry).sharer & (~(1.U << memreq_data_reg.requester_id)).asUInt).asUInt)
    } */
    hash_table_write_request.state := coherenceTable.io.nextState
    hash_table_write_request.sharer := coherenceTable.io.nextSharers
    hash_table.io.request.bits.value := hash_table_write_request.asUInt
    hash_table.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_WRITE
    hash_table.io.request.valid := 1.U
  }.elsewhen(llc_state === LLCStates.LLC_HASH_REMOVE.U) {
    hash_table.io.request.bits.key := getTagAddress(memreq_data_reg.address)
    hash_table.io.request.bits.value := 0.U
    hash_table.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_REMOVE
    hash_table.io.request.valid := 1.U
  }

  // driving dram request signals
  io.dram.request_channel.bits := 0.U.asTypeOf(genDramReq)
  io.dram.request_channel.valid := 0.U
  when(llc_state === LLCStates.LLC_MEMORY_READ.U) {
    io.dram.request_channel.bits.address := Cat(getTagAddress(memreq_data_reg.address),
      0.U(cacheParams.lineOffsetWidth.W))
    io.dram.request_channel.bits.length := "b110".U
    io.dram.request_channel.bits.mem_type := 1.U // load
    io.dram.request_channel.valid := 1.U
  }
  when(llc_state === LLCStates.LLC_MEMORY_WRITE.U && data_done === 1.U) {
    io.dram.request_channel.bits.address := Cat(getTagAddress(memreq_data_reg.address),
      0.U(cacheParams.lineOffsetWidth.W))
    io.dram.request_channel.bits.data := data_reg.asUInt // memreq_data_reg.data
    io.dram.request_channel.bits.length := "b110".U
    io.dram.request_channel.bits.mem_type := 0.U // store
    // no need to write back to dram if line is clean
    io.dram.request_channel.valid := memreq_data_reg.dirty === 1.U
  }

  // wait until the hash_table is ready
  io.bus.request_channel.ready := llc_state === LLCStates.LLC_IDLE.U && hash_table.io.request.ready
  hash_table.io.response.ready := llc_state === LLCStates.LLC_WAIT.U
  io.dram.response_channel.ready := llc_state === LLCStates.LLC_WAIT.U

  pr_lut_new.io.req.valid := 0.U
  pr_lut_new.io.req.bits.key := 0.U
  pr_lut_new.io.req.bits.data.req_type := 0.U
  pr_lut_new.io.req.bits.data.requester_id := 0.U
  pr_lut_new.io.req.bits.data.criticality := 0.U
  pr_lut_new.io.req.bits.request_type := 0.U

  pr_lut_new.io.resp.ready :=
    (llc_state === LLCStates.LLC_PR_TABLE_READ.U  &&
      !mem_resp_full_o(pr_lut_new.io.resp.bits.data.requester_id)) ||
      llc_state === LLCStates.LLC_PR_TABLE_WRITE.U

  switch(llc_state) {
    is(LLCStates.LLC_PR_TABLE_READ_REQ.U) {
      pr_lut_new.io.req.bits.key := getTagAddress(memreq_data_reg.address)
      pr_lut_new.io.req.bits.request_type := PRLUTReq.search_and_remove
      pr_lut_new.io.req.bits.data.req_type := Mux(memreq_data_reg.req_type === RequestType.GETM.U, 1.U, 0.U)
      pr_lut_new.io.req.bits.data.requester_id := memreq_data_reg.requester_id
      pr_lut_new.io.req.valid := 1.U
    }

    is(LLCStates.LLC_PR_TABLE_WRITE_REQ.U) {
      pr_lut_new.io.req.bits.key := getTagAddress(memreq_data_reg.address)
      pr_lut_new.io.req.bits.request_type := PRLUTReq.insert
      pr_lut_new.io.req.bits.data.req_type := Mux(memreq_data_reg.req_type === RequestType.GETM.U, 1.U, 0.U)
      pr_lut_new.io.req.bits.data.requester_id := memreq_data_reg.requester_id
      pr_lut_new.io.req.bits.data.criticality := memreq_data_reg.criticality
      pr_lut_new.io.req.valid := 1.U
    }
  }

  for{ i <- 0 until masterCount } {
    mem_resp_wr_en_i(i) := 0.U
  }

  // pumping data from cores
  for{i <- 0 until masterCount } {
    io.bus.data_channel(i).ready := 0.U
  }
  io.bus.data_channel(memreq_data_reg.requester_id).ready := 0.U
  when(need_data && !data_done
    && memreq_data_reg.req_type === RequestType.PUTM.U) {
    io.bus.data_channel(memreq_data_reg.requester_id).ready := 1.U
  }
  when(io.bus.data_channel(memreq_data_reg.requester_id).fire()) {
    printf(p"[LLC] Received WORD: ${Hexadecimal(io.bus.data_channel(memreq_data_reg.requester_id).bits)}, in pos ${data_channel_counter}\n")
    data_reg(data_channel_counter) := io.bus.data_channel(memreq_data_reg.requester_id).bits
    data_channel_counter := data_channel_counter + 1.U
  }

  val ht_resp_state = WireInit(hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).state)
  val ht_resp_sharer_sharer = WireInit(hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).sharer)
  dontTouch(ht_resp_state)
  dontTouch(ht_resp_state)

  switch(llc_state) {
    is(LLCStates.LLC_IDLE.U) {
      timeout := 0.U
      when(io.bus.request_channel.fire()) {
        llc_state := LLCStates.LLC_HASH_READ.U
      }
      satisfy_pending_read := false.B
      satisfy_pending_getm := false.B
      pending_shares := 0.U

      resp_state_reg.state := 0.U
      resp_state_reg.ack := 0.U
      data_channel_counter := 0.U
    }
    // =====================================

    is(LLCStates.LLC_WAIT.U) {
      timeout := timeout + 1.U
      when(timeout >= llcWaitTimeout.U) {
        printf("[LLC] Timeout, Error Occured\n")
        llc_state := LLCStates.LLC_FAILURE.U
        log(7.U)
      }
      when(hash_table.io.response.fire()) {
        switch(hash_table.io.response.bits.req_type) {
          is(HashTableReqType.sHASH_TABLE_READ) {
            resp_state_reg.state := hash_table.io.response.bits.value
            resp_state_reg.ack := hash_table.io.response.bits.ack
            when(hash_table.io.response.bits.ack.toBool()) {
              val resp = hash_table.io.response.bits.value.asTypeOf(genHashTableEntry)
              coherenceTable.io.currentSharers := resp.sharer
              coherenceTable.io.currentState := resp.state
              when(hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).state === llcLineStates.Xclusive.U) {
                // found in ht Xclusive or in Shared, in this case, we must
                // wait for another core write back
                when(memreq_data_reg.req_type === RequestType.GETM.U ||
                  memreq_data_reg.req_type === RequestType.GETS.U) {
                  // coherenceTable.io.currentSharers := resp.sharer
                  // coherenceTable.io.currentState := resp.state
                  // llc_state := LLCStates.LLC_PR_TABLE_WRITE_REQ.U
                  // assert(coherenceTable.io.actionLLCStates  === LLCStates.LLC_PR_TABLE_WRITE_REQ.U, "expected: %d, got: %d",
                  //   LLCStates.LLC_PR_TABLE_WRITE_REQ.U,
                  //   coherenceTable.io.actionLLCStates
                  // )
                  llc_state := coherenceTable.io.actionLLCStates
                }.elsewhen(memreq_data_reg.req_type === RequestType.PUTM.U) {
                  llc_state := LLCStates.LLC_PR_TABLE_READ_REQ.U
                  when(!data_done) { llc_state := LLCStates.LLC_WAIT_DATA.U }
                  when(hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).sharer =/=
                    (1.U << memreq_data_reg.requester_id).asUInt) {
                    // PUTM should match the owner
                    log(0.U)
                    llc_state := LLCStates.LLC_FAILURE.U
                  }
                }.elsewhen(memreq_data_reg.req_type === RequestType.UPG.U) {
                  llc_state := LLCStates.LLC_IDLE.U
                }.elsewhen(memreq_data_reg.req_type === RequestType.PUTS.U) {
                  // Cannot PUTS for exclusive line, should be putM
                  // printf("[LLC] Cannot PUTS for exclusive line\n")
                  // The aformentioned scenario can happen:
                  // Core 0 REPLACEMENT A: S -> SI_A
                  // Core 1 GETM A: I -> IM_AD -> IM_D (Braodcast done)
                  // Core 0 Boardcasts PUTS
                  llc_state := LLCStates.LLC_IDLE.U
                  // log(0.U)
                }
              }.elsewhen(hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).state ===
                llcLineStates.Shared.U) {
                when(memreq_data_reg.req_type === RequestType.GETM.U
                  || memreq_data_reg.req_type === RequestType.GETS.U) {
                  llc_state := LLCStates.LLC_MEMORY_READ.U
                }.elsewhen(memreq_data_reg.req_type === RequestType.UPG.U) {
                  // llc_state := LLCStates.LLC_HASH_WRITE.U
                  // coherenceTable.io.currentSharers := resp.sharer
                  // coherenceTable.io.currentState := resp.state
                  // assert(coherenceTable.io.actionLLCStates  === LLCStates.LLC_HASH_WRITE.U, "expected: %d, got: %d",
                  //   LLCStates.LLC_HASH_WRITE.U,
                  //   coherenceTable.io.actionLLCStates
                  // )
                  llc_state := coherenceTable.io.actionLLCStates
                }.elsewhen(memreq_data_reg.req_type === RequestType.PUTS.U) {
                  // it is the only sharer
                  when(hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).sharer ===
                    (1.U << memreq_data_reg.requester_id).asUInt) {
                    // Invalidate the copy of the last sharer
                    // llc_state := LLCStates.LLC_HASH_REMOVE.U
                    // coherenceTable.io.currentSharers := resp.sharer
                    // coherenceTable.io.currentState := resp.state
                    // assert(coherenceTable.io.actionLLCStates  === LLCStates.LLC_HASH_REMOVE.U, "expected: %d, got: %d",
                    //   LLCStates.LLC_HASH_REMOVE.U,
                    //   coherenceTable.io.actionLLCStates
                    // )
                    llc_state := coherenceTable.io.actionLLCStates
                  }.elsewhen((hash_table.io.response.bits.value.asTypeOf(genHashTableEntry).sharer &
                    (1.U << memreq_data_reg.requester_id).asUInt) =/= 0.U) {
                    // llc_state := LLCStates.LLC_HASH_WRITE.U
                    // coherenceTable.io.currentSharers := resp.sharer
                    // coherenceTable.io.currentState := resp.state
                    // assert(coherenceTable.io.actionLLCStates  === LLCStates.LLC_HASH_WRITE.U, "expected: %d, got: %d",
                    //   LLCStates.LLC_HASH_WRITE.U,
                    //   coherenceTable.io.actionLLCStates
                    // )
                    llc_state := coherenceTable.io.actionLLCStates
                  }.otherwise {
                    printf("[[LLC] receiving PUTS from a non-sharer.\n")
                    llc_state := LLCStates.LLC_FAILURE.U
                    log(1.U)
                  }
                }.elsewhen(memreq_data_reg.req_type === RequestType.PUTM.U) {
                  printf("[[LLC] PUTM for Shared state.\n")
                  llc_state := LLCStates.LLC_FAILURE.U
                  log(2.U)
                }
              }
            }.otherwise { // in invalid state
              // invalid or in shared state, no need to wait for another
              // core
              when(memreq_data_reg.req_type === RequestType.GETM.U
                || memreq_data_reg.req_type === RequestType.GETS.U) {
                llc_state := LLCStates.LLC_MEMORY_READ.U
              }.elsewhen(memreq_data_reg.req_type === RequestType.UPG.U) {
                // printf("[LLC] UPG from Invalid  state.\n")
                // llc_state := LLCStates.LLC_FAILURE.U
                // Could be possible as well:
                // C0          C1
                // S->SM_W     I->IM_D->M
                // (WB slot)   PUTM
                // UPG           ...
                // llc_state := LLCStates.LLC_IDLE.U
                llc_state := coherenceTable.io.actionLLCStates
                //log(3.U)
              }.elsewhen(memreq_data_reg.req_type === RequestType.PUTS.U) {
                printf("[LLC] PUTS from Invalid state.\n")
                llc_state := LLCStates.LLC_FAILURE.U
                log(4.U)
              }.elsewhen(memreq_data_reg.req_type === RequestType.PUTM.U) {
                printf("[LLC] PUTM from Invalid state.\n")
                llc_state := LLCStates.LLC_FAILURE.U
                log(5.U)
              }
            }
          }
          is(HashTableReqType.sHASH_TABLE_WRITE) {
            when(hash_table.io.response.bits.ack.toBool) {
              llc_state := LLCStates.LLC_IDLE.U
            }
          }
          is(HashTableReqType.sHASH_TABLE_REMOVE) {
            when(hash_table.io.response.bits.ack.toBool) {
              llc_state := LLCStates.LLC_IDLE.U
            }.otherwise {
              printf("Trying to remove non-existent entry from hash_table module.")
              llc_state := LLCStates.LLC_FAILURE.U
              log(6.U)
            }
          }
        }
      }

      when(io.dram.response_channel.fire()) {
        when(memreq_data_reg.req_type === RequestType.PUTM.U) {
          /*
          when(!satisfy_pending_read) {
            llc_state := LLCStates.LLC_HASH_REMOVE.U
            assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_REMOVE.U)
          }.otherwise{
            llc_state := LLCStates.LLC_HASH_WRITE.U
            printf(">>> TARGET::: State: %d Event: %d Action: %d\n", coherenceTable.io.currentState, coherenceTable.io.currentEvent, coherenceTable.io.actionLLCStates)
            assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_WRITE.U)
          }*/
          llc_state := coherenceTable.io.actionLLCStates
        }.otherwise {
          llc_state := LLCStates.LLC_RESP_QUEUE_PUSH.U
        }
      }
    }

    is(LLCStates.LLC_HASH_READ.U) {
      when(hash_table.io.request.fire()) {
        llc_state := LLCStates.LLC_WAIT.U
      }
    }
    is(LLCStates.LLC_HASH_WRITE.U) {
      timeout := timeout + 1.U
      when(timeout >= llcWaitTimeout.U) {
        printf("LLC Timeout, Error Occured\n")
        llc_state := LLCStates.LLC_FAILURE.U
        log(9.U)
      }
      when(hash_table.io.request.fire()) {
        llc_state := LLCStates.LLC_WAIT.U
      }
    }
    is(LLCStates.LLC_HASH_REMOVE.U) {
      when(hash_table.io.request.fire()) {
        llc_state := LLCStates.LLC_WAIT.U
      }
    }
    is(LLCStates.LLC_PR_TABLE_READ_REQ.U) { // data is read
      when(pr_lut_new.io.req.fire()) { llc_state := LLCStates.LLC_PR_TABLE_READ.U }
    }
    is(LLCStates.LLC_PR_TABLE_READ.U) { // data is read
      when(pr_lut_new.io.resp.fire()) {
        // printf(p"[LLC] PRLUT: ${pr_lut_new.io.resp}\n")
        when(pr_lut_new.io.resp.bits.found === 1.U) { // fulfilling previous requests
          //there is pending request
          when(pr_lut_new.io.resp.bits.data.req_type === 0.U) {
            satisfy_pending_read := true.B
            pending_shares := pending_shares | (1.U << pr_lut_new.io.resp.bits.data.requester_id).asUInt
            when(!mem_resp_full_o(pr_lut_new.io.resp.bits.data.requester_id)) { // GETS
              mem_resp_wr_en_i(pr_lut_new.io.resp.bits.data.requester_id) := 1.U
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).data := data_reg.asUInt
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).address := memreq_data_reg.address
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).ack := 1.U
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).is_edata := 0.U
            }
            when(pr_lut_new.io.resp.bits.last === 1.U) {
              llc_state := LLCStates.LLC_MEMORY_WRITE.U
            }
          }.otherwise {
            when(!mem_resp_full_o(pr_lut_new.io.resp.bits.data.requester_id)) { // GETM
              satisfy_pending_getm := true.B
              pending_shares := (1.U << pr_lut_new.io.resp.bits.data.requester_id).asUInt
              mem_resp_wr_en_i(pr_lut_new.io.resp.bits.data.requester_id) := 1.U

              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).data := data_reg.asUInt
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).address := memreq_data_reg.address
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).ack := 1.U
              mem_resp_wr_data_i(pr_lut_new.io.resp.bits.data.requester_id).is_edata := 1.U
              // Update owner
              llc_state := LLCStates.LLC_HASH_WRITE.U
            }
          }
        }.otherwise {
          llc_state := LLCStates.LLC_MEMORY_WRITE.U
        }




      }
    }

    is(LLCStates.LLC_PR_TABLE_WRITE_REQ.U) {
      when(pr_lut_new.io.req.fire()) { llc_state := LLCStates.LLC_PR_TABLE_WRITE.U }
    }
    is(LLCStates.LLC_PR_TABLE_WRITE.U) {
      // no response for write, as we can do nothing about a full pr table
      llc_state := LLCStates.LLC_IDLE.U
    }
    is(LLCStates.LLC_MEMORY_READ.U) {
      when(io.dram.request_channel.fire()) {
        llc_state := LLCStates.LLC_WAIT.U
      }
    }
    is(LLCStates.LLC_MEMORY_WRITE.U) {
      // printf(p"[LLC] satisfied reader: $satisfy_pending_read, readers: ${Binary(pending_shares)}, satisfied modifier: $satisfy_pending_getm\n")
      // after PR is sent
      when(io.dram.request_channel.fire()) {
        llc_state := LLCStates.LLC_WAIT.U
      }.elsewhen(memreq_data_reg.req_type === RequestType.PUTM.U && !memreq_data_reg.dirty) {
        /*
        when(satisfy_pending_read) {
          llc_state := LLCStates.LLC_HASH_WRITE.U
          assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_WRITE.U)
        }.elsewhen(satisfy_pending_getm) { // replacement
          llc_state := LLCStates.LLC_HASH_WRITE.U
          assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_WRITE.U)
        }.otherwise {
          llc_state := LLCStates.LLC_HASH_REMOVE.U
          assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_REMOVE.U)
        } */
        llc_state := coherenceTable.io.actionLLCStates
      }
    }
    is(LLCStates.LLC_RESP_QUEUE_PUSH.U) {
      when(!mem_resp_full_o(memreq_data_reg.requester_id)) {
        printf(p"  PUSHING response into Q[${memreq_data_reg.requester_id}], data: ${Hexadecimal(memory_response_reg.data)}\n")
        mem_resp_wr_en_i(memreq_data_reg.requester_id) := 1.U
        mem_resp_wr_data_i(memreq_data_reg.requester_id).data := memory_response_reg.data
        mem_resp_wr_data_i(memreq_data_reg.requester_id).address := memreq_data_reg.address
        mem_resp_wr_data_i(memreq_data_reg.requester_id).ack := 1.U
        mem_resp_wr_data_i(memreq_data_reg.requester_id).is_edata := resp_state_reg.ack === 0.U ||
          resp_state_reg.state.asTypeOf(genHashTableEntry).state =/= llcLineStates.Shared.U
        // exclusive if it was not in the hash table
        //

        /*
        when(memreq_data_reg.req_type === RequestType.GETM.U) {
          //printf("[LLC] I -> X\n")
          llc_state := LLCStates.LLC_HASH_WRITE.U // I -> Xclusive
          assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_WRITE.U)
        }.elsewhen(memreq_data_reg.req_type === RequestType.GETS.U) {
          when(resp_state_reg.ack === 1.U) {
            //printf("[LLC] X -> S\n")
            llc_state := LLCStates.LLC_HASH_WRITE.U // Xclusive -> Shared
            assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_WRITE.U)
          }.otherwise {
            //printf("[LLC] I -> X\n")
            llc_state := LLCStates.LLC_HASH_WRITE.U // I -> Xclusive
            assert(coherenceTable.io.actionLLCStates === LLCStates.LLC_HASH_WRITE.U)
          }
        }
         */
        llc_state := coherenceTable.io.actionLLCStates
      }
    }
    is(LLCStates.LLC_WAIT_DATA.U) {
      when(data_done) {
        llc_state := LLCStates.LLC_PR_TABLE_READ_REQ.U
      }
    }
    // ====================================

  }

  /*
  val (ctr, wrap) = Counter(true.B, 3000)
  when(wrap) { assert(false.B) }
   */


  for {i <- 0 until masterCount} {
    io.bus.response_channel(i).valid := !mem_resp_empty_o(i)
    mem_resp_rd_en_i(i) := 0.U
    when(io.bus.response_channel(i).fire()) {
      mem_resp_rd_en_i(i) := 1.U
    }
  }

  for{i <- 0 until masterCount } {
    io.bus.response_channel(i).bits := mem_resp_rd_data_o(memresp_owner_encoded)
  }
  val llcReg = RegInit(LLCStates.LLC_IDLE.U)
  llcReg := llc_state
  when(llcReg =/= llc_state) {
    printf("[LCC] ")
    LLCStates.printState(llc_state)
    printf(p" ${hash_table.io.request} ${hash_table.io.request.bits.value.asTypeOf(genHashTableEntry)}")
    printf("\n")
  }

  when(hash_table.io.response.fire()) {
    // printf("[LCC-HT] ")
    // printf(p" ${hash_table.io.response} ${hash_table.io.response.bits.value.asTypeOf(genHashTableEntry)}")
    // printf("\n")
  }

  for{i <- 0 until masterCount } {
    when(io.bus.response_channel(i).fire()) {
      printf(p"[LLC] -> [CC$i] ${io.bus.response_channel(0).bits}\n")
    }
    when(resp_buffers(i).io.q.enq.valid === 1.U) {
      printf(p"[LLC] Writing to resp buffer of $i: ${resp_buffers(i).io.q.enq.bits}\n")
    }
    when(resp_buffers(i).io.dataq.valid) {
      printf("[LLC] Core%d has pending data\n", i.U)
    }
  }
  // printf(p"[LLC] memresp_owner_encoded: $memresp_owner_encoded, memresp_owner ${Binary(memresp_owner)}\n")

  when(io.dram.request_channel.fire()) {
    printf(p"[LLC] type: ${io.dram.request_channel.bits.mem_type}, dram_channel (req): ${core.Hexadecimal(io.dram.request_channel.bits.data)}\n")
  }
  when(io.dram.response_channel.fire()) {
    printf(p"[LLC] type: dram_channel (resp): ${core.Hexadecimal(io.dram.response_channel.bits.data)}\n")
  }

  // --------- added for debugging information about the slot-width ----------
  val requestProcessingCounter = RegInit(0.U)
  val coreID = RegInit(0.U)
  val started = RegInit(false.B)
  when(io.bus.request_channel.fire()) {
    when(io.bus.request_channel.bits.req_type === RequestType.PUTM.U) {
      coreID := io.bus.request_channel.bits.requester_id
      started := true.B
      requestProcessingCounter := 1.U
    }
  }
  when(started) {
    requestProcessingCounter := requestProcessingCounter + 1.U
  }
  when(io.dram.response_channel.fire()) {
    when(started) {
      started := false.B
      requestProcessingCounter := 0.U
      printf("[LLC] Processing Request Latency for Core %d: %d", coreID, requestProcessingCounter)
    }
  }

}
