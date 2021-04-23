
package core

import chisel3._
import chisel3.core.EnumFactory
import chisel3.experimental._
import chisel3.util._
import param.CoreParam
import riscv.{Causes, Instructions}

/* the classes in this file transform the instruction specification in the opcodes file into decoder */

class DecodeIO(private val coreParam: CoreParam) extends Bundle {
  private val genControl = new Control(coreParam)
  private val genDataPipe = new DataPath(coreParam)
  private val genFetchResponse = new FetchResponse(coreParam.isaParam)
  val fetchResp = Flipped(Decoupled(genFetchResponse))

  val control = Decoupled(genControl)
  val data = Decoupled(genDataPipe)
}

object ControlTransferType extends ChiselEnum {
  val branch, jump, jumpr, trap, eret, none = Value
}

object BranchType extends ChiselEnum {
  val beq, bne, blt, bge, bltu, bgeu, none = Value
}

object AMOOP extends ChiselEnum {
  val add, swap, lr, sc, and, or, xor, max, maxu, none = Value
}

object DecodeVector {
  def apply(controlTransferType: ControlTransferType.Type,
            branchType: BranchType.Type,
            isMemory: Bool,
            isW: Bool, // is word op in RV64
            aluOp: ALUOP.Type,
            instructionType: InstructionType.Type,
            isCSR: Bool,
            csrOp: CSROP.Type,
            writeRegister: Bool,
            csrSource: CSRSourceSel.Type,
            memoryRequestType: MemoryRequestType.Type,
            amoOp: AMOOP.Type,
            in1Sel: In1Sel.Type,
            in2Sel: In2Sel.Type) = {
    new DecodeVector(controlTransferType, branchType, isMemory, isW, aluOp, instructionType, isCSR, csrOp, writeRegister, csrSource, memoryRequestType, amoOp, in1Sel, in2Sel)
  }

  def apply(): DecodeVector = {
    new DecodeVector(ControlTransferType(), BranchType(), Bool(), Bool(), ALUOP(), InstructionType(), Bool(), CSROP(), Bool(),
      CSRSourceSel(), MemoryRequestType(), AMOOP(), In1Sel(), In2Sel())
  }

  def nop(): DecodeVector = {
    new DecodeVector(ControlTransferType.none,
      BranchType.beq,
      false.B,
      false.B,
      ALUOP.undef,
      InstructionType.I,
      false.B,
      CSROP.nop,
      false.B,
      CSRSourceSel.reg,
      MemoryRequestType.read,
      AMOOP.none,
      In1Sel.reg,
      In2Sel.reg)
  }
}

class DecodeVector(val controlTransferType: ControlTransferType.Type,
                   val branchType: BranchType.Type,
                   val isMemory: Bool,
                   val isW: Bool, // is word op in RV64
                   val aluOp: ALUOP.Type,
                   val instructionType: InstructionType.Type,
                   val isCSR: Bool,
                   val csrOp: CSROP.Type,
                   val writeRegister: Bool,
                   val csrSource: CSRSourceSel.Type,
                   val memoryRequestType: MemoryRequestType.Type,
                   val amoOp: AMOOP.Type,
                   val in1Sel: In1Sel.Type, // TODO: could merge
                   val in2Sel: In2Sel.Type
                  ) extends Bundle {
  override def cloneType: DecodeVector.this.type = new DecodeVector(
    controlTransferType,
    branchType,
    isMemory,
    isW,
    aluOp,
    instructionType,
    isCSR,
    csrOp,
    writeRegister,
    csrSource,
    memoryRequestType,
    amoOp,
    in1Sel,
    in2Sel
    ).asInstanceOf[this.type]
}

class Decode(coreParam: CoreParam) extends Module {
  val io = IO(new DecodeIO(coreParam))
  private val Y = true.B
  private val N = false.B
  private val isaParam = coreParam.isaParam
  private val insnDecomp = Module(new InstructionField(coreParam))
  private val decodeTable: List[(BitPat, DecodeVector)] = List(
    // TODO: InstructionType may be inferred from the generated file
    (Instructions.AUIPC,  DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.add,   InstructionType.U, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.pc, In2Sel.imm)),
    (Instructions.LUI,    DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.in2,   InstructionType.U, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),

    (Instructions.ADD,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.XOR,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.xor,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.OR,     DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.or,    InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.AND,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.and,    InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SUB,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sub,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SRA,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sra,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SRL,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.srl,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SLT,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.slt,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SLTU,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sltu,  InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SLL,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sll,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.ADDW,   DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SUBW,   DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.sub,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SRAW,   DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.sraw,  InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SLLW,   DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.sllw,  InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.SRLW,   DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.srlw,  InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),

    (Instructions.ADDI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.XORI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.xor,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.ANDI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.and,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.ORI,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.or,    InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SLTI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.slt,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SLLI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sll,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SRAI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sra,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SRLI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.srl,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SLTIU,  DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sltu,  InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.ADDIW,  DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.addw,  InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SRAIW,  DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.sraw,  InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SRLIW,  DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.srlw,  InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SLLIW,  DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.sllw,  InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),

    (Instructions.SLLI,   DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.sll,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SLLIW,  DecodeVector(ControlTransferType.none, BranchType.none, N, Y, ALUOP.sllw,  InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.ORI,    DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.or,    InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),

    (Instructions.LB,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.LH,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.LW,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.LD,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.LBU,    DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.LHU,    DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.LWU,    DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.imm)),

    (Instructions.SB,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.S, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.write, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SH,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.S, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.write, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SW,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.S, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.write, AMOOP.none, In1Sel.reg, In2Sel.imm)),
    (Instructions.SD,     DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.S, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.write, AMOOP.none, In1Sel.reg, In2Sel.imm)),

    (Instructions.AMOADD_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.add, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOADD_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.add, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOAND_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.and, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOAND_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.and, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOXOR_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.xor, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOXOR_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.xor, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOOR_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.or, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOOR_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.or, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOSWAP_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.swap, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOSWAP_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.swap, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOMAX_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.max, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOMAX_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.max, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOMAXU_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.maxu, In1Sel.reg, In2Sel.imm)),
    (Instructions.AMOMAXU_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.maxu, In1Sel.reg, In2Sel.imm)),
    (Instructions.LR_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.lr, In1Sel.reg, In2Sel.imm)),
    (Instructions.SC_W, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.sc, In1Sel.reg, In2Sel.imm)),
    (Instructions.LR_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.lr, In1Sel.reg, In2Sel.imm)),
    (Instructions.SC_D, DecodeVector(ControlTransferType.none, BranchType.none, Y, N, ALUOP.add,   InstructionType.R, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.amo, AMOOP.sc, In1Sel.reg, In2Sel.imm)),


    (Instructions.FENCE_I, DecodeVector.nop()), // we do not need fences for now
    (Instructions.FENCE,   DecodeVector.nop()),

    (Instructions.JAL,    DecodeVector(ControlTransferType.jump, BranchType.none, N, N, ALUOP.in1,   InstructionType.J, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.pcPlus4)),
    (Instructions.JALR,   DecodeVector(ControlTransferType.jumpr,BranchType.none, N, N, ALUOP.in1,   InstructionType.I, N, CSROP.nop,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.pcPlus4)),

    (Instructions.BEQ,    DecodeVector(ControlTransferType.branch, BranchType.beq,  N, N, ALUOP.sub,   InstructionType.B, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.BNE,    DecodeVector(ControlTransferType.branch, BranchType.bne,  N, N, ALUOP.sub,   InstructionType.B, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.BLT,    DecodeVector(ControlTransferType.branch, BranchType.blt,  N, N, ALUOP.slt,   InstructionType.B, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.BLTU,   DecodeVector(ControlTransferType.branch, BranchType.bltu, N, N, ALUOP.sltu,  InstructionType.B, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.BGE,    DecodeVector(ControlTransferType.branch, BranchType.bge,  N, N, ALUOP.slt,   InstructionType.B, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.BGEU,   DecodeVector(ControlTransferType.branch, BranchType.bgeu, N, N, ALUOP.sltu,  InstructionType.B, N, CSROP.nop,   N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),

    (Instructions.CSRRWI, DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.write, Y, CSRSourceSel.uimm, MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.CSRRSI, DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.set,   Y, CSRSourceSel.uimm, MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.CSRRCI, DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.clear, Y, CSRSourceSel.uimm, MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.CSRRW,  DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.write, Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.CSRRS,  DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.set,   Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.CSRRC,  DecodeVector(ControlTransferType.none, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.clear, Y, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),

    // privileged instructions
    (Instructions.ECALL,  DecodeVector(ControlTransferType.trap, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.exception, N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.EBREAK, DecodeVector(ControlTransferType.trap, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.exception, N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
    (Instructions.MRET,   DecodeVector(ControlTransferType.eret, BranchType.none, N, N, ALUOP.undef, InstructionType.I, N, CSROP.mret,      N, CSRSourceSel.reg,  MemoryRequestType.read, AMOOP.none, In1Sel.reg, In2Sel.reg)),
  )
  val decodeVector = Wire(DecodeVector())
  val pcPlus4 = io.fetchResp.bits.pc
  val pc = pcPlus4 - 4.U
  // default case
  setDecodeVector(DecodeVector.nop())
  val insnMatched = generateDecodeLogic(io.fetchResp.bits.instruction)


  io.fetchResp.ready := io.control.ready

  insnDecomp.io.instruction := io.fetchResp.bits.instruction
  insnDecomp.io.instructionType := decodeVector.instructionType

  // register file control
  io.control.bits.raddr1 := insnDecomp.io.instructionFields.rs1
  io.control.bits.raddr2 := insnDecomp.io.instructionFields.rs2
  io.control.bits.waddr := insnDecomp.io.instructionFields.rd
  io.control.bits.wen := decodeVector.writeRegister

  io.control.bits.length := insnDecomp.io.instructionFields.funct3

  io.control.bits.controlTransferType := decodeVector.controlTransferType
  io.control.bits.aluop := decodeVector.aluOp
  io.control.bits.isW := decodeVector.isW
  io.control.bits.isMemory := decodeVector.isMemory
  io.control.bits.acquire := insnDecomp.io.instructionFields.aq
  io.control.bits.release := insnDecomp.io.instructionFields.rl

  io.control.bits.csrOp := decodeVector.csrOp
  io.control.bits.memoryRequestType := decodeVector.memoryRequestType
  io.control.bits.isAMO := decodeVector.amoOp =/= AMOOP.none
  io.control.bits.amoOp := decodeVector.amoOp

  io.control.bits.csrAddress := insnDecomp.io.instructionFields.csrDest

  io.control.bits.exception.cause := Causes.illegal_instruction.U
  io.control.bits.exception.tval := io.fetchResp.bits.instruction
  io.control.bits.exception.valid := !insnMatched
  when(decodeVector.csrOp === CSROP.exception) {
    io.control.bits.exception.cause := Causes.machine_ecall.asUInt
    io.control.bits.exception.tval := 0.U
    io.control.bits.exception.valid := true.B
  }.elsewhen(decodeVector.csrOp === CSROP.breakpoint) {
    io.control.bits.exception.cause := Causes.breakpoint.asUInt
    io.control.bits.exception.tval := 0.U
    io.control.bits.exception.valid := true.B
  }

  io.control.bits.in1Sel := decodeVector.in1Sel
  io.control.bits.in2Sel := decodeVector.in2Sel

  io.control.bits.csrSource := decodeVector.csrSource
  io.control.valid := io.fetchResp.valid

  io.control.bits.branchTaken := false.B
  io.control.bits.branchType := decodeVector.branchType

  io.control.bits.instructionType := decodeVector.instructionType

  // provide default values for data path, won't be used
  io.data.bits := 0.U.asTypeOf(io.data.bits)
  io.data.valid := io.fetchResp.valid
  io.data.bits.imm := insnDecomp.io.instructionFields.imm
  io.data.bits.pcPlus4 := pcPlus4
  io.data.bits.pc := pc

  def setDecodeVector(value: DecodeVector): Unit = {
    decodeVector.controlTransferType := value.controlTransferType
    decodeVector.isMemory            := value.isMemory
    decodeVector.isW                 := value.isW
    decodeVector.aluOp               := value.aluOp
    decodeVector.instructionType     := value.instructionType
    decodeVector.isCSR               := value.isCSR
    decodeVector.csrOp               := value.csrOp
    decodeVector.writeRegister       := value.writeRegister
    decodeVector.csrSource           := value.csrSource
    decodeVector.memoryRequestType   := value.memoryRequestType
    decodeVector.amoOp               := value.amoOp
    decodeVector.in1Sel              := value.in1Sel
    decodeVector.in2Sel              := value.in2Sel
    decodeVector.branchType          := value.branchType
  }
  def generateDecodeLogic(insn: UInt): Bool = {
    val head :: remainingDecodeTable = decodeTable
    val instructionMatched = WireInit(false.B)
    var whenBranch = when(insn === head._1) {
      setDecodeVector(head._2)
      instructionMatched := true.B
    }
    for { (p, v) <- remainingDecodeTable } {
      whenBranch = whenBranch.elsewhen(insn === p) {
        setDecodeVector(v)
        instructionMatched := true.B
      }
    }
    instructionMatched
  }
  when(io.fetchResp.fire()) {
    printf(p"[D${coreParam.coreID}] ${io.fetchResp}, raddr2 ${io.control.bits.raddr2} \n")
  }
}

object DecodeGen extends App {
}