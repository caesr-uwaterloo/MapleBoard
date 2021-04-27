
package core


import chisel3._
import chisel3.util._
import chisel3.iotesters._
import org.scalatest.{Matchers, FlatSpec}

import scala.util.control.Breaks._

import param.{CoreParam, RISCVParam}

class CoreTester(c: CoreGroupAXIWithMemory) extends PeekPokeTester(c) {

  var count = 0
  breakable {
    while (true) {
      count += 1
      step(1)

      if (count > 1000  || peek(c.io.inst) == BigInt("00000073", 16)) {
        break
      }
    }
  }
}

class CoreSpec extends FlatSpec with Matchers {
  behavior of "CoreSpec"

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
    coreID = 0,
    withAXIMemoryInterface = true,
    nCore = 1)

  it should "work" in {
    chisel3.iotesters.Driver(() => new CoreGroupAXIWithMemory(coreParam)) { c =>
      new CoreTester(c)
    } should be(true)
  }
}
