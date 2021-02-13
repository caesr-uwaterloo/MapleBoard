
package components

import chisel3._
import chisel3.util._
import _root_.core.MemoryRequestType
import _root_.core.AMOOP
import param.CoreParam
import params._
object DataModification {
  class DataModificationIO[S <: Data, M <: Data, B <: Data](
                                                             m: MemorySystemParams,
                                                            cohSpec: CoherenceSpec[S, M, B]) extends Bundle {
    val pipe_in = Flipped(Decoupled(new PipeData(m, cohSpec)))
    val pipe_out = Decoupled(new PipeData(m, cohSpec))
    val core_response = Decoupled(m.getGenCacheResp)
    val replayBuffer = Decoupled(m.getGenCacheReq)
    val releaseReplay = Output(Bool())

    val mshr_ctrl = new Bundle {
      val wen = Output(Bool())
      val waddr = Output(UIntHolding(m.MSHRSize))
      val insert = Output(Bool())
      val remove = Output(Bool())
      val wdata = Output(m.getGenCacheReq)
    }
    val mshr = Input(Vec(m.MSHRSize, m.getGenCacheReq))
    val mshrValid = Input(Vec(m.MSHRSize, Bool()))

    override def cloneType: this.type = new DataModificationIO(m, cohSpec).asInstanceOf[this.type]
    val id = Input(UIntHolding(m.masterCount + 1))
    val time = Input(UIntHolding(128))
  }
}

// either reply the cache response or clean the MSHR
class DataModification[S <: Data, M <: Data, B <: Data](coreParams: CoreParam,
                                                        m: MemorySystemParams,
                                                        cohSpec: CoherenceSpec[S, M, B]) extends Module {
  val io = IO(new DataModification.DataModificationIO(m, cohSpec))
  assert(m.dataWidth == 64)
  io.pipe_out.bits := io.pipe_in.bits
  val genCacheLineWords = Vec(m.cacheParams.lineWidth / m.dataWidth, UInt(m.dataWidth.W))
  val lastMSHR = WireInit(false.B)
  val dataSrcSel = WireInit(false.B)
  val mshrMatch = Wire(Vec(m.MSHRSize, Bool()))
  val dataReg = Reg(genCacheLineWords)
  val pipe_data = io.pipe_in.bits
  val coh_resp = io.pipe_in.bits.coh_resp

  val lrsc_address = RegInit(0.U(m.cacheParams.addrWidth.W))
  val lrsc_valid = RegInit(false.B)
  val lrsc_coutner = RegInit(0.U(log2Ceil(m.masterCount * m.slotWidth).W))
  when(lrsc_valid && lrsc_coutner > 0.U) {
    lrsc_coutner := lrsc_coutner - 1.U
  }
  // generate lastMSHR
  for {i <- 0 until m.MSHRSize} {

    when(io.mshrValid(i) &&
      m.cacheParams.getTagAddress(io.mshr(i).address) === m.cacheParams.getTagAddress(pipe_data.address)) {
      mshrMatch(i) := true.B
    }.otherwise {
      mshrMatch(i) := false.B
    }
    when(mshrMatch.asUInt === (1 << i).U) {
      lastMSHR := true.B
    }
  }
  val allUnmatch = mshrMatch.foldLeft(true.B)(_ && !_)
  val satisfied = RegInit(0.U)
  when(io.pipe_out.fire()) {
    satisfied := 0.U
  }.elsewhen(io.core_response.fire()) {
    satisfied := 1.U
  }
  // dataSrcSel := PopCount(io.mshrValid) >= 1.U
  dataSrcSel := satisfied =/= 0.U
  val canProceed = WireInit(false.B)
  when(io.pipe_in.valid) {
    when(pipe_data.coh_resp.cleanMSHR) {
      when(lastMSHR) {
        canProceed := true.B
      }.elsewhen(allUnmatch) {
        assert(false.B, "mshr must have a corresponding request! %b", mshrMatch.asUInt)
      }
    }.otherwise { // no need for multiple cycles...
      canProceed := true.B
    }
  }

  io.pipe_in.ready := canProceed && io.pipe_out.ready
  io.pipe_out.valid := canProceed && io.pipe_in.valid
  val currentCoreRequest = Wire(m.getGenCacheReq)
  val hitMSHR = Wire(UIntHolding(m.MSHRSize))
  hitMSHR := 0.U
  currentCoreRequest := pipe_data.core
  when(io.pipe_in.valid) {
    when(coh_resp.cleanMSHR) {
      // we need to get one from one of the matching request...
      0.until(m.MSHRSize).foldLeft(when(false.B) {}) { (prev, id) =>
        prev.elsewhen(mshrMatch(id)) {
          currentCoreRequest := io.mshr(id)
          hitMSHR := id.U
        }
      }
    }
  }


  val modified_data_for_cache_resp = Wire(genCacheLineWords)
  val original_data_for_cache_resp = Wire(genCacheLineWords)
  val read_word = Wire(UInt(m.dataWidth.W))
  val write_word = Wire(UInt(m.dataWidth.W))
  val write_word_byte = Wire(Vec(m.dataWidth / 8, UInt(8.W)))
  val offset_start = log2Ceil(m.dataWidth / 8) // 3
  val offset_end = m.cacheParams.lineOffsetWidth
  val word_offset = currentCoreRequest.address(offset_end - 1, offset_start)
  val byte_offset = currentCoreRequest.address(offset_start - 1, 0)
  val shiftamt = Cat(byte_offset, 0.U(3.W))
  val amoALU = Module(new AMOALU(coreParams, m.cacheParams))
  /*
  amoALU.io.amo_alu_op := cachereq_data_reg.amo_alu_op
  amoALU.io.isW := cachereq_data_reg.length === 2.U
  amoALU.io.in1 := cache_resp_data
  amoALU.io.in2 := cachereq_data_reg.data
  amoALU.io.lrsc_valid := lrsc_valid
  amoALU.io.lrsc_address := lrsc_address
  amoALU.io.sc_address := cachereq_data_reg.address
  */
  when(dataSrcSel === false.B) {
    original_data_for_cache_resp := io.pipe_in.bits.data.asTypeOf(modified_data_for_cache_resp)
  }.otherwise {
    original_data_for_cache_resp := dataReg.asTypeOf(modified_data_for_cache_resp)
  }
  modified_data_for_cache_resp := original_data_for_cache_resp



  val shifted_data = Wire(UInt(m.dataWidth.W))
  val shifted_write_data = Wire(UInt(m.dataWidth.W))
  val shifted_write_data_byte = Wire(Vec(m.dataWidth / 8, UInt(8.W)))
  val amo_shifted_write_data = Wire(UInt(m.dataWidth.W))
  val amo_shifted_write_data_byte = Wire(Vec(m.dataWidth / 8, UInt(8.W)))
  val write_word_mask = Wire(UInt((m.dataWidth / 8).W))
  read_word := original_data_for_cache_resp(word_offset)
  // read_word := original_data_for_cache_resp(word_offset)
  shifted_data := read_word >> shiftamt
  shifted_write_data := currentCoreRequest.data << shiftamt
  when(currentCoreRequest.use_wstrb) {
    shifted_write_data_byte := currentCoreRequest.data.asTypeOf(shifted_write_data_byte)
    assert(shifted_write_data_byte.getWidth == currentCoreRequest.data.getWidth)
  }.otherwise {
    shifted_write_data_byte := shifted_write_data.asTypeOf(shifted_write_data_byte)
  }
  amo_shifted_write_data := amoALU.io.out << shiftamt
  amo_shifted_write_data_byte := amo_shifted_write_data.asTypeOf(amo_shifted_write_data_byte)
  write_word_mask := 0.U

  amoALU.io.amo_alu_op := currentCoreRequest.amo_alu_op
  amoALU.io.isW := currentCoreRequest.length === 2.U
  amoALU.io.in1 := shifted_data
  amoALU.io.in2 := currentCoreRequest.data
  amoALU.io.lrsc_valid := lrsc_valid
  amoALU.io.lrsc_address := lrsc_address
  amoALU.io.sc_address := currentCoreRequest.address

  when(currentCoreRequest.use_wstrb) {
    write_word_mask := currentCoreRequest.wstrb.asTypeOf(write_word_mask)
    assert(currentCoreRequest.wstrb.getWidth == write_word_mask.getWidth)
  }.otherwise {
    switch(currentCoreRequest.length) {
      is(0.U) {
        write_word_mask := "b1".U << byte_offset
      }
      is(1.U) {
        write_word_mask := "b11".U << byte_offset
      }
      is(2.U) {
        write_word_mask := "b1111".U << byte_offset
      }
      is(3.U) {
        write_word_mask := "b11111111".U << byte_offset
      }
    }
  }
  for { i <- 0 until m.dataWidth / 8 } {
    when(write_word_mask(i)) {
      when(currentCoreRequest.mem_type === MemoryRequestType.amo &&
        currentCoreRequest.amo_alu_op =/= AMOOP.lr &&
        currentCoreRequest.amo_alu_op =/= AMOOP.sc
      ) {
        write_word_byte(i) := amo_shifted_write_data_byte(i)
      }.otherwise {
        write_word_byte(i) := shifted_write_data_byte(i)
      }
    }.otherwise {
      write_word_byte(i) := read_word((i + 1) * 8 - 1, i * 8)
    }
  }
  write_word := write_word_byte.asTypeOf(write_word)

  // io.core_response.bits.data := shifted_data // for read (maybe amo?)
  io.core_response.bits.data := 0.U // for read (maybe amo?)
  io.core_response.bits.latency := 0.U // for read (maybe amo?)
  switch(currentCoreRequest.length) {
    is(0.U) { io.core_response.bits.data := shifted_data(7,0) }
    is(1.U) { io.core_response.bits.data := shifted_data(15,0) }
    is(2.U) { io.core_response.bits.data := shifted_data(31,0) }
    is(3.U) { io.core_response.bits.data := shifted_data(63,0) }
  }
  io.core_response.bits.mem_type := currentCoreRequest.mem_type
  io.core_response.bits.address := currentCoreRequest.address
  io.core_response.bits.length := currentCoreRequest.length
  io.core_response.valid := false.B


  val sc_succ = lrsc_valid &&
    m.cacheParams.getTagAddress(lrsc_address) === m.cacheParams.getTagAddress(currentCoreRequest.address)

  dontTouch(lrsc_valid)
  dontTouch(lrsc_address)
  dontTouch(currentCoreRequest.address)

  when(io.pipe_in.valid) {
    when(coh_resp.pushCacheResp || coh_resp.cleanMSHR) {
      // assert(pipe_data.src =/= ESource.snoop, "Snoop cannot modify data")
      assert(pipe_data.src === ESource.core && coh_resp.pushCacheResp ||
        pipe_data.src === ESource.mem && coh_resp.cleanMSHR ||
        pipe_data.src === ESource.snoop && coh_resp.cleanMSHR, "Sources must be correct for replying... %d",
        io.id)
      io.core_response.valid := io.pipe_out.fire()
      printf(p"CurrentCoreRequest: ${currentCoreRequest}\n")
      when(currentCoreRequest.mem_type === MemoryRequestType.read) {
        printf(p"Hit Branch 1\n")
        // driveDataBasedOnReq(currentCoreRequest, io.core_response.bits)
        lrsc_valid := false.B
      }.elsewhen(currentCoreRequest.mem_type === MemoryRequestType.write) {
        printf(p"Hit Branch 2\n")
        modified_data_for_cache_resp(word_offset) := write_word
        lrsc_valid := false.B
      }.elsewhen(currentCoreRequest.mem_type === MemoryRequestType.amo) {
        when(currentCoreRequest.amo_alu_op === AMOOP.lr) {
          printf("Hit AMO LR\n")
          lrsc_address := currentCoreRequest.address
          lrsc_coutner := 255.U // 255 cycles
          lrsc_valid := true.B
        }.elsewhen(currentCoreRequest.amo_alu_op === AMOOP.sc) {
          printf("Hit AMO SC\n")
          when(sc_succ) {
            printf("Hit AMO SC Succ\n")
            modified_data_for_cache_resp(word_offset) := write_word
            io.core_response.bits.data := 0.U
            lrsc_valid := false.B
          }.otherwise {
            printf("Hit AMO SC Failurie\n")
            io.core_response.bits.data := 1.U
          }
        }.otherwise {
          printf(p"Hit Branch AMO\n")
          modified_data_for_cache_resp(word_offset) := write_word // amoALU.io.out << shiftamt
          printf(p"Current Response?: \n")
          utils.printbundle(io.core_response.bits)
          printf("\n")
          lrsc_valid := false.B
          printf(p"modified: ${modified_data_for_cache_resp}\n")
        }
      }.elsewhen(currentCoreRequest.mem_type === MemoryRequestType.sc){
        printf("Hit SC\n")
        when(sc_succ) {
          printf("Hit SC Succ\n")
          modified_data_for_cache_resp(word_offset) := write_word
          io.core_response.bits.data := 0.U
          lrsc_valid := false.B
        }.otherwise {
          io.core_response.bits.data := 1.U
        }
      }.elsewhen(currentCoreRequest.mem_type === MemoryRequestType.lr) {
        printf("Hit LR")
        // driveDataBasedOnReq(currentCoreRequest, io.core_response.bits)
        lrsc_address := currentCoreRequest.address
        lrsc_coutner := 255.U // 255 cycles
        lrsc_valid := true.B
      }.otherwise {
          assert(false.B, "other type of memory operation is not supported yet etc)")
      }
      io.pipe_out.bits.data := modified_data_for_cache_resp.asTypeOf(io.pipe_out.bits.data)
      // just write it anyway
      dataReg := modified_data_for_cache_resp
    }
    // When another core tries to access this same data
    when(pipe_data.src === ESource.snoop && lrsc_valid &&
      m.cacheParams.getTagAddress(pipe_data.address) === m.cacheParams.getTagAddress(lrsc_address) &&
      // Delay the invalidation of the databus a bit
      pipe_data.coh_resp.pushDataBus) {
      lrsc_valid := false.B
    }
  }

  when(io.core_response.valid) {
    assert(io.core_response.ready, "The core response queue should not congest")
  }

  io.replayBuffer.bits := io.pipe_in.bits.core
  when(io.pipe_out.fire()) {
    // TODO: this may be replaced with valid...but with assertions
    io.replayBuffer.valid := coh_resp.insertReplay
    io.releaseReplay := coh_resp.releaseReplay
  }.otherwise {
    io.replayBuffer.valid := false.B
    io.releaseReplay := false.B
  }



  // This part is a bit different, requires enumeration !
  // That said we probably dont need to check the tag
  // we need a bit check...to satisfy the operation...
  io.mshr_ctrl.wen := false.B
  io.mshr_ctrl.insert := false.B
  io.mshr_ctrl.waddr := pipe_data.freeMSHR
  io.mshr_ctrl.wdata := pipe_data.core
  when(coh_resp.insertMSHR) {
    io.mshr_ctrl.insert := true.B
  }
  io.mshr_ctrl.remove := false.B
  when(coh_resp.cleanMSHR) {
    io.mshr_ctrl.remove := true.B
    io.mshr_ctrl.waddr := hitMSHR
  }

  // must be assertion for the mshr, there must only be one match. otherwise, we need modifications!

  when(io.pipe_in.valid) {
    printf("=== [CC%d.DataMod] @%d ===\n", io.id, io.time)
    utils.printbundle(io.pipe_in.bits.coh_resp)
    printf("\n")
    printf(p"canProceed ${canProceed}\n")
    printf("\n")
    when(io.core_response.fire()) {
      printf(" Sent Data Response to Core: ")
      utils.printbundle(io.core_response.bits)
      printf("\n")
    }
    printf(p"mshrMatch: ${mshrMatch} lastMSHR: ${lastMSHR} hitMSHR: ${hitMSHR}\n")
    printf(p"mshr: ${io.mshr}\n")
    printf(p"address: ${Hexadecimal(pipe_data.address)}\n")
    // printf(p"${Hexadecimal(shifted_data)}\n")
    // printf(p"${Hexadecimal(shifted_write_data)}\n")
    // printf(p"${shifted_write_data_byte}\n")
    // printf(p"${Hexadecimal(write_word_mask)}\n")
    // printf(p"writeword: ${Hexadecimal(write_word)}\n")
    // printf(p"writewordbyte: ${Hexadecimal(write_word_byte.asUInt)}\n")
    // printf(p"WordOffset: ${word_offset}\n")
    // printf(p"ByteOffset: ${byte_offset}\n")
    // printf(p"Modified: ${Hexadecimal(modified_data_for_cache_resp.asUInt)}\n")
    when(io.pipe_in.bits.coh_resp.defined) {
      // when the transition is enabled, we need to set it as err
      assert(!io.pipe_in.bits.coh_resp.isErr, "Error defined in coherence table happens Core %d", io.id)
    }
  }

}
