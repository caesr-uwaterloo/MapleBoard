
package core

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, log2Ceil}
import param.CoreParam

object In1Sel extends ChiselEnum {
  val pc, reg = Value
}
object In2Sel extends ChiselEnum {
  val reg, pcPlus4, imm = Value
}

object CSRSourceSel extends ChiselEnum {
  val reg, uimm = Value
}

object MemoryRequestType extends ChiselEnum {
  val read, write, amo, lr, sc = Value
}

class DataPath(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam

  // immediate
  val imm = UInt(isaParam.XLEN.W)

  // csr data
  val csrWriteData = UInt(isaParam.XLEN.W)

  val pcPlus4 = UInt(isaParam.XLEN.W)
  val pc = UInt(isaParam.XLEN.W)

  // register data read output
  val regData1 = UInt(isaParam.XLEN.W)
  val regData2 = UInt(isaParam.XLEN.W)

  // alu output
  val aluData = UInt(isaParam.XLEN.W)

  // memory write data
  val memoryData = UInt(isaParam.XLEN.W)
  val memoryAddress = UInt(isaParam.XLEN.W)

  // branch target
  val branchTarget = UInt(isaParam.XLEN.W)
}

// The structure is the control signal of different component
class Control(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam

  // alu operation
  val isW = Bool()
  val aluop = ALUOP()

  // register read
  val raddr1 = UInt(log2Ceil(isaParam.registerCount).W)
  val raddr2 = UInt(log2Ceil(isaParam.registerCount).W)
  val waddr = UInt(log2Ceil(isaParam.registerCount).W)
  val wen = Bool()

  // branch target
  val controlTransferType = ControlTransferType()

  // memory request
  val isMemory = Bool()
  val memoryRequestType = MemoryRequestType()
  val isAMO = Bool()
  val amoOp = AMOOP()
  // require(requirement = false, "TODO: check AMO width")
  // memory consistency annotations
  val acquire = Bool()
  val release = Bool()
  val length = UInt(3.W)

  // ALU Op Sel
  val in1Sel = In1Sel()
  val in2Sel = In2Sel()

  // CSR operation
  val csrOp = CSROP()
  // require(requirement = false, "TODO: check CSR Op")
  val csrAddress = UInt(log2Ceil(isaParam.csrCount).W)
  val csrSource = CSRSourceSel()

  // exception
  val exception = new Exception(isaParam)

  // branches
  val branchTaken = Bool()
  val branchType = BranchType()
}


class StageInterfaceIO(private val coreParam: CoreParam) extends Bundle {
  val control = Decoupled(new Control(coreParam))
  val data = Decoupled(new DataPath(coreParam))
}
object StageInterface {
  def apply(coreParam: CoreParam): StageInterface = new StageInterface(coreParam)
}
/**
  *  Two-stage pipelined stage interface, only clock the data when input is valid and next stage is ready
  */
class StageInterface(private val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new StageInterfaceIO(coreParam))
    val out = new StageInterfaceIO(coreParam)
  })
  val control = Reg(new Control(coreParam))
  val data = Reg(new DataPath(coreParam))
  // we have two decoupled interfaces in StageInterfaceIO but only using one of the ready/valid pairs
  // TODO: combine the two decoupled interfaces into one
  when (io.in.control.valid && io.out.control.ready) {
    control := io.in.control
    data := io.in.data
  }

  io.out.control := control
  io.out.data := data
}
