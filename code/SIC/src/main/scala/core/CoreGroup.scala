
package core

import chisel3._
import chisel3.util._
import dbgutil.exposeTop
import param.CoreParam

class CoreGroupAXI(coreParam: CoreParam) extends MultiIOModule {
  private val wordWidth = 64
  val m = IO(new Bundle {
    val axi = new AXI(coreParam.isaParam.XLEN, wordWidth, log2Ceil(2 * coreParam.nCore))
    val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
    val baseAddress = Input(UInt(coreParam.isaParam.XLEN.W))
    val coreAddr = Output(UInt(64.W))
    val coreData = Output(UInt(32.W))
    val coreValid = Output(Bool())
    val coreReady = Output(Bool())
    val inst = Output(UInt(coreParam.isaParam.instructionWidth.W))
  })

  val axiTranslationLayer = Module(new AXITranslationLayer(coreParam))
  val cores = for { i <- 0 until coreParam.nCore } yield {
    val core = Module(new Core(coreParam.copy(coreID = i)))

    core.io.dCacheReq <> axiTranslationLayer.io.dCacheReq(i)
    core.io.iCacheReq <> axiTranslationLayer.io.iCacheReq(i)
    core.io.dCacheResp <> axiTranslationLayer.io.dCacheResp(i)
    core.io.iCacheResp <> axiTranslationLayer.io.iCacheResp(i)

    core.io.irq := 0.U
    core.io.initPC := m.initPC


    core
  }

  m.coreData := cores(0).io.iCacheResp.bits.data
  m.coreAddr := cores(0).io.iCacheResp.bits.address
  m.coreValid := cores(0).io.iCacheResp.valid
  m.coreReady := cores(0).io.iCacheResp.ready

  m.inst := cores(0).io.inst

  m.axi <> axiTranslationLayer.io.m_axi
  axiTranslationLayer.io.baseAddress := m.baseAddress
}

/**
  *  Only used for verification, because of the memory
  */
class CoreGroupAXIWithMemory(coreParam: CoreParam) extends Module {
  val io = IO(new Bundle {
    val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
    val baseAddress = Input(UInt(coreParam.isaParam.XLEN.W))
    val inst = Output(UInt(coreParam.isaParam.instructionWidth.W))
  })

  val initPC = "h_0100_0000".U
  val baseAddress = "h_0100_0000".U
  val mem = Module(new AXIMemory(coreParam))
  val coreGroup = Module(new CoreGroupAXI(coreParam))
  io.inst := coreGroup.m.inst
  coreGroup.m.initPC := initPC
  coreGroup.m.baseAddress := baseAddress
  coreGroup.m.axi <> mem.io.m_axi
}
