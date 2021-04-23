
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

  // for stalling control
  val has_rs1 = Bool()
  val has_rs2 = Bool()
  val has_rd = Bool()

  def nop(): Control = {
    val control = new Control(coreParam)
    control.isW := false.B
    control.aluop := ALUOP.undef
    control.raddr1 := 0.U
    control.raddr2 := 0.U
    control.waddr := 0.U
    control.wen := 0.U
    control.controlTransferType := ControlTransferType.none
    control.isMemory := false.B
    control.memoryRequestType := MemoryRequestType.read
    control.isAMO := false.B
    control.amoOp := AMOOP.none
    control.acquire := false.B
    control.release := false.B
    control.length := 0.U
    control.in1Sel := In1Sel.reg
    control.in2Sel := In2Sel.reg
    control.csrOp := CSROP.nop
    control.csrAddress := 0.U
    control.csrSource := CSRSourceSel.reg
    control.exception.valid := false.B
    control.branchTaken := false.B
    control.branchType := BranchType.none
    control.has_rs1 := false.B
    control.has_rs2 := false.B
    control.has_rd := false.B
    control
  }
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
  val valid = Reg(Bool())
  // we have two decoupled interfaces in StageInterfaceIO but only using one of the ready/valid pairs
  // TODO: combine the two decoupled interfaces into one
  when (io.in.control.valid && io.out.control.ready) {
    control := io.in.control.bits
    data := io.in.data.bits
  } .otherwise {
    control := control.nop()
    data := io.in.data.bits
  }
  valid := io.in.control.valid && io.out.control.ready

  io.out.control.bits := control
  io.out.data.bits := data
  io.out.control.valid := valid
  io.out.data.valid := valid
}

/**
  *  DX stage interface with bypass
  */
class DXStageInterface(private val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new StageInterfaceIO(coreParam))
    val out = new StageInterfaceIO(coreParam)
    val regData1_bypass = Input(UInt(coreParam.isaParam.XLEN.W))
    val regData2_bypass = Input(UInt(coreParam.isaParam.XLEN.W))
  })
  val control = Reg(new Control(coreParam))
  val data = Reg(new DataPath(coreParam))
  val valid = Reg(Bool())
  // we have two decoupled interfaces in StageInterfaceIO but only using one of the ready/valid pairs
  // TODO: combine the two decoupled interfaces into one
  when (io.in.control.valid && io.out.control.ready) {
    control := io.in.control.bits
    data := io.in.data.bits
  } .otherwise {
    control := control.nop()
    data := io.in.data.bits
  }
  valid := io.in.control.valid && io.out.control.ready

  io.out.control.bits := control
  io.out.data.bits := data

  // reconnect bypass connection
  io.out.data.bits.regData1 := io.regData1_bypass
  io.out.data.bits.regData2 := io.regData2_bypass

  io.out.control.valid := valid
  io.out.data.valid := valid
}

/**
  *  XM stage interface with bypass
  */
class XMStageInterface(private val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(new StageInterfaceIO(coreParam))
    val out = new StageInterfaceIO(coreParam)
    val memData_bypass = Input(UInt(coreParam.isaParam.XLEN.W))
  })
  val control = Reg(new Control(coreParam))
  val data = Reg(new DataPath(coreParam))
  val valid = Reg(Bool())
  // we have two decoupled interfaces in StageInterfaceIO but only using one of the ready/valid pairs
  // TODO: combine the two decoupled interfaces into one
  when (io.in.control.valid && io.out.control.ready) {
    control := io.in.control.bits
    data := io.in.data.bits
  } .otherwise {
    control := control.nop()
    data := io.in.data.bits
  }
  valid := io.in.control.valid && io.out.control.ready

  io.out.control.bits := control
  io.out.data.bits := data

  // reconnect bypass connection
  io.out.data.bits.memoryData := io.memData_bypass

  io.out.control.valid := valid
  io.out.data.valid := valid
}