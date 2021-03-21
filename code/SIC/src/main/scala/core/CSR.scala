
package core

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import param.{CoreParam, RISCVParam}
import riscv._

class Exception(private val isaParam: RISCVParam) extends Bundle {
  val cause = UInt(isaParam.exceptionWidth.W)
  val tval = UInt(isaParam.tvalWidth.W)
  val valid = Bool()
}

object Privilege extends ChiselEnum {
  val user = Value("b00".U(2.W))
  val supervisor = Value("b01".U(2.W))
  val machine = Value("b11".U(2.W))
}

object CSROP extends ChiselEnum {
  val read, write, set, clear, nop, uret, sret, mret, wfi, exception, breakpoint = Value
}

class CSRControl(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam
  val irq = Bool()  // interrupt
  val ipi = Bool()  // inter processor interrupt
  val csrOp = CSROP()
  val addr = UInt(log2Ceil(isaParam.csrCount).W)
  val exception = new Exception(isaParam)
  val pc = UInt(isaParam.XLEN.W)
  val data = UInt(isaParam.XLEN.W)
}

class CSROutput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam
  val epc = UInt(isaParam.XLEN.W)
  val trapVectorBase = UInt(isaParam.XLEN.W)
  val rdata = UInt(isaParam.XLEN.W)
  val exception = new Exception(isaParam)
}

class CSRIO(private val coreParam: CoreParam) extends Bundle {
  val ctrl = Input(new CSRControl(coreParam))
  val out = Output(new CSROutput(coreParam))
}
class CSR(coreParam: CoreParam) extends Module {
  private val isaParam = coreParam.isaParam
  val io = IO(new CSRIO(coreParam))
  // private val reg = RegInit(VecInit(Seq.fill(isaParam.csrCount){ 0.U(isaParam.XLEN.W) }))
  private val _reg = Map(
    CSRs.mepc -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mcause -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mtval -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.sepc -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.scause -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.stval -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mtvec -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mstatus -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mscratch -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mhartid -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.misa -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mie -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mip -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.medeleg -> RegInit(0.U(isaParam.XLEN.W)),
    CSRs.mideleg -> RegInit(0.U(isaParam.XLEN.W)),
  )

  // whether the privilege check for operating on csr is ok
  val privilegeCheckPass = WireInit(true.B)
  val privilegeLevel = RegInit(Privilege.machine)
  val targetPrivilegeLevel = WireInit(Privilege.machine)
  val isWrite = WireInit(false.B)
  val write = WireInit(false.B)
  val writeData = Wire(UInt(isaParam.XLEN.W))
  val writeAddr = Wire(UInt(log2Ceil(isaParam.csrCount).W))
  val pendingInterrupt = RegInit(false.B)

  when(io.ctrl.irq && !pendingInterrupt) {
    pendingInterrupt := true.B
  }

  // originally, privilege level will keep current privilege
  privilegeLevel := targetPrivilegeLevel

  // register writing logic
  writeData := io.ctrl.data
  writeAddr := io.ctrl.addr
  switch(io.ctrl.csrOp) {
    is(CSROP.read) {}
    is(CSROP.write) {
      isWrite := true.B
      writeAddr := io.ctrl.addr
    }
    is(CSROP.clear) {
      isWrite := true.B
      writeAddr := io.ctrl.addr
      writeData := io.ctrl.data & (~io.out.rdata).asUInt
    }
    is(CSROP.set)   {
      isWrite := true.B
      writeAddr := io.ctrl.addr
      writeData := io.out.rdata | io.ctrl.data
    }
    is(CSROP.exception) {
    } // privilege level may be involved
    is(CSROP.mret) {
    }
  }
  // post condition writeData, some registers may not be written with certain bits


  write := isWrite && privilegeCheckPass
  when(write) {
    // printf(p"[CSR${coreParam.coreID}] CSROP: ${io.ctrl.csrOp.asUInt}, data: ${io.ctrl.data}\n")
    // printf(p"[CSR${coreParam.coreID}] write? ${write}, writeAddr ${Hexadecimal(writeAddr)} writeData ${Hexadecimal(writeData)}\n")
    /*
    when(writeAddr === CSRs.mstatus.U) {
      reg(writeAddr) := generateMaskedMstatusWrite(writeData)
    }.elsewhen(writeAddr === CSRs.mtvec.U) {
      reg(writeAddr) := Cat(writeData(isaParam.XLEN-1, 2), "b00".U(2.W)) // no interrupt
    }.otherwise {
      reg(writeAddr) := writeData
      reg(CSRs.mstatus.U) := generateMaskedMstatusWrite(reg(CSRs.mstatus.U))
    }
     */
    when(writeAddr === CSRs.mstatus.U) {
      _reg(CSRs.mstatus) := generateMaskedMstatusWrite(writeData)
    }.otherwise {
      _reg(CSRs.mstatus) := generateMaskedMstatusWrite(_reg(CSRs.mstatus))
    }
    when(writeAddr === CSRs.mtvec.U) {
      _reg(CSRs.mtvec) := Cat(writeData(isaParam.XLEN-1, 2), "b00".U(2.W)) // no interrupt
    }
    when(writeAddr === CSRs.mscratch.U) { _reg(CSRs.mscratch) := writeData }
    when(writeAddr === CSRs.mepc.U) { _reg(CSRs.mepc) := writeData }
    when(writeAddr === CSRs.mcause.U) { _reg(CSRs.mcause) := writeData }
    when(writeAddr === CSRs.mtval.U) { _reg(CSRs.mtval) := writeData }
    when(writeAddr === CSRs.sepc.U) { _reg(CSRs.sepc) := writeData }
    when(writeAddr === CSRs.scause.U) { _reg(CSRs.scause) := writeData }
    when(writeAddr === CSRs.stval.U) { _reg(CSRs.stval) := writeData }
    when(writeAddr === CSRs.mideleg.U) { _reg(CSRs.mideleg) := writeData }
    when(writeAddr === CSRs.medeleg.U) { _reg(CSRs.medeleg) := writeData }
  }
  generateExceptionControlTransfer(io.ctrl.exception, privilegeLevel)


  // register reading logic
  io.out.epc := _reg(CSRs.mepc)
  io.out.trapVectorBase := _reg(CSRs.mtvec)
  when(io.ctrl.exception.valid) {
    // interrupts
    val interruptOffset = WireInit(0.U(isaParam.XLEN.W))
    when(io.ctrl.exception.cause(isaParam.exceptionWidth - 1) === 1.U) {
      when(_reg(CSRs.mtvec)(1, 0) === "b01".U) {
        interruptOffset := Cat(io.ctrl.exception.cause(isaParam.exceptionWidth - 2, 0), "b00".U(2.W))
      }
    }
    io.out.trapVectorBase := Cat(_reg(CSRs.mtvec)(isaParam.XLEN - 1, 2), "b00".U(2.W)) + interruptOffset
  }
  io.out.rdata := 0.U
  for { (k, v) <- _reg } {
    when(io.ctrl.addr === k.U) { io.out.rdata := v }
  }
  // io.out.rdata := _reg(io.ctrl.addr)
  // TODO: get into the normal control flow
  io.out.exception := io.ctrl.exception

  // always set mhartid to the core id value
  // reg(CSRs.mhartid) := coreParam.coreID.U
  // reg(CSRs.misa) := Cat("b10".U(2.W), 0.U(36.W), "b00000000000000000100000001".U(26.W))
  // reg(CSRs.mie) := 0.U
  // reg(CSRs.mip) := 0.U

  _reg(CSRs.mhartid) := coreParam.coreID.U
  _reg(CSRs.misa) := Cat("b10".U(2.W), 0.U(36.W), "b00000000000000000100000001".U(26.W))
  _reg(CSRs.mie) := 0.U
  _reg(CSRs.mip) := 0.U

  // TODO: when trapped into different privilege levels, the logic should be changed
  def generateExceptionControlTransfer(e: Exception, currentPrivilegeLevel: Privilege.Type): Unit = {
    when(e.valid) {
      /*
      when(reg(CSRs.medeleg)(e.cause) === 1.U && privilegeLevel === Privilege.supervisor) { // allows delegation to supervisor mode
        reg(CSRs.sepc) := io.ctrl.pc
        reg(CSRs.scause) := io.ctrl.pc
        reg(CSRs.stval) := io.ctrl.pc
      }.otherwise { // in user mode or not delegated
        // printf(s"[CSR${coreParam.coreID}] Trying to write to mepc...cause: ${Hexadecimal(e.cause.asUInt)}, pc: ${Hexadecimal(io.ctrl.pc.asUInt)}\n")
        reg(CSRs.mepc) := io.ctrl.pc
        reg(CSRs.mcause) := e.cause
        reg(CSRs.mtval) := e.tval
        // also control the mstatus register
      } */
      when(_reg(CSRs.medeleg)(e.cause) === 1.U && privilegeLevel === Privilege.supervisor) { // allows delegation to supervisor mode
        _reg(CSRs.sepc) := io.ctrl.pc
        _reg(CSRs.scause) := io.ctrl.pc
        _reg(CSRs.stval) := io.ctrl.pc
      }.otherwise { // in user mode or not delegated
        // printf(s"[CSR${coreParam.coreID}] Trying to write to mepc...cause: ${Hexadecimal(e.cause.asUInt)}, pc: ${Hexadecimal(io.ctrl.pc.asUInt)}\n")
        _reg(CSRs.mepc) := io.ctrl.pc
        _reg(CSRs.mcause) := e.cause
        _reg(CSRs.mtval) := e.tval
        // also control the mstatus register
      }
    }
  }
  def generateMaskedMstatusWrite(v: UInt): UInt = {
    if(isaParam.XLEN == 32) {
      Cat(writeData(31),
        _reg(CSRs.mstatus)(30, 23),
        v(22, 11),
        _reg(CSRs.mstatus)(10, 9),
        v(8, 7),
        _reg(CSRs.mstatus)(6),
        v(5, 3),
        _reg(CSRs.mstatus)(2),
        v(1, 0))
    } else if(isaParam.XLEN == 64) {
      Cat(writeData(63),
        _reg(CSRs.mstatus)(62, 36),
        "b10".U, "b10".U,
        // writeData(35, 32), // SXL, UXL
        _reg(CSRs.mstatus)(31, 23),
        v(22, 11),
        _reg(CSRs.mstatus)(10, 9),
        v(8, 7),
        _reg(CSRs.mstatus)(6),
        v(5, 3),
        _reg(CSRs.mstatus)(2),
        v(1, 0))
    } else {
      0.U
    }
  }
  when(io.ctrl.exception.valid || io.ctrl.csrOp =/= CSROP.nop) {
    // printf(p"[CSR${coreParam.coreID}] mepc: ${Hexadecimal(io.out.epc)}\n")
    // printf(p"[CSR${coreParam.coreID}] mcause: ${Hexadecimal(reg(CSRs.mcause))}\n")
    // printf(p"[CSR${coreParam.coreID}] mtval: ${Hexadecimal(reg(CSRs.mtval))}\n")
    // printf(p"[CSR${coreParam.coreID}] mtvec: ${Hexadecimal(reg(CSRs.mtvec))}\n")
    // printf(p"[CSR${coreParam.coreID}] ${io.out.exception} ${io.ctrl.csrOp.asUInt}\n")
  }
}
