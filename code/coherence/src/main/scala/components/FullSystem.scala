
package components

import chisel3._
import chisel3.util.{Cat, Decoupled, Queue, log2Ceil}
import coherence.internal.AutoEnum
import coherences.PMESI
import firrtl.FirrtlProtos.Firrtl.Type.BundleType
import params.{MemorySystemParams, SimpleCacheParams}
import firrtl.{ir, _}
import firrtl.ir.{BundleType, IntWidth, UIntType}
import firrtl.passes._
import firrtl.transforms._

import scala.io.Source
import java.io.File
import java.io.PrintWriter

import chisel3.MultiIOModule
import chisel3.internal.naming.chiselName
import param.{CoreParam, RISCVParam}
// import dbgutil.exposeTop

@chiselName
class FullSystem(
                coreParam: CoreParam,
                memorySystemParams: MemorySystemParams
                 ) extends MultiIOModule{

  val masterCount: Int = memorySystemParams.masterCount
  val busDataWidth: Int = memorySystemParams.busDataWidth
  val CohS: AutoEnum = memorySystemParams.CohS
  val cacheParams: SimpleCacheParams = memorySystemParams.cacheParams

  assert(masterCount % 2 == 0, "masterCount should be a multiple of 2")
  val dataWidth = memorySystemParams.dataWidth
  val addrWidth = cacheParams.addrWidth
  val interfaceAddrWidth = memorySystemParams.interfaceAddrWidth
  val nCore = masterCount / 2
  val genCacheReq = memorySystemParams.getGenCacheReq
  val genDebugCacheLine = memorySystemParams.getGenDebugCacheline
  val genDramReq = memorySystemParams.getGenDramReq
  val genDramResp = memorySystemParams.getGenDramResp

  val genErrorMessage = new ErrorMessage()
  val transactionIDWidth = 1
  val beatWidth = 128
  val genAXI4 = new AXI4(transactionIDWidth, interfaceAddrWidth, beatWidth)

  val io = IO(new Bundle {
    val reset_pc_i = Input(UInt(addrWidth.W))
    val timer_irq_i = Input(UInt(1.W))
    //val reg_wr_en_o = Output(UInt((nCore * 1).W))
    //val reg_addr_o  = Output(UInt((nCore * 5).W))
    //val reg_data_o  = Output(UInt((nCore * dataWidth).W))
    //val ir_o        = Output(UInt((nCore * dataWidth).W))
    //val pc_o        = Output(UInt((nCore * addrWidth).W))

    // val data_array_debug_clock = Input(Clock())
    // val data_array_debug_read_addr = Input(UInt(cacheParams.lineAddrWidth.W))
    // val data_array_debug_read_data_o = Output(UInt( (masterCount * genDebugCacheLine.getWidthM).W))

    val err = Output(genErrorMessage)


    val gpio_syscall_reg_data_o = Output(UInt(memorySystemParams.dataWidth.W))
    val gpio_syscall_en_o = Output(UInt((memorySystemParams.masterCount / 2).W))
    val gpio_syscall_ctrl_i = Input( UInt(new GPIOControl(memorySystemParams).getWidth.W) )
    val gpio_dram_base_i = Input(SInt(memorySystemParams.interfaceAddrWidth.W))

    val bus_req_addr = Output(Vec(masterCount, UInt(32.W)))

    val bus_llc_req_addr = Output(UInt(32.W))
  })
  val m_axi = IO(genAXI4)

  val memory = Module(new MemorySubsystem(coreParam, memorySystemParams, genErrorMessage))

  val mem_to_axi4 = Module(new MemToAXI4(memorySystemParams))

  memory.io.err <> io.err
  memory.io.dram.request_channel <> mem_to_axi4.io.dramReq
  memory.io.dram.response_channel <> mem_to_axi4.io.dramResp

  mem_to_axi4.m_axi <> m_axi //axi4_to_mem.s_axi
  m_axi.awaddr := (mem_to_axi4.m_axi.awaddr.asSInt + io.gpio_dram_base_i).asUInt
  m_axi.araddr := (mem_to_axi4.m_axi.araddr.asSInt + io.gpio_dram_base_i).asUInt


  /*
  for {i <- 0 until masterCount} {
    memory.io.debug(i).data_array_debug_clock := io.data_array_debug_clock
    memory.io.debug(i).data_array_debug_read_addr := io.data_array_debug_read_addr
  }
  io.data_array_debug_read_data_o := Cat(for{ i <- (0 until masterCount).reverse } yield {
    memory.io.debug(i).data_array_debug_read_data_o.asUInt
  })
  */

  val cores = for { i <- 0 until (masterCount / 2) } yield {
    val core = Module(new ProcessorWrapper(i, addrWidth, dataWidth))
    //val icacheQ = Module(new Queue(genCacheReq, 2))
    val dcacheQ = Module(new Queue(genCacheReq, 2))
    core.io.clock := clock
    core.io.reset := reset
    core.io.reset_pc_i := io.reset_pc_i

    core.io.timer_irq_i := io.timer_irq_i

    memory.io.core.request_channel(2*i).bits.address     := core.io.icachereq_data_o_address
    memory.io.core.request_channel(2*i).bits.data        := core.io.icachereq_data_o_data
    memory.io.core.request_channel(2*i).bits.length      := core.io.icachereq_data_o_length
    memory.io.core.request_channel(2*i).bits.mem_type    := core.io.icachereq_data_o_mem_type
    memory.io.core.request_channel(2*i).bits.is_amo      := core.io.icachereq_data_o_is_amo
    memory.io.core.request_channel(2*i).bits.amo_alu_op  := core.io.icachereq_data_o_amo_alu_op
    memory.io.core.request_channel(2*i).bits.aq          := core.io.icachereq_data_o_aq
    memory.io.core.request_channel(2*i).bits.rl          := core.io.icachereq_data_o_rl
    memory.io.core.request_channel(2*i).bits.flush       := core.io.icachereq_data_o_flush
    memory.io.core.request_channel(2*i).bits.llcc_flush  := core.io.icachereq_data_o_llcc_flush
    memory.io.core.request_channel(2*i).valid            := core.io.icachereq_valid_o
    core.io.icachereq_ready_i                            := memory.io.core.request_channel(2*i).ready

    core.io.icacheresp_data_i_mem_type                   := memory.io.core.response_channel(2*i).bits.mem_type
    core.io.icacheresp_data_i_length                     := memory.io.core.response_channel(2*i).bits.length
    core.io.icacheresp_data_i_data                       := memory.io.core.response_channel(2*i).bits.data
    core.io.icacheresp_valid_i                           := memory.io.core.response_channel(2*i).valid
    memory.io.core.response_channel(2*i).ready           := core.io.icacheresp_ready_o

    // == == == == I/D $ == == == ==

    dcacheQ.io.enq.bits.address     := core.io.dcachereq_data_o_address
    dcacheQ.io.enq.bits.data        := core.io.dcachereq_data_o_data
    dcacheQ.io.enq.bits.length      := core.io.dcachereq_data_o_length
    dcacheQ.io.enq.bits.mem_type    := core.io.dcachereq_data_o_mem_type
    dcacheQ.io.enq.bits.is_amo      := core.io.dcachereq_data_o_is_amo
    dcacheQ.io.enq.bits.amo_alu_op  := core.io.dcachereq_data_o_amo_alu_op
    dcacheQ.io.enq.bits.aq          := core.io.dcachereq_data_o_aq
    dcacheQ.io.enq.bits.rl          := core.io.dcachereq_data_o_rl
    dcacheQ.io.enq.bits.flush       := core.io.dcachereq_data_o_flush
    dcacheQ.io.enq.bits.llcc_flush  := core.io.dcachereq_data_o_llcc_flush
    dcacheQ.io.enq.valid            := core.io.dcachereq_valid_o
    core.io.dcachereq_ready_i       := dcacheQ.io.enq.ready
    memory.io.core.request_channel(2*i + 1) <> dcacheQ.io.deq

    core.io.dcacheresp_data_i_mem_type                       := memory.io.core.response_channel(2*i + 1).bits.mem_type
    core.io.dcacheresp_data_i_length                         := memory.io.core.response_channel(2*i + 1).bits.length
    core.io.dcacheresp_data_i_data                           := memory.io.core.response_channel(2*i + 1).bits.data
    core.io.dcacheresp_valid_i                               := memory.io.core.response_channel(2*i + 1).valid
    memory.io.core.response_channel(2*i + 1).ready           := core.io.dcacheresp_ready_o

    core
  }


  val reg_wr_en_o = Cat(for{i <- (0 until masterCount / 2).reverse } yield {cores(i).io.reg_wr_en_o })
  val reg_addr_o  = Cat(for{i <- (0 until masterCount / 2).reverse } yield {cores(i).io.reg_addr_o })
  val reg_data_o  = Cat(for{i <- (0 until masterCount / 2).reverse } yield {cores(i).io.reg_data_o })
  val ir_o        = Cat(for{i <- (0 until masterCount / 2).reverse } yield {cores(i).io.ir_o })
  val pc_o        = Cat(for{i <- (0 until masterCount / 2).reverse } yield {cores(i).io.pc_o })
  for { i <- 0 until masterCount / 2} {
    val pc_o_wire = WireInit(cores(i).io.pc_o)
    val ir_o_wire = WireInit(cores(i).io.ir_o)
    // exposeTop(pc_o_wire)
    // exposeTop(ir_o_wire)
  }

  val systemIO = Module(new GPIOInterface(memorySystemParams))

  systemIO.io.ir_o := ir_o
  systemIO.io.reg_addr_i := reg_addr_o
  systemIO.io.reg_wr_en_i := reg_wr_en_o
  systemIO.io.reg_data_i := reg_data_o

  systemIO.io.gpio_syscall_ctrl_i := io.gpio_syscall_ctrl_i
  io.gpio_syscall_en_o := systemIO.io.gpio_syscall_en_o
  io.gpio_syscall_reg_data_o := systemIO.io.gpio_syscall_reg_data_o

  io.bus_req_addr   := memory.io.bus_req_addr

  io.bus_llc_req_addr  := memory.io.bus_llc_req_addr

}

object FullSystemRTL extends App {
  val nCore = 3
  val depth = 4
  val lineSize = 64
  val addrWidth = 32
  val interfaceAddrWidth = 40
  val dataWidth = 32
  val slotWidth = 128
  val busDataWidth = 32
  val masterCount = nCore * 2
  val cacheParams = SimpleCacheParams(depth, 1, lineSize * 8, addrWidth)
  val memorySystemParams = MemorySystemParams(
    addrWidth = addrWidth,
    interfaceAddrWidth = interfaceAddrWidth,
    dataWidth = dataWidth,
    slotWidth = slotWidth,
    busDataWidth = busDataWidth,
    busRequestType = new RequestType {},
    masterCount = masterCount,
    CohS = new PMESI {},
    cacheParams = cacheParams,
    () => { new PMESICoherenceTable() },
    () => { new PMESILLCCoherenceTable(masterCount) },
    outOfSlotResponse = false,
    withCriticality = false
  )
  val XLEN = 64
  val fetchWidth = 32
  val isaParam = new RISCVParam(XLEN = XLEN,
    Embedded = false,
    Atomic = true,
    Multiplication = false,
    Compressed = false,
    SingleFloatingPoint = false,
    DoubleFloatingPoint = false)

  val coreParam = new CoreParam(fetchWidth = fetchWidth,
    isaParam = isaParam,
    iCacheReqDepth = 1,
    iCacheRespDepth = 1,
    resetRegisterAddress =  0x80000000L,
    initPCRegisterAddress = 0x80003000L,
    baseAddrAddress = 0x80001000L,
    coreID = 0, // Note this is just a placeholder, internally, coreID will be adjusted
    withAXIMemoryInterface = true,
    nCore = nCore)
  chisel3.Driver.execute(args, () => new FullSystem(coreParam, memorySystemParams))
  chisel3.Driver.execute(args, () => new AXI4ToMem(memorySystemParams))

  /* genearte the connections for AXI4 interface between core and dram, these parameters are currently fixed */
  val file = "verilog/AXI4ToMem.fir"
  val input = Source.fromFile(file).getLines.mkString("\n")
  // Parse the input
  val state = CircuitState(firrtl.Parser.parse(input), UnknownForm)
  for { module <- state.circuit.modules } {
    for { port <- module.ports } {
      if(port.name.endsWith("axi")) {
        val connDeclaration3: Seq[(String, String, String)] = port.tpe match {
          case t: ir.BundleType => t.fields.map(i => {
            val width = i.tpe.asInstanceOf[UIntType].width.asInstanceOf[IntWidth].width
            (s"wire [${width-1}:0] ${i.name};", s".m_axi_${i.name}(${i.name})", s".${port.name}_${i.name}(${i.name})")
          })
          case _ => Seq()
        }
        val (wireDecl, masterConn, slaveConn): (Seq[String], Seq[String], Seq[String]) = connDeclaration3.unzip3
        val wireDeclMacro = wireDecl.mkString("`define AXI_WIRES ", "\\\n", "\n\n")
        val masterConnMacro = masterConn.mkString("`define M_AXI_CONNECTION ", sep=",\\\n", end="\n\n")
        val slaveConnMacro = slaveConn.mkString("`define S_AXI_CONNECTION ", sep=",\\\n", end="\n\n")

        val writer = new PrintWriter(new File("verilog/conn_inc.v"))
        writer.write(wireDeclMacro)
        writer.write(masterConnMacro)
        writer.write(slaveConnMacro)
        writer.close()
      }
    }
  }

}
