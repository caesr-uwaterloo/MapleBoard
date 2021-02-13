
package components

import chiseltest._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import chiseltest.legacy.backends.verilator.VerilatorFlags
import chiseltest.experimental.TestOptionBuilder._
import components._
import chisel3._
import org.scalatest.{FlatSpec, Matchers}
import chisel3.experimental.BundleLiterals._

object PRLUTSpec {
  import PendingRequestLookupTable._
  // scalastyle:off
  implicit class PendingRequestLookupTableSWInterface[B <: Data](c: PendingRequestLookupTable[_ <: Data, _ <: Data, B]) {
    def expectInsert(tag: Int, requestor: Int, requestType: B, data: Int,
                     res: PendingReqeustLookupTableResponseType.Type): Unit = {
      c.io.requestChannel.enqueueNow(chiselTypeOf(c.io.requestChannel.bits).Lit(
        _.requestType -> requestType, _.requestTypePRLUT -> PendingReqeustLookupTableRequestType.Insert,
        _.queryAndRemove -> false.B, _.tag -> tag.U,
        _.requestor -> requestor.U,  _.data -> data.U
      ))
      c.io.responseChannel.ready.poke(true.B)
      while(!c.io.responseChannel.valid.peek().litToBoolean) {
        c.clock.step()
      }
      c.io.responseChannel.bits.responseType.expect(res)
      c.clock.step()
      c.io.responseChannel.ready.poke(false.B)
      // c.io.responseChannel.expectDequeue(chiselTypeOf(c.io.responseChannel.bits).Lit(
      //   _.responseType -> res
      // ))
    }

    def expectDeq(tag: Int, requestor: Int, requestType: B, data: Int, remove: Boolean,
                  req: PendingReqeustLookupTableRequestType.Type,
                  resp: PendingReqeustLookupTableResponseType.Type): Unit = {
      c.io.requestChannel.bits.requestTypePRLUT.poke(req)
      c.io.requestChannel.bits.queryAndRemove.poke(remove.B)
      c.io.requestChannel.bits.tag.poke(tag.U)
      c.io.requestChannel.bits.requestor.poke(requestor.U)
      c.io.requestChannel.valid.poke(true.B)
      while (!c.io.requestChannel.ready.peek().litToBoolean) {
        c.clock.step()
      }
      c.clock.step()
      c.io.requestChannel.valid.poke(false.B)

      if(resp.litValue() == PendingRequestLookupTable.PendingReqeustLookupTableResponseType.Found.litValue() ||
        resp.litValue() == PendingRequestLookupTable.PendingReqeustLookupTableResponseType.FoundAndRemoved.litValue()
      ) {
        c.io.responseChannel.expectDequeue(chiselTypeOf(c.io.responseChannel.bits).Lit(
          _.data -> data.U, _.tag -> tag.U, _.requestor -> requestor.U, _.requestType -> requestType,
          _.responseType -> resp
        ))
      } else {
        c.io.responseChannel.ready.poke(true.B)
        while(!c.io.responseChannel.valid.peek().litToBoolean) {
          c.clock.step()
        }
        c.io.responseChannel.bits.responseType.expect(resp)
        c.clock.step()
        c.io.responseChannel.ready.poke(false.B)
      }
    }

    def expectNextGlobal(tag: Int, requestor: Int, requestType: B, data: Int, remove: Boolean,
                         resp: PendingReqeustLookupTableResponseType.Type
                        ): Unit = {
      expectDeq(tag, requestor, requestType, data, remove,
        PendingReqeustLookupTableRequestType.NextRequestInGlobalOrder, resp)
    }

    def expectNextAddress(tag: Int, requestor: Int, requestType: B, data: Int, remove: Boolean,
                          resp: PendingReqeustLookupTableResponseType.Type
                         ): Unit = {
      expectDeq(tag, requestor, requestType, data, remove,
        PendingReqeustLookupTableRequestType.NextRequestGivenAddress, resp)
    }

    def expectNextSlot(tag: Int, requestor: Int, requestType: B, data: Int, remove: Boolean,
                       resp: PendingReqeustLookupTableResponseType.Type
                      ): Unit = {
      expectDeq(tag, requestor, requestType, data, remove,
        PendingReqeustLookupTableRequestType.NextRequestGivenSlot, resp)
    }

    def setupDesign(): Unit = {
      c.io.requestChannel.initSource().setSourceClock(c.clock)
      c.io.responseChannel.initSink().setSinkClock(c.clock)
      while (!c.io.requestChannel.ready.peek().litToBoolean) {
        c.clock.step()
      }
    }
  }
}

class PRLUTSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  import PRLUTSpec._
  import PendingRequestLookupTable._
  behavior of "new PRLUT"
  val memParam = fixtures.TargetPlatformPMSI.memorySystemParams
  val cohSpec = fixtures.TargetPlatformPMSI.coherenceSpec
  // val resType = PendingRequestLookupTable.PendingReqeustLookupTableResponseType.type
  it should "insert and read and remove for same address" in {
    test(new PendingRequestLookupTable(memParam, cohSpec)) { c =>
      c.setupDesign()
      for { i <- 0 until memParam.masterCount } {
        c.expectInsert(0x123, i, BusRequestType.GetM, 0,
          PendingReqeustLookupTableResponseType.Success)
      }
      c.expectNextAddress(0x123, 0, BusRequestType.GetM, 0, false,
        PendingReqeustLookupTableResponseType.Found)
      for { i <- 0 until memParam.masterCount } {
        c.expectNextSlot(0x123, i, BusRequestType.GetM, 0, true,
          PendingReqeustLookupTableResponseType.FoundAndRemoved)
      }
      for { i <- 0 until memParam.masterCount } {
        c.expectNextAddress(0x123, i, BusRequestType.GetM, 0, false,
          PendingReqeustLookupTableResponseType.NotFound)
      }
    }
  }

  it should "insert and read and write for different address" in {
    test(new PendingRequestLookupTable(memParam, cohSpec)) { c =>
      c.setupDesign()
      for { i <- 0 until memParam.masterCount } {
        c.expectInsert(i, i, BusRequestType.GetM, 0,
          PendingReqeustLookupTableResponseType.Success)
      }
      for { i <- 0 until memParam.masterCount } {
        c.expectNextAddress(i, i, BusRequestType.GetM, 0, false,
          PendingReqeustLookupTableResponseType.Found)
      }
      for { i <- 0 until memParam.masterCount } {
        c.expectNextAddress(i, i, BusRequestType.GetM, 0, true,
          PendingReqeustLookupTableResponseType.FoundAndRemoved)
      }
      for { i <- 0 until memParam.masterCount } {
        c.expectNextAddress(i, i, BusRequestType.GetM, 0, false,
          PendingReqeustLookupTableResponseType.NotFound)
      }
    }
  }

  it should "insert and read and write for global order" in {
    test(new PendingRequestLookupTable(memParam, cohSpec)) { c =>
      c.setupDesign()
      // NOTE: set the size of global buffer to be m.masterCount
      // for this test
      for {
        i <- 0 until memParam.masterCount
      } {
        val j = memParam.masterCount - i - 1
        c.expectInsert(i, j, BusRequestType.GetS, 0,
          PendingReqeustLookupTableResponseType.Success)
      }
      for {
        i <- 0 until memParam.masterCount
      } {
        val j = memParam.masterCount - i - 1
        c.expectNextGlobal(i, j, BusRequestType.GetS, 0, true,
          PendingReqeustLookupTableResponseType.FoundAndRemoved)
      }
    }
  }
}
