
package components

import chisel3._
import chisel3.util._
import chisel3.iotesters.{ChiselFlatSpec, Driver, OrderedDecoupledHWIOTester, PeekPokeTester}

/*
class HashTableUnitTester extends OrderedDecoupledHWIOTester {
// enable_all_debug = true
// OrderedDecoupledHWIOTester.max_tick_count = 20
val keyW = 18
val dep = 4
val valW = 24
val typeCount : Int = 1 << HashTableReqType.sHASH_TABLE_REMOVE.getWidth
override val device_under_test = Module(new HashTable(dep, UInt(keyW.W), UInt(valW.W)))
rnd.setSeed(0)


def inputEvent(poke: HashTableReq[UInt, UInt]): Unit = {
  super.inputEvent(
    device_under_test.io.request.bits.req_type -> poke.req_type.litValue(),
    device_under_test.io.request.bits.req_data.key -> poke.req_data.key.litValue(),
    device_under_test.io.request.bits.req_data.value -> poke.req_data.value.litValue(),
  )
}

def outputEvent(expect: HashTableResp[UInt, UInt]) : Unit = {
  super.outputEvent(
    device_under_test.io.response.bits.req_type -> expect.req_type.litValue(),
    device_under_test.io.response.bits.req_data.key -> expect.req_data.key.litValue(),
    device_under_test.io.response.bits.req_data.value -> expect.req_data.value.litValue(),
    device_under_test.io.response.bits.ack -> expect.ack.litValue()
  )
}

}
*/

class HashTableTester extends ChiselFlatSpec {
  // Hack in the example project
  private val backendNames = if(firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }

  /*
  "Empty HashTable" should "not respond ack" in {
    assertTesterPasses { new HashTableUnitTester() {
      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_READ, genKey=1.U, genValue =0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_READ, genKey=1.U, genValue=0.U, ack=0.U) )
    }}
  }
  "HashTable" should "read written key" in {
    assertTesterPasses { new HashTableUnitTester() {
      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, 5.U, 2.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, 5.U, 2.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_READ, 5.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_READ, 5.U, 2.U, 1.U) )
    }}
  }

  it should "remove key" in {
    assertTesterPasses { new HashTableUnitTester() {
      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, 5.U, 2.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, 5.U, 2.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_READ, 5.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_READ, 5.U, 2.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_REMOVE, 5.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_REMOVE, 5.U, 0.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_READ, 5.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_READ, 5.U, 0.U, 0.U) )
    }}
  }

  it should "update (remove then write) key" in {
    assertTesterPasses { new HashTableUnitTester() {
      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, 6.U, 2.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, 6.U, 2.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_REMOVE, 6.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_REMOVE, 6.U, 0.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, 6.U, 100.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, 6.U, 100.U, 1.U) )

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_READ, 6.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_READ, 6.U, 100.U, 1.U) )
    }}
  }

  it should "hold written key" in {
    assertTesterPasses { new HashTableUnitTester() {
      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, 6.U, 2.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, 6.U, 2.U, 1.U) )
      for {i <- 0 until 6} {
        inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, i.U, i.U) )
        outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, i.U, i.U, 1.U) )

        inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_REMOVE, i.U, 0.U) )
        outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_REMOVE, i.U, 0.U, 1.U) )
      }

      inputEvent( HashTableReq(HashTableReqType.sHASH_TABLE_READ, 6.U, 0.U) )
      outputEvent( HashTableResp(HashTableReqType.sHASH_TABLE_READ, 6.U, 2.U, 1.U) )
    }}
  }

  it should "not write when full" in {
    assertTesterPasses { new HashTableUnitTester() {
      for { i <- 0 until dep} {
        inputEvent(HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, i.U, 2.U))
        outputEvent(HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, i.U, 2.U, 1.U))
      }
      inputEvent(HashTableReq(HashTableReqType.sHASH_TABLE_WRITE, dep.U, 2.U))
      // This event will not happen since not request will be accepted
      outputEvent(HashTableResp(HashTableReqType.sHASH_TABLE_WRITE, 5.U, 2.U, 0.U))
    }}
  }
   */

}
