
package components

import chisel3._
import chisel3.util._
import params.MemorySystemParams

// Used in responding data from LLC to cache
class ResponseQueueIO(private val depth: Int,
                      private val memorySystemParams: MemorySystemParams) extends Bundle {
  val q = new QueueIO(memorySystemParams.getGenMemRespCommand, depth)

  // interface for pumping data to the bus
  // the logic itself should be responsible for counting the number data beats to transfer
  // in the scenario of integration, the user is required decorate the interface with signals like last
  val dataq = DecoupledIO(UInt(memorySystemParams.busDataWidth.W))
  val dataq_enq = Input(UInt(memorySystemParams.cacheParams.lineWidth.W))
}

class ResponseQueue(val depth: Int,
                    val memorySystemParams: MemorySystemParams) extends Module {
  private val cacheParams = memorySystemParams.cacheParams
  private val genMemRespCommand = memorySystemParams.getGenMemRespCommand
  private val busDataWidth = memorySystemParams.busDataWidth
  private val beat_per_request = cacheParams.lineWidth / busDataWidth
  val io = IO(new ResponseQueueIO(depth, memorySystemParams))



  val read_ptr = RegInit(0.U(log2Ceil(depth).W))
  val write_ptr = RegInit(0.U(log2Ceil(depth).W))

  val read_data_ptr = RegInit(0.U(log2Ceil(depth).W))

  val (read_data_counter, read_data_counter_wrap) = Counter(io.dataq.fire(), beat_per_request)

  val count = RegInit(0.U(log2Ceil(depth).W))
  val command = Reg(Vec(depth, genMemRespCommand))
  val data = Module(new BRAMVerilog(depth, beat_per_request * busDataWidth))
  // Mem(Vec(beat_per_request, UInt(busDataWidth.W)), depth)
  //Reg(Vec(depth, Vec(beat_per_request, UInt(busDataWidth.W)) ))
  val valid_data = RegInit(VecInit.tabulate(depth)(_ => 0.U(1.W)))
  val empty = count === 0.U
  val full = count === depth.U
  val output_bits = command(read_ptr)

  io.q.count := count


  // Logic for queue
  when(io.q.enq.fire() && io.q.deq.fire()) {
    write_ptr := write_ptr + 1.U
    read_ptr := read_ptr + 1.U

    valid_data(write_ptr) := true.B
    valid_data(read_ptr) := false.B
  }.elsewhen(io.q.enq.fire()) {
    write_ptr := write_ptr + 1.U
    count := count + 1.U
    valid_data(write_ptr) := true.B
  }.elsewhen(io.q.deq.fire()) {
    read_ptr := read_ptr + 1.U
    count := count - 1.U
    valid_data(read_ptr) := false.B
  }

  val read_done = read_data_ptr === read_ptr
  when(io.q.enq.fire()) {
    printf(p"[RPQ] Enq ${io.q.enq.bits}, Data ${Hexadecimal(io.dataq_enq)}\n")
  }

  when(io.q.deq.fire()) {
    printf(p"[RPQ] Deq ${io.q.deq.bits}\n")
  }

  when(io.dataq.fire()) {
    printf(p"[RPQ] Deq (Data Channel) (${Hexadecimal(io.dataq.bits)})\n")
  }

  // when(io.q.deq.valid) {
  //   printf("[RPQ] Valid Response, Check Data Channel: %b %b rd_data_ptr %d rd_ptr %d\n", io.dataq.valid, io.dataq.ready, read_data_ptr, read_ptr)
  // }
  /*
  val ctr = RegInit(0.U(32.W))
  when(io.q.deq.valid) {
    ctr := ctr + 1.U
  }.otherwise {
    // resetting
    ctr := 0.U
  }
  when(ctr > (256.U)) {
    // takes too long to transmit the data
    printf("[RPQ] io.deq.valid: %b, io.deq.ready: %b\n", io.q.deq.valid, io.q.deq.ready)
    assert(false.B, "latency too long to send response")
  } */

  when(io.q.enq.fire() && !read_done) {
    assert(false.B, "Cannot satisfy two response at once!!!")
  }



  io.q.enq.ready := !full
  io.q.deq.valid := !empty // && read_done
  io.q.deq.bits := output_bits

  io.dataq.valid := !read_done
  io.dataq.bits := data.io.rdata.asTypeOf(Vec(beat_per_request, UInt(busDataWidth.W)))(read_data_counter)
  //data.read(read_data_ptr)(read_data_counter)

  data.io.raddr := read_data_ptr
  data.io.clock := clock
  data.io.reset := reset

  data.io.we := 0.U
  data.io.waddr := write_ptr
  data.io.wdata := io.dataq_enq
  data.io.we := io.q.enq.fire()
  when(io.q.enq.fire()) {
    command(write_ptr) := io.q.enq.bits
    //data.write(write_ptr,
    // Vec.tabulate(beat_per_request)(i => io.dataq_enq((i + 1) * busDataWidth - 1, i * busDataWidth)))
    printf(p"[RPQ] Enq Data ${Hexadecimal(io.dataq_enq)}\n")
  }


  when(read_data_counter_wrap) { read_data_ptr := read_data_ptr + 1.U }

}
