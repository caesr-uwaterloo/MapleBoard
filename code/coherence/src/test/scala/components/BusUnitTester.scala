// See README.md for license details.

package components

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}
import chisel3.util.{LockingArbiterLike, Queue}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/* class BusUnitTester
(c: SnoopyBus) extends PeekPokeTester(c) {
} */

class BusTester  extends ChiselFlatSpec {
  // Hack in the example project
  /*
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }
  val masterCount = 4
  val controllerReq : UInt = UInt(width=2.W)
  val controllerResp : UInt = UInt(width=2.W)
  val snoopReq : UInt = UInt(2.W)
  val snoopResp : UInt = UInt(2.W)
  val slotWidth = 10

  def dut : SnoopyBus= new SnoopyBus(masterCount,
    controllerReq, controllerResp, snoopReq, snoopResp) {
    protected override lazy val arbiter: LockingArbiterLike[UInt] with HasChoice = Module(
      new TDMArbiter(controllerReq, masterCount, slotWidth)
    )

    override def reqToSnoop(i: Int): Unit = {
      io.snoop.request_channel(i).bits := slot_owner_req_reg
    }
  }

  "Bus" should "pass echo test" in {
    iotesters.Driver.execute(Array(), () => dut) {
      c => new BusUnitTester(c) {
        private val reqQSource = List.fill(masterCount) { new ListBuffer[BigInt]() }
        private val reqQSink = new ListBuffer[BigInt]()
        private val respQSink = List.fill(masterCount) { new ListBuffer[BigInt]() }
        private val respQSource = List.fill(masterCount) { new ListBuffer[BigInt]() }
        private var reqSent = ArrayBuffer.fill(masterCount) { 0 }
        private var reqReceived = 0
        private var respReceived = ArrayBuffer.fill(masterCount) { 0 }
        private var respSent = ArrayBuffer.fill(masterCount) { 0 }
        private var curReq = ArrayBuffer.fill(masterCount) { 0 }
        private var curResp = ArrayBuffer.fill(masterCount) { 0 }
        private var snoopReqQSink = List.fill(masterCount) { new ListBuffer[BigInt]() }
        private var snoopRespQSource = List.fill(masterCount) { new ListBuffer[BigInt]() }
        private var curSnoopResp = ArrayBuffer.fill(masterCount) { 0 }

        for { i <- 0 until masterCount } {
          reqQSource(i) += i
          poke(c.io.controller.response_channel.out(i).ready, 1)
          poke(c.io.snoop.request_channel(i).ready, 1)
        }
        poke(c.io.controller.request_channel.out.ready, 1)
        for { cycle <- 0 until masterCount * slotWidth } {
          // pre edge
          // request
          for { i <- 0 until masterCount } {
            val idx = curReq(i)
            if(idx < reqQSource(i).length) {
              poke(c.io.controller.request_channel.in(i).valid, 1)
              poke(c.io.controller.request_channel.in(i).bits, reqQSource(i)(idx))
            } else {
              poke(c.io.controller.request_channel.in(i).valid, 0)
            }
          }
          // response
          for { i <- 0 until masterCount } {
            val idx = curResp(i)
            if(idx < respQSource(i).length) {
              poke(c.io.controller.response_channel.in(i).valid, 1)
              poke(c.io.controller.response_channel.in(i).bits, respQSource(i)(idx))
            } else {
              poke(c.io.controller.response_channel.in(i).valid, 0)
            }
          }
          for { i <- 0 until masterCount } {
            val rdy = peek(c.io.controller.request_channel.in(i).ready)
            val vld = peek(c.io.controller.request_channel.in(i).valid)
            if (rdy == 1 && vld == 1) {
              reqSent(i) = reqSent(i) + 1
              curReq(i) = curReq(i) + 1
            }
          }
          for { i <- 0 until masterCount } {
            val rdy = peek(c.io.controller.response_channel.in(i).ready)
            val vld = peek(c.io.controller.response_channel.in(i).valid)
            if(rdy == 1 && vld == 1) {
              curResp(i) = curResp(i) + 1
              respSent(i) = respSent(i) + 1
            }
          }

          val reqOutRdy = peek(c.io.controller.request_channel.out.ready)
          val reqOutVld = peek(c.io.controller.request_channel.out.valid)
          val reqValue = peek(c.io.controller.request_channel.out.bits)
          if(reqOutRdy == 1 && reqOutVld == 1) {
            reqQSink += reqValue
            reqReceived += 1
            respQSource(reqValue.toInt) += reqValue
          }

          for { i <- 0 until masterCount } {
            val rdy = peek(c.io.controller.response_channel.out(i).ready)
            val vld = peek(c.io.controller.response_channel.out(i).valid)
            val respValue = peek(c.io.controller.response_channel.out(i).bits)
            if(rdy == 1 && vld == 1) {
              respQSink(i) += respValue
              respReceived(i) += 1
            }
          }

          for { i <- 0 until masterCount } {
            val idx = curSnoopResp(i)
            if( idx < snoopRespQSource(i).length ) {
              poke(c.io.snoop.response_channel(i).valid, 1)
              poke(c.io.snoop.response_channel(i).bits, snoopRespQSource(i)(idx))
            } else {
              poke(c.io.snoop.response_channel(i).valid, 0)
            }
          }

          // Snoop
          for { i <- 0 until masterCount } {
            val rdy = peek(c.io.snoop.request_channel(i).ready)
            val vld = peek(c.io.snoop.request_channel(i).valid)
            val value = peek(c.io.snoop.request_channel(i).bits)
            if(rdy == 1 && vld == 1) {
              snoopReqQSink(i) += value
              snoopRespQSource(i) += value
            }
          }

          for {i <- 0 until masterCount } {
            val rdy = peek(c.io.snoop.response_channel(i).ready)
            val vld = peek(c.io.snoop.response_channel(i).valid)
            val value = peek(c.io.snoop.response_channel(i).bits)
            if(rdy == 1 && vld == 1) {
              curSnoopResp(i) += 1
            }
          }

          step(1)
          // post edge
        }

        expect(reqSent.forall(_ == 1), s"Number of sent requests does not match: $reqSent")
        expect(reqReceived == masterCount, s"Number of received requests does not match: $reqReceived")
        expect(respSent.forall(_ == 1), s"Number of sent responses does not match: $respSent")
        expect(respReceived.forall(_ == 1), s"Number of received responses does not match: $respReceived")
        println(s"$snoopReqQSink")
        expect(snoopReqQSink.forall(p => (0 until masterCount).forall( q => { q == p(q) })),
          s"Snoop requests does not match: $snoopReqQSink")
      }
    } should be(true)
  }
   */
}
