
package core

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import param.CoreParam

object Rs1BypassSel extends ChiselEnum {
  val nobypass, mx, wx = Value
}
object Rs2BypassSel extends ChiselEnum {
  val nobypass, mx, wx = Value
}

object MemBypassSel extends ChiselEnum {
  val nobypass, wm = Value
}

class BypassControl extends Bundle {
  val rs1BypassSel = Rs1BypassSel()
  val rs2BypassSel = Rs2BypassSel()
  val memBypassSel = MemBypassSel()
}

class BypassInput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam
  // 1. Input Data (source of bypass)
  // register data read output in execute stage
  val regData1 = UInt(isaParam.XLEN.W)
  val regData2 = UInt(isaParam.XLEN.W)
  // alu output in memory stage
  val aluData = UInt(isaParam.XLEN.W)
  // write data in write stage
  val writeData = UInt(isaParam.XLEN.W)

  // 2. Control
  val raddr1_E = UInt(log2Ceil(isaParam.registerCount).W)
  val raddr2_E = UInt(log2Ceil(isaParam.registerCount).W)
  val waddr_M = UInt(log2Ceil(isaParam.registerCount).W)
  val wen_M = Bool()  // wen in memory stage
  val waddr_W = UInt(log2Ceil(isaParam.registerCount).W)
  val wen_W = Bool()  // wen in write stage
  val raddr2_M = UInt(log2Ceil(isaParam.registerCount).W)
}

class BypassOutput(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam
  // execute stage
  val regData1 = UInt(isaParam.XLEN.W)
  val regData2 = UInt(isaParam.XLEN.W)
  // memory stage
  val memBypassSel = MemBypassSel()
}

class Bypass(private val coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val in = Input(new BypassInput(coreParam))
    val out = Output(new BypassOutput(coreParam))
  })

  private val bypassControl = Wire(new BypassControl())

  // Datapath
  io.out.regData1 := 0.U
  io.out.regData2 := 0.U

  switch(bypassControl.rs1BypassSel) {
    is(Rs1BypassSel.nobypass) { io.out.regData1 := io.in.regData1 }
    is(Rs1BypassSel.mx) {  io.out.regData1 := io.in.aluData }
    is(Rs1BypassSel.wx) { io.out.regData1 := io.in.writeData }
  }

  switch(bypassControl.rs2BypassSel) {
    is(Rs2BypassSel.nobypass) { io.out.regData2 := io.in.regData2 }
    is(Rs2BypassSel.mx) {  io.out.regData2 := io.in.aluData }
    is(Rs2BypassSel.wx) { io.out.regData2 := io.in.writeData }
  }

  // Control logic
  when(io.in.raddr1_E =/= 0.U ) {
    // mx bypass has the highest priority
    when(io.in.raddr1_E === io.in.waddr_M && io.in.wen_M === true.B) {
      bypassControl.rs1BypassSel := Rs1BypassSel.mx
    } .elsewhen(io.in.raddr1_E === io.in.waddr_W && io.in.wen_W === true.B) {
      bypassControl.rs1BypassSel := Rs1BypassSel.wx
    } .otherwise {
      bypassControl.rs1BypassSel := Rs1BypassSel.nobypass
    }
  } .otherwise {
    bypassControl.rs1BypassSel := Rs1BypassSel.nobypass
  }

  when(io.in.raddr2_E =/= 0.U ) {
    // mx bypass has the highest priority
    when(io.in.raddr2_E === io.in.waddr_M && io.in.wen_M === true.B) {
      bypassControl.rs2BypassSel := Rs2BypassSel.mx
    } .elsewhen(io.in.raddr2_E === io.in.waddr_W && io.in.wen_W === true.B) {
      bypassControl.rs2BypassSel := Rs2BypassSel.wx
    } .otherwise {
      bypassControl.rs2BypassSel := Rs2BypassSel.nobypass
    }
  } .otherwise {
    bypassControl.rs2BypassSel := Rs2BypassSel.nobypass
  }

  when(io.in.raddr2_M =/= 0.U) {
    when(io.in.raddr2_M === io.in.waddr_W && io.in.wen_W === true.B) {
      bypassControl.memBypassSel := MemBypassSel.wm
    } .otherwise {
      bypassControl.memBypassSel := MemBypassSel.nobypass
    }
  } .otherwise {
    bypassControl.memBypassSel := MemBypassSel.nobypass
  }

  io.out.memBypassSel := bypassControl.memBypassSel

}
