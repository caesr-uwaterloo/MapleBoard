
package components

import chisel3._
import chisel3.util._
import params.{CoherenceSpec, MemorySystemParams, SimpleCacheParams}

class InputArbitrationPipe[S <: Data, M <: Data, B <: Data](memorySystemParams: MemorySystemParams,
                                                 cohSpec: CoherenceSpec[S, M, B]) extends Module {
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
  val genControllerResp: MemRespCommand = memorySystemParams.getGenMemRespCommand
  val genSnoopReq: SnoopReq = memorySystemParams.getGenSnoopReq
  val genSnoopResp: SnoopResp = memorySystemParams.getGenSnoopResp
  val genDebugCacheLine: DebugCacheline = memorySystemParams.getGenDebugCacheline

  val nrtCores = 1
  val io = IO(new Bundle {
    val replay_request_channel = Flipped(Decoupled(genCacheReq))
    val core_request_channel = Flipped(Decoupled(genCacheReq))
    val bus_response_channel = Flipped(Decoupled(genControllerResp))
    val snoop_request_channel = Flipped(Decoupled(genSnoopReq))
    /*
    val dedicated_wb_fire = Input(Bool())
    val dedicated_address = Input(memorySystemParams.cacheParams.genAddress)
     */
    val bus_dedicated_request_channel = Flipped(Decoupled(genControllerReqCommand))

    val pipe = Decoupled(new PipeData(memorySystemParams, cohSpec))
    val mshrVacant = Input(Bool())
    val pendingMemVacant = Input(Bool())
    val busy = Input(Bool())

    val readTag = Output(Bool())
    val readData = Output(Bool())
    val readSet = Output(UIntHolding(memorySystemParams.cacheParams.nSets))

    val readBufferDone = Input(Bool())
    val readBuffer = Input(cacheParams.genCacheLine)
    val readBufferRead = Output(Bool())
    val id = Input(UIntHolding(memorySystemParams.masterCount + 1))
    val time = Input(UIntHolding(128))

    val readingPWBs = Input(Bool())
  })
  io.pipe.bits := DontCare
  io.pipe.bits.core := Mux(
    io.replay_request_channel.valid,
    io.replay_request_channel.bits,
    io.core_request_channel.bits)
  io.pipe.bits.mem := io.bus_response_channel.bits
  io.pipe.bits.snoop := io.snoop_request_channel.bits
  io.pipe.bits.isDedicatedWB := false.B

  // we don't really care about other fields
  // might infer latch...

  io.pipe.bits.data := io.readBuffer.asTypeOf(cacheParams.genCacheLineBytes)

  io.replay_request_channel.ready := false.B
  io.core_request_channel.ready := false.B
  io.bus_response_channel.ready := false.B
  io.snoop_request_channel.ready := false.B
  io.bus_dedicated_request_channel.ready := false.B
  io.pipe.valid := false.B
  val address = Wire(UInt(memorySystemParams.cacheParams.addrWidth.W))
  address := 0.U
  when(!io.busy) {
    io.pipe.bits.src := ESource.core
    when(io.replay_request_channel.valid) {
      io.replay_request_channel.ready := io.pipe.ready
      io.pipe.valid := io.replay_request_channel.valid
      io.pipe.bits.src := ESource.core
      address := io.replay_request_channel.bits.address
    }.elsewhen(io.bus_dedicated_request_channel.valid) {
      io.pipe.bits.src := ESource.snoop
      // address := io.dedicated_address
      io.bus_dedicated_request_channel.ready := io.pipe.ready
      address := io.bus_dedicated_request_channel.bits.address
      io.pipe.valid := io.bus_dedicated_request_channel.valid
      io.pipe.bits.isDedicatedWB := true.B
    }.elsewhen(io.bus_response_channel.valid) {
      // printf(p"Core knows here is a response pipe ready: ${io.pipe.ready}, read buffer done ${io.readBufferDone}...\n")
      when(io.bus_response_channel.valid && io.bus_response_channel.bits.ack === 0.U) {
        io.bus_response_channel.ready := io.pipe.ready && io.readBufferDone
        io.pipe.valid := io.bus_response_channel.valid && io.readBufferDone
      }.otherwise {
        io.bus_response_channel.ready := io.pipe.ready
        io.pipe.valid := io.bus_response_channel.valid
      }
      io.pipe.bits.src := ESource.mem
      address := io.bus_response_channel.bits.address
    }.elsewhen(io.snoop_request_channel.valid) {
      io.snoop_request_channel.ready := io.pipe.ready
      io.pipe.valid := io.snoop_request_channel.valid
      io.pipe.bits.src := ESource.snoop
      address := io.snoop_request_channel.bits.address
    }.elsewhen(io.core_request_channel.valid && !io.readingPWBs) {
      io.core_request_channel.ready := io.pipe.ready && io.pendingMemVacant && io.mshrVacant
      io.pipe.valid := io.core_request_channel.valid && io.pendingMemVacant && io.mshrVacant
      io.pipe.bits.src := ESource.core
      address := io.core_request_channel.bits.address
    }
    io.pipe.bits.address := address
  }
  // only read when it is from memory
  io.readBufferRead := io.pipe.fire() && io.pipe.bits.src === ESource.mem && io.pipe.bits.mem.ack === 0.U
  io.readSet := 0.U
  io.readData := false.B
  io.readTag := false.B
  when(io.pipe.fire()) {
    io.readSet := memorySystemParams.cacheParams.getLineAddress(address)
    io.readData := true.B
    io.readTag := true.B
  }

  // ===== Stats =====
  when(io.pipe.valid) {
    printf("=== [CC%x.InputArb] @%d (busy: %d) ===\n", io.id, io.time, io.busy)
    when(io.replay_request_channel.fire()) {
      printf("Replay Buffer: ")
      utils.printbundle(io.replay_request_channel.bits)
      printf("\n")
    }.elsewhen(io.bus_dedicated_request_channel.valid) {
      printf(p"Dedicated Writeback: ${io.bus_dedicated_request_channel.bits.address}\n")
    }.elsewhen(io.bus_response_channel.fire()) {
      printf("Bus Response: ")
      printf(">>> Mem Response Available !\n")
      utils.printbundle(io.bus_response_channel.bits)
      printf("\n")
    }.elsewhen(io.snoop_request_channel.fire()) {
      printf("Snoop Request: ")
      utils.printbundle(io.snoop_request_channel.bits)
      printf("\n")
    }.elsewhen(io.core_request_channel.fire()) {
      printf("Core Request: ")
      utils.printbundle(io.core_request_channel.bits)
      printf("\n")
    }
  }
}
