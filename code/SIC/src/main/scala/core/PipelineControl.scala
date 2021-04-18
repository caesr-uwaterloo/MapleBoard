
package core

import chisel3._
import chisel3.util._

import param.CoreParam

class PipelineControlInput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam

  val has_rs1_D = Bool()
  val has_rs2_D = Bool()
  val has_rd_E = Bool()
  val has_rd_M = Bool()
  val has_rd_W = Bool()

  val raddr1_D = UInt(log2Ceil(isaParam.registerCount).W)
  val raddr2_D = UInt(log2Ceil(isaParam.registerCount).W)
  val waddr_E = UInt(log2Ceil(isaParam.registerCount).W)
  val waddr_M = UInt(log2Ceil(isaParam.registerCount).W)
  val waddr_W = UInt(log2Ceil(isaParam.registerCount).W)
  val inst_E_is_load = Bool()
  val inst_D_is_store = Bool()
  val inst_D_is_branch = Bool()
  val inst_E_is_branch = Bool()
}

class PipelineControlOutput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam

  val stall_FD = Bool()
}

class PipelineControl(private val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val in = Input(new PipelineControlInput(coreParam))
    val out = Output(new PipelineControlOutput(coreParam))
  })

  when(  // RAW dependency on rs1
    (io.in.has_rs1_D && io.in.raddr1_D =/= 0.U) &&  // decode has rs1 and rs1 is not r0
    (io.in.has_rd_W && io.in.raddr1_D === io.in.waddr_W) && // writeback inst writes to rs1
    (!(io.in.has_rd_M && io.in.raddr1_D === io.in.waddr_M)) &&  // memory inst does not write to rs1
    (!(io.in.has_rd_E && io.in.raddr1_D === io.in.waddr_E))  // execute inst does not write to rs1
  ) {
    io.out.stall_FD := true.B  // stall inst in fetch
  } .elsewhen(  // RAW dependency on rs2
    (io.in.has_rs1_D && io.in.raddr1_D =/= 0.U) &&  // decode has rs2 and rs2 is not r0
    (io.in.has_rd_W && io.in.raddr1_D === io.in.waddr_W) && // writeback inst writes to rs2
    (!(io.in.has_rd_M && io.in.raddr1_D === io.in.waddr_M)) &&  // memory inst does not write to rs2
    (!(io.in.has_rd_E && io.in.raddr1_D === io.in.waddr_E))  // execute inst does not write to rs2
  ) {
    io.out.stall_FD := true.B  // stall inst in fetch
  } .elsewhen(  // load-use dependency
    io.in.inst_E_is_load && (
      (io.in.has_rs1_D && io.in.raddr1_D === io.in.waddr_E) ||
      (io.in.has_rs2_D && (io.in.raddr2_D === io.in.waddr_E) && (!io.in.inst_D_is_store))
    )
  ) {
    io.out.stall_FD := true.B
  } .elsewhen(io.in.inst_D_is_branch || io.in.inst_E_is_branch) {  // branch resolution
    io.out.stall_FD := true.B
  } .otherwise {
    io.out.stall_FD := false.B
  }
}

