
package core

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import param.CoreParam
import riscv.Causes

class DCacheReq(private val coreParam: CoreParam) extends Bundle {
  val address: UInt = UInt(coreParam.isaParam.XLEN.W)
  val length: UInt = UInt(2.W)
  val data: UInt = UInt(coreParam.isaParam.XLEN.W)
  val memoryType = MemoryRequestType()
  val isAMO: Bool = Bool()
  val amoOP = AMOOP()
  override def toPrintable: Printable = {
    p"DCacheReq(address=0x${Hexadecimal(address)}, " +
      p"length=${Hexadecimal(length)}, data=0x${Hexadecimal(data)}, " +
      p"memType=${memoryType.asUInt}, isAMO=${isAMO}, amoOP=${amoOP.asUInt})"
  }
}


class DCacheResp(private val coreParam: CoreParam) extends Bundle {
  val address: UInt = UInt(coreParam.isaParam.XLEN.W)
  val data: UInt = UInt(coreParam.isaParam.XLEN.W)
  override def toPrintable: Printable = {
    p"DCacheResp(address=0x${Hexadecimal(address)}, data=0x${Hexadecimal(data)})"
  }
}

class MemoryIO(private val coreParam: CoreParam) extends Bundle {
  private val genDCacheReq = new DCacheReq(coreParam)
  private val genDCacheResp = new DCacheResp(coreParam)

  val controlInput = Flipped(Decoupled(new Control(coreParam)))
  val dataInput = Flipped(Decoupled(new DataPath(coreParam)))

  val controlOutput = Decoupled(new Control(coreParam))
  val dataOutput = Decoupled(new DataPath(coreParam))

  val dCacheReq = Decoupled(genDCacheReq)
  val dCacheResp = Flipped(Decoupled(genDCacheResp))
}

object MemoryStageState extends ChiselEnum {
  val idle, waiting = Value
}

class Memory(coreParam: CoreParam) extends Module {
  private val genDCacheReq = new DCacheReq(coreParam)
  private val genDCacheResp = new DCacheResp(coreParam)
  val io = IO(new MemoryIO(coreParam))
  io.controlInput <> io.controlOutput
  io.dataInput <> io.dataOutput
  val state = RegInit(MemoryStageState.idle)
  val reqQ = Module(new Queue(genDCacheReq, entries = 1))
  val respQ = Module(new Queue(genDCacheResp, entries = 1))
  val misaligned = WireInit(false.B)

  when(io.controlInput.bits.isMemory && io.controlInput.valid) {
    switch(io.controlInput.bits.length(1, 0)) {
      is("b00".U) { misaligned := false.B }
      is("b01".U) { misaligned := io.dataInput.bits.memoryAddress(0) =/= 0.U }
      is("b10".U) { misaligned := io.dataInput.bits.memoryAddress(1, 0) =/= 0.U }
      is("b11".U) { misaligned := io.dataInput.bits.memoryAddress(2, 0) =/= 0.U }
    }
  }

  reqQ.io.enq.valid := state === MemoryStageState.idle && io.controlInput.bits.isMemory && io.controlInput.valid && !misaligned
  reqQ.io.enq.bits.address := io.dataInput.bits.memoryAddress
  reqQ.io.enq.bits.data := io.dataInput.bits.memoryData
  reqQ.io.enq.bits.length := io.controlInput.bits.length(1, 0)  // explicitly set the length to the 2 LSB
  reqQ.io.enq.bits.amoOP := io.controlInput.bits.amoOp
  reqQ.io.enq.bits.isAMO := io.controlInput.bits.isAMO
  reqQ.io.enq.bits.memoryType := io.controlInput.bits.memoryRequestType
  // TODO: make this logic simpler
  io.controlInput.ready := io.controlInput.valid &&
    (state === MemoryStageState.waiting && respQ.io.deq.fire() ||
      (misaligned || !io.controlInput.bits.isMemory) && io.controlOutput.ready)
  io.dataInput.ready := state === MemoryStageState.idle && reqQ.io.enq.ready
  io.dCacheReq <> reqQ.io.deq

  io.controlOutput.valid := io.controlInput.valid && (respQ.io.deq.valid || !io.controlInput.bits.isMemory || misaligned)
  io.dataOutput.valid := respQ.io.deq.valid
  //
  io.dataOutput.bits.memoryData := 0.U /* respQ.io.deq.bits.data */
  switch(io.controlInput.bits.length) {
    // LB
    is("b000".U) { io.dataOutput.bits.memoryData := signExt(respQ.io.deq.bits.data(7, 0)) }
    // LH
    is("b001".U) { io.dataOutput.bits.memoryData := signExt(respQ.io.deq.bits.data(15, 0)) }
    // LW
    is("b010".U) { io.dataOutput.bits.memoryData := signExt(respQ.io.deq.bits.data(31, 0)) }
    // LBU
    is("b100".U) { io.dataOutput.bits.memoryData := zeroExt(respQ.io.deq.bits.data(7, 0)) }
    // LHU
    is("b101".U) { io.dataOutput.bits.memoryData := zeroExt(respQ.io.deq.bits.data(15, 0)) }
    // LWU
    is("b110".U) { io.dataOutput.bits.memoryData := zeroExt(respQ.io.deq.bits.data(31, 0)) }
    // LD
    is("b011".U) {
      if (coreParam.isaParam.XLEN >= 64) {
        io.dataOutput.bits.memoryData := respQ.io.deq.bits.data(63, 0)
      }
    }
  }
  // AMO W is always sign extended
  when(io.controlInput.bits.isAMO && io.controlInput.bits.length(1,0) === "b10".U) {
    io.dataOutput.bits.memoryData := signExt(respQ.io.deq.bits.data(31, 0))
  }
  //
  respQ.io.deq.ready := io.controlOutput.ready
  respQ.io.enq <> io.dCacheResp

  when(reqQ.io.enq.fire()) {
    state := MemoryStageState.waiting
  }.elsewhen(respQ.io.deq.fire()) {
    state := MemoryStageState.idle
  }

  when(misaligned && !io.controlInput.bits.exception.valid) {
    io.controlOutput.bits.exception.valid := true.B
    when(io.controlInput.bits.memoryRequestType === MemoryRequestType.read) {
      io.controlOutput.bits.exception.cause := Causes.misaligned_load.U
    }.otherwise {
      io.controlOutput.bits.exception.cause := Causes.misaligned_store.U
    }
    io.controlOutput.bits.exception.tval := io.dataInput.bits.memoryAddress
  }

  // printf(p"[M] controlInput.valid: ${io.controlInput.valid} controlInput.ready: ${io.controlInput.ready}\n")
  // printf(p"[M] controlOutput.valid: ${io.controlOutput.valid} controlOutput.ready: ${io.controlOutput.ready}\n")
  when(io.dCacheReq.fire()) {
    printf(p"[M${coreParam.coreID}] dCacheReq: ${io.dCacheReq.bits}\n")
  }
  when(io.dCacheResp.fire()) {
    printf(p"[M${coreParam.coreID}] dCacheResp: ${io.dCacheResp.bits} ready: ${io.dCacheResp.ready} \n")
  }
  when(io.controlInput.valid && io.controlInput.ready) {
    printf(p"[M${coreParam.coreID}] controlInputValid: ${io.controlInput.valid} controlInputReady: ${io.controlInput.ready}\n")
    printf(p"[M${coreParam.coreID}] exception: pc: ${Hexadecimal(io.dataOutput.bits.pc)} valid ${io.controlOutput.bits.exception.valid} cause: ${Hexadecimal(io.controlOutput.bits.exception.cause)} tval: ${Hexadecimal(io.controlOutput.bits.exception.tval)}\n")
    printf(p"[M${coreParam.coreID}] isMemory? ${io.controlInput.bits.isMemory} state: ${state.asUInt}, controloutput valid? ${io.controlOutput.valid}\n")
  }
  def signExt(imm: UInt): UInt = {
    val width = imm.getWidth
    val sign = imm(width - 1)
    val extension = Fill(coreParam.isaParam.XLEN - width, sign)
    Cat(extension, imm)
  }
  def zeroExt(imm: UInt): UInt = {
    val width = imm.getWidth
    val sign = 0.U(1.W)
    val extension = Fill(coreParam.isaParam.XLEN - width, sign)
    Cat(extension, imm)
  }
}
