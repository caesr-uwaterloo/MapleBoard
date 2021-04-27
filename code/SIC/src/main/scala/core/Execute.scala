
package core

import chisel3._
import chisel3.util._
import chisel3.experimental._
import param.CoreParam

class ExecuteIO(private val coreParam: CoreParam) extends Bundle {
  val controlInput = Flipped(Decoupled(new Control(coreParam)))
  val dataInput = Flipped(Decoupled(new DataPath(coreParam)))

  val controlOutput = Decoupled(new Control(coreParam))
  val dataOutput = Decoupled(new DataPath(coreParam))
}

/**
  *  Note for branch instructions, the alu is used as comparing unit while the branch target is calculated using
  *  a separate adder
  */
class Execute(private val coreParam: CoreParam) extends Module {
  val isaParam = coreParam.isaParam
  val io = IO(new ExecuteIO(coreParam))
  private val alu = Module(new ALU(isaParam))
  // for calculating branch target

  val in1Mux = WireInit(0.U(isaParam.XLEN.W))
  val in2Mux = WireInit(0.U(isaParam.XLEN.W))
  val addrRes = Mux(io.controlInput.bits.controlTransferType === ControlTransferType.jumpr,
    io.dataInput.bits.regData1,
    io.dataInput.bits.pc) + io.dataInput.bits.imm
  val branchTarget = Cat(addrRes(isaParam.XLEN-1, 1), 0.U(1.W))

  io.controlOutput <> io.controlInput
  io.dataOutput <> io.dataInput

  /**
    * Execute stage ready valid signal generation block
    */
  val busy = io.controlInput.valid
  io.controlInput.ready := !busy || io.controlOutput.fire()
  io.dataInput.ready := io.controlInput.ready
  io.controlOutput.valid := io.controlInput.valid
  io.dataOutput.valid := io.controlOutput.valid
  /**
    * block end
    */

  switch(io.controlInput.bits.in1Sel) {
    is(In1Sel.pc) { in1Mux := io.dataInput.bits.pc }
    is(In1Sel.reg) { in1Mux := io.dataInput.bits.regData1 }
  }
  switch(io.controlInput.bits.in2Sel) {
    is(In2Sel.imm) { in2Mux := io.dataInput.bits.imm }
    is(In2Sel.pcPlus4) { in1Mux := io.dataInput.bits.pcPlus4 }
    is(In2Sel.reg) { in2Mux := io.dataInput.bits.regData2 }
  }

  // branch result resolution
  io.controlOutput.bits.branchTaken := false.B
  switch(io.controlInput.bits.branchType) {
    is(BranchType.beq) { io.controlOutput.bits.branchTaken := alu.io.out === 0.U }
    is(BranchType.bne) { io.controlOutput.bits.branchTaken := alu.io.out =/= 0.U }
    // NOTE in these cases, the ALU will use the slt/sltu operations so the result of
    // the two instructions can be used to determine the branches
    is(BranchType.blt)  { io.controlOutput.bits.branchTaken := alu.io.out === 1.U }
    is(BranchType.bltu) { io.controlOutput.bits.branchTaken := alu.io.out === 1.U }
    is(BranchType.bge)  { io.controlOutput.bits.branchTaken := alu.io.out === 0.U }
    is(BranchType.bgeu) { io.controlOutput.bits.branchTaken := alu.io.out === 0.U }
  }


  alu.io.aluop := io.controlInput.bits.aluop
  alu.io.in1 := in1Mux
  alu.io.in2 := in2Mux
  alu.io.isW := io.controlInput.bits.isW
  io.dataOutput.bits.aluData := alu.io.out
  io.dataOutput.bits.memoryAddress := alu.io.out
  io.dataOutput.bits.memoryData := io.dataInput.bits.regData2  // rs2, store
  io.dataOutput.bits.branchTarget := branchTarget
  switch(io.controlInput.bits.csrSource) {
    is(CSRSourceSel.reg) { io.dataOutput.bits.csrWriteData := io.dataInput.bits.regData1 } // bypassing
    is(CSRSourceSel.uimm) { io.dataOutput.bits.csrWriteData := io.controlInput.bits.raddr1 } // zero extension
  }
  when(io.controlInput.valid) {
    printf(p"[E${coreParam.coreID}] controlTransferType: ${io.controlInput.bits.controlTransferType.asUInt} branchType: ${io.controlInput.bits.branchType.asUInt}, branchTaken: ${io.controlOutput.bits.branchTaken.asUInt}, branchTarget: ${Hexadecimal(branchTarget)}\n")
    printf(p"[E${coreParam.coreID}] pc: ${Hexadecimal(io.dataInput.bits.pc)} + ${Hexadecimal(io.dataInput.bits.imm)}, inst: ${Hexadecimal(io.dataOutput.bits.inst)}, tval: ${Hexadecimal(io.controlOutput.bits.exception.tval)}\n")
    printf(p"[E${coreParam.coreID}] rs1: ${io.controlInput.bits.in1Sel.asUInt} in1: ${Hexadecimal(alu.io.in1)} rs2: ${io.controlInput.bits.in2Sel.asUInt} in2: ${Hexadecimal(alu.io.in2)} aluout: ${Hexadecimal(alu.io.out)}\n")
    printf(p"[E${coreParam.coreID}] regData2: ${Hexadecimal(io.dataInput.bits.regData2)}\n")
  }
}
