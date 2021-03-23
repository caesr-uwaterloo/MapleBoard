
package core

import chisel3._
import chisel3.dontTouch
import chisel3.util._
import param.{CoreParam, RISCVParam}

class CoreIO(private val coreParam: CoreParam) extends Bundle {
  private val genICacheReq = new ICacheReq(coreParam)
  private val genICacheResp = new ICacheResp(coreParam)
  private val genDCacheReq = new DCacheReq(coreParam)
  private val genDCacheResp = new DCacheResp(coreParam)
  val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
  val irq = Input(Bool())
  val iCacheReq = Decoupled(genICacheReq)
  val iCacheResp = Flipped(Decoupled(genICacheResp))
  val dCacheReq = Decoupled(genDCacheReq)
  val dCacheResp = Flipped(Decoupled(genDCacheResp))
}

/**
  * Data path and control path of the core
  * Note that stage interfaces may be replaced with registers for pipelining and insert forwarding logic
  */
class Core(coreParam: CoreParam) extends Module {
  private val isaParam = coreParam.isaParam
  val io = IO(new CoreIO(coreParam))

  private val fetch = Module(new Fetch(coreParam))
  private val decode = Module(new Decode(coreParam))
  private val dxInterface = Module(StageInterface(coreParam))
  private val execute = Module(new Execute(coreParam))
  private val xmInterface = Module(StageInterface(coreParam))
  private val memory = Module(new Memory(coreParam))
  private val mwInterface = Module(StageInterface(coreParam))
  private val writeBack = Module(new WriteBack(coreParam))
  private val registerFile = Module(new RegisterFile(isaParam))
  private val csr = Module(new CSR(coreParam))

  // Wires
  private val pcPlus4 = fetch.io.fetchResp.bits.pc
  private val jumpTarget = execute.io.dataOutput.bits.branchTarget
  private val branchTarget = execute.io.dataOutput.bits.branchTarget

  // Fetch
  fetch.io.iCacheReq <> io.iCacheReq
  fetch.io.iCacheResp <> io.iCacheResp
  fetch.io.initPC := io.initPC
  fetch.io.pcPlus4 := pcPlus4
  fetch.io.jumpTarget := jumpTarget
  fetch.io.branchTarget := branchTarget
  fetch.io.trapVectorBase := csr.io.out.trapVectorBase
  fetch.io.eret := csr.io.out.epc
  // TODO: when pipeling, the fetchReqeust signal part needs changes
  fetch.io.fetchReq.valid := writeBack.io.controlOutput.valid
  fetch.io.fetchReq.bits.fetchFrom := NextPCSel.pcPlus4
  // priority from high to low:
  when(csr.io.out.exception.valid) {
    fetch.io.fetchReq.bits.fetchFrom := NextPCSel.trapVectorBase
  }.elsewhen(writeBack.io.controlOutput.bits.controlTransferType === ControlTransferType.eret) {
    fetch.io.fetchReq.bits.fetchFrom := NextPCSel.eret
  }.elsewhen(execute.io.controlOutput.bits.controlTransferType === ControlTransferType.branch
    && execute.io.controlOutput.bits.branchTaken) {
    fetch.io.fetchReq.bits.fetchFrom := NextPCSel.branchTarget
  }.elsewhen(execute.io.controlOutput.bits.controlTransferType === ControlTransferType.jump ||
    execute.io.controlOutput.bits.controlTransferType === ControlTransferType.jumpr) {
    fetch.io.fetchReq.bits.fetchFrom := NextPCSel.jumpTarget
  }

  fetch.io.fetchResp <> decode.io.fetchResp

  // Decode
  // register file
  registerFile.io.raddr1 := decode.io.control.bits.raddr1
  registerFile.io.raddr2 := decode.io.control.bits.raddr2
  decode.io.control <> dxInterface.io.in.control
  decode.io.data <> dxInterface.io.in.data

  // Execute
  dxInterface.io.in.data.bits.regData1 := registerFile.io.rdata1
  dxInterface.io.in.data.bits.regData2 := registerFile.io.rdata2
  execute.io.controlInput <> dxInterface.io.out.control
  execute.io.dataInput <> dxInterface.io.out.data

  execute.io.controlOutput <> xmInterface.io.in.control
  execute.io.dataOutput <> xmInterface.io.in.data

  // Memory
  memory.io.controlInput <> xmInterface.io.out.control
  memory.io.dataInput <> xmInterface.io.out.data
  memory.io.controlOutput <> mwInterface.io.in.control
  memory.io.dataOutput <> mwInterface.io.in.data
  memory.io.dCacheReq <> io.dCacheReq
  memory.io.dCacheResp <> io.dCacheResp

  // WriteBack
  writeBack.io.controlInput <> mwInterface.io.out.control
  writeBack.io.dataInput <> mwInterface.io.out.data

  registerFile.io.wen := writeBack.io.controlOutput.bits.wen && writeBack.io.controlOutput.valid && !csr.io.out.exception.valid
  registerFile.io.waddr := writeBack.io.controlOutput.bits.waddr
  when(writeBack.io.controlOutput.bits.isMemory) {
    registerFile.io.wdata := writeBack.io.dataOutput.bits.memoryData
  }.elsewhen(writeBack.io.controlOutput.bits.csrOp =/= CSROP.nop) {
    registerFile.io.wdata := csr.io.out.rdata
  }.otherwise {
    registerFile.io.wdata := writeBack.io.dataOutput.bits.aluData
  }
  csr.io.ctrl.addr := writeBack.io.controlOutput.bits.csrAddress
  csr.io.ctrl.pc := writeBack.io.dataOutput.bits.pc
  when(writeBack.io.controlOutput.valid) {
    csr.io.ctrl.csrOp := writeBack.io.controlOutput.bits.csrOp
  }.otherwise {
    csr.io.ctrl.csrOp := CSROP.nop
  }
  csr.io.ctrl.data := writeBack.io.dataOutput.bits.csrWriteData
  csr.io.ctrl.irq := io.irq
  csr.io.ctrl.ipi := false.B
  writeBack.io.controlOutput.ready := fetch.io.fetchReq.ready
  writeBack.io.dataOutput.ready := fetch.io.fetchReq.ready
  csr.io.ctrl.exception := writeBack.io.controlOutput.bits.exception

  /** todo why optimized out? **/
  val irq = dontTouch(io.irq)
  val icacherespready = dontTouch(io.iCacheResp.ready)
  val dcacherespready = dontTouch(io.dCacheResp.ready)

  when(writeBack.io.controlOutput.valid) {
    printf(p"[W${coreParam.coreID}] wen ${registerFile.io.wen}, reg_addr ${registerFile.io.waddr}, reg_data ${Hexadecimal(registerFile.io.wdata)}\n")
  }
}
