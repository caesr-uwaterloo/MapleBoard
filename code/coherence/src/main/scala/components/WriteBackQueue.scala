
package components

import chisel3._
import chisel3.util._
import firrtl.transforms.DontTouchAnnotation
import params.{MemorySystemParams, SimpleCacheParams}

class WriteBackQueueIO(val depth: Int,
                       val dataWidth: Int,
                       val busDataWidth: Int,
                       val cacheParams: SimpleCacheParams,
                       val memorySystemParams: MemorySystemParams,
                       private val genMemReq: MemReq) extends Bundle {
  val q = new QueueIO(memorySystemParams.getGenMemReqCommand, depth)
  // this port allows requests to hit for pending write back requests
  val peek = new Bundle {
    val address = Input(UInt(cacheParams.addrWidth.W))
    val found = Output(Bool())

    val enable = Input(Bool())
    val remove = Input(Bool())
    val read_write = Input(UInt(1.W))
    val length = Input(UInt(2.W))
    val data_in = Input(UInt(dataWidth.W))
    val data_out = Output(UInt(dataWidth.W))
  }

  // interface for pumping data to the bus
  // the logic itself should be responsible for counting the number data beats to transfer
  // in the scenario of integration, the user is required decorate the interface with signals like last
  val dataq = new QueueIO(UInt(busDataWidth.W), depth * cacheParams.lineWidth / busDataWidth)

  val cancelled = Output(UInt(32.W))
}

class WriteBackQueue(val depth: Int,
                     val memorySystemParams: MemorySystemParams) extends Module{

  private val dataWidth = memorySystemParams.dataWidth
  private val busDataWidth = memorySystemParams.busDataWidth
  private val cacheParams = memorySystemParams.cacheParams
  private val beat_per_request = cacheParams.lineWidth / busDataWidth
  private val genMemReq = memorySystemParams.getGenMemReq

  val io = IO(new WriteBackQueueIO(depth, dataWidth, busDataWidth, cacheParams, memorySystemParams, genMemReq))

  val read_ptr = RegInit(0.U(log2Ceil(depth).W))
  val write_ptr = RegInit(0.U(log2Ceil(depth).W))

  val read_data_ptr = RegInit(0.U(log2Ceil(depth).W))
  val write_data_ptr = RegInit(0.U(log2Ceil(depth).W))

  val (read_data_counter, read_data_counter_wrap) = Counter(io.dataq.deq.fire(), beat_per_request)
  val (write_data_counter, write_data_counter_wrap) = Counter(io.dataq.enq.fire(), beat_per_request)

  val count = RegInit(0.U(log2Ceil(depth).W))
  val command = Reg(Vec(depth, memorySystemParams.getGenMemReqCommand))
  //val data = Reg(Vec(depth, Vec(cacheParams.lineBytes, UInt(8.W)) ))
  val data_bram = Module(new ByteEnableBRAMVerilog(depth, cacheParams.lineWidth))
  val data_bram_rdata_as_byte = data_bram.io.rdata.asTypeOf(Vec(cacheParams.lineBytes, UInt(8.W)))
  val valid_data = RegInit(VecInit.tabulate(depth)(_ => 0.U(1.W)))
  val empty = count === 0.U
  val full = count === depth.U
  val match_lines = VecInit((0 until depth).map((f: Int) => {
    // only take the highest order bits
    valid_data(f).toBool && cacheParams.getTagAddress(io.peek.address) === cacheParams.getTagAddress(command(f).address)
  }))
  val has_match = match_lines.asUInt.orR
  val matched_line = OHToUInt(match_lines)
  val output_bits = Wire(memorySystemParams.getGenMemReqCommand)
  val offset = io.peek.address(cacheParams.lineOffsetWidth - 1, 0)


  io.q.count := count

  // Logic for queue
  when(io.q.enq.fire() && io.q.deq.fire()) {
    //printf(p"[WBQ] Enq: ${io.q.enq.bits} Deq: ${io.q.deq.bits}\n")
    //printf(p"[WBQDBG] W: ${write_ptr}, R: ${read_ptr}\n")
    write_ptr := write_ptr + 1.U
    read_ptr := read_ptr + 1.U

    valid_data(write_ptr) := true.B
    valid_data(read_ptr) := false.B
  }.elsewhen(io.q.enq.fire()) {
    printf(p"[WBQ] Enq: ${io.q.enq.bits}\n")
    printf(p"[WBQDBG] W: ${write_ptr}, R: ${read_ptr}\n")
    write_ptr := write_ptr + 1.U
    count := count + 1.U
    valid_data(write_ptr) := true.B
  }.elsewhen(io.q.deq.fire()) {
    printf(p"[WBQ] Deq: ${io.q.deq.bits}\n")
    printf(p"[WBQDBG] W: ${write_ptr}, R: ${read_ptr}\n")
    read_ptr := read_ptr + 1.U
    count := count - 1.U
    valid_data(read_ptr) := false.B
  }
  //printf(p"[WBQDBG] @Zero: ${command(0)}\n")
  //printf(p"[WBQDBG] outputbits: ${output_bits}\n")
  //printf(p"[WBQDBG] W: ${write_ptr}, R: ${read_ptr}\n")

  val write_done = write_data_ptr === write_ptr
  val read_done = read_data_ptr === read_ptr
  val origin_peek_data_out = Wire(UInt(busDataWidth.W))
  val origin_q_data_out = Wire(UInt(busDataWidth.W))

  data_bram.io.clock := clock
  data_bram.io.reset := reset

  output_bits := command(read_ptr)
  // output_bits.data := data(read_ptr).asUInt
  io.q.enq.ready := !full && write_done
  io.q.deq.valid := !empty && read_done && valid_data(read_ptr).asBool
  io.q.deq.bits := output_bits

  io.dataq.enq.ready := !write_done && !io.peek.enable
  io.dataq.deq.valid := !read_done && !io.peek.enable
  io.dataq.deq.bits := Cat(for {i <- (0 until busDataWidth / 8).reverse} yield {
    data_bram_rdata_as_byte( (read_data_counter << log2Ceil(busDataWidth / 8).U).asUInt + i.U)
  })
  origin_q_data_out :=  Cat(for {i <- (0 until busDataWidth / 8).reverse} yield {
    // data(read_data_ptr)( (read_data_counter << 2.U).asUInt + i.U)
    0.U
  })
  io.dataq.count := 0.U

  val wdata_byte_enable_q = WireInit(VecInit.tabulate(cacheParams.lineBytes)(_ => 0.U(1.W)))
  val wdata_q = WireInit(VecInit.tabulate(cacheParams.lineWidth / busDataWidth)(_ => io.dataq.enq.bits))
  when(io.dataq.enq.fire()) {
    for{i <- 0 until busDataWidth / 8} {
      // data(write_data_ptr)( (write_data_counter << 2.U).asUInt + i.U) := io.dataq.enq.bits((i + 1) * 8 - 1, i * 8)
      wdata_byte_enable_q((write_data_counter << log2Ceil(busDataWidth / 8).U).asUInt + i.U) := 1.U
    }
    printf(p"[WBQWB] ${Binary(wdata_byte_enable_q.asUInt)}\n")
    printf(p"[WBQWB] ${Hexadecimal(wdata_q.asUInt)}\n")
  }
  // printf(p"[WBQ] Enq ${io.dataq.enq} (${Hexadecimal(io.dataq.enq.bits)}), write_ptr ${write_ptr}, write_data_ptr ${write_data_ptr}, write_done ${write_done}\n")
  // printf(p"[WBQ] Deq ${io.dataq.deq} (${Hexadecimal(io.dataq.deq.bits)}), read_ptr ${read_ptr}, read_data_ptr ${read_data_ptr}, read_done ${read_done} (${count})\n")
  // printf(p"[WBQ] Enq ${io.dataq.enq.valid}, ${io.dataq.enq.ready} Deq ${io.dataq.deq.valid}, ${io.dataq.deq.ready}\n")
  // printf(p"[WBQ] RDATA ${Hexadecimal(data_bram_rdata_as_byte.asUInt)}\n")
  // printf(p"[WBQ] peek ${io.peek.enable}\n")
  when(io.q.enq.fire()) {
    command(write_ptr) := io.q.enq.bits
  }

  // Logic for peek and modification
  // note that simultaneous requests of deq/enq and modification may result in failure
  io.peek.found := has_match

  io.peek.data_out := 0.U
  origin_peek_data_out := 0.U
  val wdata_byte_enable_peek = WireInit(VecInit.tabulate(cacheParams.lineBytes)(_ => 0.U(1.W)))
  val wdata_in_tmp = (io.peek.data_in << (io.peek.address(log2Ceil(busDataWidth / 8) - 1, 0) << 3).asUInt).asUInt
  val wdata_in = Wire(UInt(busDataWidth.W))
  wdata_in := wdata_in_tmp
  val wdata_peek = VecInit.tabulate(cacheParams.lineWidth / memorySystemParams.dataWidth)(_ => wdata_in)
  val cancelledCount = RegInit(0.U(32.W))
  io.cancelled := cancelledCount

  dontTouch(io.cancelled)
  val last_remove = RegInit(false.B)
  last_remove := false.B

  when(io.peek.enable && io.peek.read_write === 0.U && has_match) { // write & remove
    // It's ok to not found the data here
    when(!has_match) { /* printf(p"[WBQ] Invalid Write @${Hexadecimal(io.peek.address)}\n") */ }
    when(io.peek.remove === false.B) {
      switch(io.peek.length) {
        is(0.U) { // b
          // data(matched_line)(offset) := io.peek.data_in
          wdata_byte_enable_peek(offset) := 1.U
        }
        is(1.U) { // 16b
          for {i <- 0 until 2} {
            // data(matched_line)(offset + i.U) := io.peek.data_in((i + 1)*8 - 1, i * 8)
            wdata_byte_enable_peek(offset + i.U) := 1.U
          }
        }
        is(2.U) { // 32b
          for {i <- 0 until 4} {
            // data(matched_line)(offset + i.U) := io.peek.data_in((i + 1)*8 - 1, i * 8)
            wdata_byte_enable_peek(offset + i.U) := 1.U
          }
        }
        is(3.U) { // 32b
          for {i <- 0 until 8} {
            // data(matched_line)(offset + i.U) := io.peek.data_in((i + 1)*8 - 1, i * 8)
            wdata_byte_enable_peek(offset + i.U) := 1.U
          }
        }
      }
    }.otherwise {
      // This is a remove, only happening...
      printf("[WBQ] Hit the remove path\n")
      when(has_match) {

        // Invalidate that line
        printf("[WBQ] Removing entry from the Lo_CRIT_WBQ\n")
        valid_data(matched_line) := false.B
        cancelledCount := cancelledCount + 1.U
      }.otherwise {
        printf("[WBQ] Not found...!?\n")
      }
    }

  }.elsewhen(io.peek.enable && io.peek.read_write === 1.U) {
    // set data out based on input
    // must be aligned
    when(!has_match) { printf(p"[WBQ] Invalid Read @${Hexadecimal(io.peek.address)}\n") }
    switch(io.peek.length) {
      is(0.U) { // b
        //origin_peek_data_out := data(matched_line)(offset)
        io.peek.data_out := data_bram_rdata_as_byte(offset)
      }
      is(1.U) { // 16b
        //origin_peek_data_out := Cat(data(matched_line)(offset + 1.U), data(matched_line)(offset))
        io.peek.data_out := Cat(data_bram_rdata_as_byte(offset + 1.U), data_bram_rdata_as_byte(offset))
      }
      is(2.U) { // 32b
        //origin_peek_data_out := Cat(
        //  data(matched_line)(offset + 3.U), data(matched_line)(offset + 2.U),
        //  data(matched_line)(offset + 1.U), data(matched_line)(offset)
        //)
        io.peek.data_out := Cat(
          data_bram_rdata_as_byte(offset + 3.U), data_bram_rdata_as_byte(offset + 2.U),
          data_bram_rdata_as_byte(offset + 1.U), data_bram_rdata_as_byte(offset)
        )

      }
      is(3.U) { // 64b
        //origin_peek_data_out := Cat(
        //  data(matched_line)(offset + 3.U), data(matched_line)(offset + 2.U),
        //  data(matched_line)(offset + 1.U), data(matched_line)(offset)
        //)
        io.peek.data_out := Cat(
          data_bram_rdata_as_byte(offset + 7.U), data_bram_rdata_as_byte(offset + 6.U),
          data_bram_rdata_as_byte(offset + 5.U), data_bram_rdata_as_byte(offset + 4.U),
          data_bram_rdata_as_byte(offset + 3.U), data_bram_rdata_as_byte(offset + 2.U),
          data_bram_rdata_as_byte(offset + 1.U), data_bram_rdata_as_byte(offset)
        )

      }
    }
  }

  //io.peek.data_out := origin_peek_data_out
  //io.dataq.deq.bits := origin_q_data_out


  data_bram.io.raddr := 0.U
  data_bram.io.waddr := 0.U
  data_bram.io.wdata := 0.U
  data_bram.io.ena := 0.U
  data_bram.io.we := 0.U
  when(io.peek.enable) {
    data_bram.io.raddr := matched_line
    data_bram.io.waddr := matched_line
    data_bram.io.wdata := wdata_peek.asUInt
    data_bram.io.ena := io.peek.read_write === 0.U && has_match
    data_bram.io.we := wdata_byte_enable_peek.asUInt
  }.otherwise {
    data_bram.io.raddr := read_data_ptr
    data_bram.io.waddr := write_data_ptr
    data_bram.io.wdata := wdata_q.asUInt
    data_bram.io.ena := io.dataq.enq.fire()
    data_bram.io.we := wdata_byte_enable_q.asUInt
  }

  when(read_data_counter_wrap) {
    read_data_ptr := read_data_ptr + 1.U
  }.elsewhen(io.q.deq.fire() && io.q.deq.bits.req_type === RequestType.PUTS.U) {
    read_data_ptr := read_data_ptr + 1.U
  }
  when(write_data_counter_wrap) {
    write_data_ptr := write_data_ptr + 1.U
  }.elsewhen(io.q.enq.fire() && io.q.enq.bits.req_type === RequestType.PUTS.U) {
    write_data_ptr := write_data_ptr + 1.U
  }
  /*
  when(io.peek.enable && io.peek.read_write === 1.U && io.peek.data_out =/= origin_peek_data_out) {
    printf(p"[ERROR] peek does not match: ${Hexadecimal(io.peek.data_out)} vs ${Hexadecimal(origin_peek_data_out)}\n")
    printf(p"[ERROR] line: ${Hexadecimal(data_bram_rdata_as_byte.asUInt)} vs ${Hexadecimal(data(matched_line).asUInt)}\n")
  }
  when(io.dataq.deq.fire() && io.dataq.deq.bits =/= origin_q_data_out) {
    printf(p"[ERROR] counter: ${read_data_counter}\n")
    printf(p"[ERROR] deq does not match: ${Hexadecimal(io.dataq.deq.bits)} vs ${Hexadecimal(origin_q_data_out)}\n")
    printf(p"[ERROR] data_bram addr: ${data_bram.io.raddr}\n")
    printf(p"[ERROR] peek enable ${io.peek.enable}\n")
    printf(p"[ERROR] line: ${Hexadecimal(data_bram_rdata_as_byte.asUInt)} vs ${Hexadecimal(data(read_data_ptr).asUInt)}\n")
  }
  */
  // for the remove logic, take more cycles but should be fine for only one core
  // clause 1. prevent read in the middle of a transaction
  // clause 2. check for invalid entry
  // clause 3. ensure that # is ok
  when(read_ptr === read_data_ptr && !valid_data(read_ptr) && count > 0.U) {
    read_ptr := read_ptr + 1.U
    read_data_ptr := read_data_ptr + 1.U
    count := count - 1.U
  }
}
