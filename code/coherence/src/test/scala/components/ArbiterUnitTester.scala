// See README.md for license details.

package components

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}
import components.arbiters.WTDMArbiter

import scala.collection.mutable.ArrayBuffer


/*
class WTDMArbiterUnitTester
(c: WTDMArbiter[UInt]) extends PeekPokeTester(c) {
  val totTime: ArrayBuffer[Int] = new ArrayBuffer
  for{i <- 0 until 4} {
    poke(c.io.in(i).bits, i)
    totTime.append(0)
  }

  for{ i <- 0 until 100 } {
    step(1)
    totTime(peek(c.io.chosen).toInt) += 1
  }

  expect(
    totTime.zip(c.weightAlloc).forall(p => {
      p._1 == p._2 * c.slotWidth
    }), "weight allocation is not satisfied"
  )

}

class ArbiterTester  extends ChiselFlatSpec {
  // Hack in the example project
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }

  "WTDMArbiter" should "grant access accrodingly" in {
    val slotWidth = 10
    val nMaster = 4
    val slotAlloc = 1 :: 2 :: 3 :: 4 :: Nil
    iotesters.Driver(() => new WTDMArbiter[UInt](UInt(4.W), nMaster, slotWidth, slotAlloc), "verilator") {
      c => new WTDMArbiterUnitTester(c)
    } should be(true)
  }
}
 */
