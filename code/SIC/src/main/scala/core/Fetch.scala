
package core

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._
import param.{CoreParam, RISCVParam}

class ICacheReq(private val coreParam: CoreParam) extends Bundle {
  val address: UInt = UInt(coreParam.isaParam.XLEN.W)
  val length: UInt = UInt(2.W)
  override def toPrintable: Printable = {
    p"ICacheReq(address=0x${Hexadecimal(address)})"
  }
}

class ICacheResp(private val coreParam: CoreParam) extends Bundle {
  val address: UInt = UInt(coreParam.isaParam.XLEN.W)
  val data: UInt = UInt(coreParam.fetchWidth.W)
  override def toPrintable: Printable = {
    p"ICacheResp(address=0x${Hexadecimal(address)}, data=0x${Hexadecimal(data)})"
  }
}

object NextPCSel extends ChiselEnum {
  val pcPlus4, jumpTarget, branchTarget, trapVectorBase, eret = Value
}

class FetchRequest(private val isaParam: RISCVParam) extends Bundle {
  val fetchFrom = NextPCSel()
  override def toPrintable: Printable = {
    p"FetchReq(fetchFrom=${fetchFrom.asUInt})"
  }
}

class FetchResponse(private val isaParam: RISCVParam) extends Bundle {
  val pc: UInt = UInt(isaParam.XLEN.W)
  val instruction: UInt = UInt(isaParam.instructionWidth.W)
  override def toPrintable: Printable = {
    p"FetchResp(pc(+4)=0x${Hexadecimal(pc)}, instruction=0x${Hexadecimal(instruction)})"
  }
}

class FetchIO(private val coreParam: CoreParam) extends Bundle {
  private val genICacheReq = new ICacheReq(coreParam)
  private val genICacheResp = new ICacheResp(coreParam)
  private val genFetchRequest = new FetchRequest(coreParam.isaParam)
  private val genFetchResponse = new FetchResponse(coreParam.isaParam)

  val fetchReq = Flipped(Decoupled(genFetchRequest))
  val fetchResp = Decoupled(genFetchResponse)

  val iCacheReq = Decoupled(genICacheReq)
  val iCacheResp = Flipped(Decoupled(genICacheResp))

  val branchTarget = Input(UInt(coreParam.isaParam.XLEN.W))
  val jumpTarget = Input(UInt(coreParam.isaParam.XLEN.W))
  val trapVectorBase = Input(UInt(coreParam.isaParam.XLEN.W))
  val eret = Input(UInt(coreParam.isaParam.XLEN.W))
  val pcPlus4 = Input(UInt(coreParam.isaParam.XLEN.W))

  // configuration port
  val initPC = Input(UInt(coreParam.isaParam.XLEN.W))
}

/**
  *  The Fetch module accepts the address of next pc and issue the request to ICache
  *  Note that when the remaining pipelines are not able to consume the the current instruction,
  *  the module is stalled
  *  Currently, no mechanism for killing or ignoring pending instructions is implemented, so speculative
  *  fetch will not work
  */
class Fetch(coreParam: CoreParam) extends Module {
  val genICacheReq = new ICacheReq(coreParam)
  val genICacheResp = new ICacheResp(coreParam)
  val genFetchRequest = new FetchRequest(coreParam.isaParam)
  val genFetchResponse = new FetchResponse(coreParam.isaParam)
  val io = IO(new FetchIO(coreParam))

  val npc = WireInit(io.pcPlus4)
  switch(io.fetchReq.bits.fetchFrom) {
    is(NextPCSel.branchTarget) { npc := io.branchTarget }
    is(NextPCSel.eret ) { npc := io.eret }
    is(NextPCSel.jumpTarget) { npc := io.jumpTarget }
    is(NextPCSel.pcPlus4) { npc := io.pcPlus4 }
    is(NextPCSel.trapVectorBase) { npc := io.trapVectorBase }
  }
  val resetting = RegInit(true.B)

  val reqQ = Module(new Queue(genICacheReq, coreParam.iCacheReqDepth))
  val respQ = Module(new Queue(genFetchResponse, coreParam.iCacheRespDepth))

  io.iCacheReq.valid := reqQ.io.deq.valid || resetting === 1.U
  reqQ.io.deq.ready := io.iCacheReq.ready
  io.iCacheReq.bits := reqQ.io.deq.bits

  io.fetchReq.ready := reqQ.io.enq.ready && resetting =/= 1.U
  reqQ.io.enq.valid := io.fetchReq.valid
  reqQ.io.enq.bits.address := npc
  require(!coreParam.isaParam.Compressed, "Fetch module only fetches 32-bit instructions")
  reqQ.io.enq.bits.length := 2.U

  respQ.io.deq <> io.fetchResp
  respQ.io.enq.valid := io.iCacheResp.valid
  respQ.io.enq.bits.pc := io.iCacheResp.bits.address + 4.U
  respQ.io.enq.bits.instruction := io.iCacheResp.bits.data
  io.iCacheResp.ready := respQ.io.enq.ready

  when(resetting === true.B) {
    reqQ.io.enq.valid := 1.U
    reqQ.io.enq.bits.address := io.initPC
    reqQ.io.enq.bits.length := 2.U
  }

  when(io.iCacheReq.fire() && resetting) {
    printf("Reset done!\n")
    resetting := false.B
  }

//  printf(p"[F${coreParam.coreID} FetchRequest] valid: ${reqQ.io.enq.valid}, ready: ${reqQ.io.enq.ready}, pc: ${Hexadecimal(reqQ.io.enq.bits.address)}, resetting: ${resetting}\n")
//  printf(p"[F${coreParam.coreID} ICacheRequestEnq] valid: ${io.iCacheReq.valid}, ready: ${io.iCacheReq.ready}, pc: ${Hexadecimal(io.iCacheReq.bits.address)}\n")

  when(io.iCacheReq.fire()) {
    printf(p"[F${coreParam.coreID}] pc: ${Hexadecimal(reqQ.io.enq.bits.address)}\n")
  }
//  when(io.iCacheResp.fire()) {
//    printf(p"[F${coreParam.coreID}] ${io.iCacheResp}, resetting: ${resetting}\n")
//  }
}
