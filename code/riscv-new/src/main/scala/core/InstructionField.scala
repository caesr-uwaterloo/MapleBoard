
package core

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import param.CoreParam

object InstructionType extends ChiselEnum {
  val R, I, S, B, U, J = Value
}

class InstructionFields(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam
  val rs1 = UInt(log2Ceil(isaParam.registerCount).W)
  val rs2 = UInt(log2Ceil(isaParam.registerCount).W)
  val rd  = UInt(log2Ceil(isaParam.registerCount).W)
  val funct3 = UInt(3.W)
  val funct7 = UInt(7.W)
  val funct5 = UInt(5.W)

  val imm = UInt(isaParam.XLEN.W)
  val aq = Bool()
  val rl = Bool()

  val csrDest = UInt(log2Ceil(isaParam.csrCount).W)
}

class InstructionFieldIO(private val coreParam: CoreParam) extends Bundle {
  private val isaParam = coreParam.isaParam
  val instruction = Input(UInt(isaParam.instructionWidth.W))
  val instructionType = Input(InstructionType())
  val instructionFields = Output(new InstructionFields(coreParam))
}

/**
  * this module extracts the instructions into fields such as rs1, rs2, shamt etc.
  * The immediate extraction can be found in RV32I, Base Instruction Format of the RISC-V spec
  */
class InstructionField(private val coreParam: CoreParam) extends Module {
  private val isaParam = coreParam.isaParam
  require(!isaParam.Compressed, "InstructionField does not support C extension")
  val io = IO(new InstructionFieldIO(coreParam))
  val insn = io.instruction
  val insnType = Wire(InstructionType())
  val imm =  signExt(composeImm(insn, isaParam.imm12Ranges))
  val simm = signExt(composeImm(insn, isaParam.simm12Ranges))
  val bimm = signExt(Cat(composeImm(insn, isaParam.bimm12Ranges), 0.U(1.W)))
  val uimm = signExt(Cat(composeImm(insn, isaParam.imm20Ranges), 0.U(12.W)))
  val jimm = signExt(Cat(composeImm(insn, isaParam.jimm20Ranges), 0.U(1.W)))
  insnType := io.instructionType
  io.instructionFields.rs1 := insn(isaParam.rs1Range._1, isaParam.rs1Range._2)
  io.instructionFields.rs2 := insn(isaParam.rs2Range._1, isaParam.rs2Range._2)
  io.instructionFields.rd := insn(isaParam.rdRange._1, isaParam.rdRange._2)
  io.instructionFields.funct3 := insn(isaParam.funct3Range._1, isaParam.funct3Range._2)
  io.instructionFields.funct5 := insn(isaParam.funct5Range._1, isaParam.funct5Range._2)
  io.instructionFields.funct7 := insn(isaParam.funct7Range._1, isaParam.funct7Range._2)
  io.instructionFields.imm := MuxLookup(insnType.asUInt, 0.U(isaParam.XLEN.W),
    Array(
      InstructionType.I.asUInt -> imm,
      InstructionType.S.asUInt -> simm,
      InstructionType.B.asUInt -> bimm,
      InstructionType.U.asUInt -> uimm,
      InstructionType.J.asUInt -> jimm
    ))

  io.instructionFields.csrDest := composeImm(insn, isaParam.imm12Ranges)

  if(isaParam.Atomic) {
    io.instructionFields.aq := false.B
    io.instructionFields.rl := false.B
  } else {
    io.instructionFields.aq := insn(26)
    io.instructionFields.rl := insn(25)
  }


  def composeImm(insn: UInt, ranges: List[(Int, Int)]): UInt = {
    Cat(ranges.map(f => {
      insn(f._1, f._2)
    }))
  }

  def signExt(imm: UInt): UInt = {
    val width = imm.getWidth
    val sign = imm(width - 1)
    val extension = Fill(isaParam.XLEN - width, sign)
    Cat(extension, imm)
  }
}
