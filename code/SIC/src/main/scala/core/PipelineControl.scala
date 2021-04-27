
package core

import chisel3._
import chisel3.util._
import chisel3.experimental._
import riscv.Instructions

import param.CoreParam

class PipelineControlInput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam

  val control_D = new Control(coreParam)
  val control_X = new Control(coreParam)
  val control_M = new Control(coreParam)
  val control_W = new Control(coreParam)

  //val dcache_hit = Bool()
  //val icache_hit = Bool()
}

class PipelineControlOutput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam

  val fetchReq_valid = Bool()
  val decode_ready = Bool()
}

class PipelineControl(private val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val in = Input(new PipelineControlInput(coreParam))
    val out = Output(new PipelineControlOutput(coreParam))
  })

  private val has_rs1_D = hasRs1(io.in.control_D.instructionType)
  private val has_rs2_D = hasRs2(io.in.control_D.instructionType)
  private val has_rd_X = hasRd(io.in.control_X.instructionType)
  private val has_rd_M = hasRd(io.in.control_M.instructionType)
  private val has_rd_W = hasRd(io.in.control_W.instructionType)
  private val raddr1_D = io.in.control_D.raddr1
  private val raddr2_D = io.in.control_D.raddr2
  private val waddr_X = io.in.control_X.waddr
  private val waddr_M = io.in.control_M.waddr
  private val waddr_W = io.in.control_W.waddr
  private val inst_X_is_load = io.in.control_X.isMemory && (io.in.control_X.memoryRequestType === MemoryRequestType.read)
  private val inst_D_is_store = io.in.control_D.isMemory && (io.in.control_D.memoryRequestType === MemoryRequestType.write)
  private val inst_D_is_branch = io.in.control_D.controlTransferType === ControlTransferType.branch
  private val inst_X_is_branch = io.in.control_X.controlTransferType === ControlTransferType.branch

  // ophaz generation block
  private val ophaz = Wire(Bool())
  when(  // RAW dependency on rs1
    (has_rs1_D && raddr1_D =/= 0.U) &&  // decode has rs1 and rs1 is not r0
    (has_rd_W && raddr1_D === waddr_W) && // writeback inst writes to rs1
    (!(has_rd_M && raddr1_D === waddr_M)) &&  // memory inst does not write to rs1
    (!(has_rd_X && raddr1_D === waddr_X))  // execute inst does not write to rs1
  ) {
    ophaz := true.B
  } .elsewhen(  // RAW dependency on rs2
    (has_rs1_D && raddr1_D =/= 0.U) &&  // decode has rs2 and rs2 is not r0
    (has_rd_W && raddr1_D === waddr_W) && // writeback inst writes to rs2
    (!(has_rd_M && raddr1_D === waddr_M)) &&  // memory inst does not write to rs2
    (!(has_rd_X && raddr1_D === waddr_X))  // execute inst does not write to rs2
  ) {
    ophaz := true.B
  } .elsewhen(  // load-use dependency
    inst_X_is_load && (
      (has_rs1_D && raddr1_D === waddr_X) ||
      (has_rs2_D && (raddr2_D === waddr_X) && (!inst_D_is_store))
    )
  ) {
    ophaz := true.B
  }  .otherwise {
    ophaz := false.B
  }

  // output signal
  io.out.decode_ready := !ophaz  // instruction in decode state is not ready to enter execute state because of data hazard
  io.out.fetchReq_valid := (!inst_X_is_branch) && (!inst_D_is_branch)  // do not start fetching request if branch is pending

  def hasRs1(instructionType: InstructionType.Type): Bool = {
    (instructionType === InstructionType.R) || (instructionType === InstructionType.I) || (instructionType === InstructionType.S) || (instructionType === InstructionType.B)
  }

  def hasRs2(instructionType: InstructionType.Type): Bool = {
    (instructionType === InstructionType.R) || (instructionType === InstructionType.S) || (instructionType === InstructionType.B)
  }

  def hasRd(instructionType: InstructionType.Type): Bool = {
    (instructionType =/= InstructionType.S) && (instructionType =/= InstructionType.B)
  }
}
