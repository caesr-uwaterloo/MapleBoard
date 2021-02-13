
package coherences

import _root_.core.{AMOOP, MemoryRequestType}
import chisel3._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import chiseltest.legacy.backends.verilator.VerilatorFlags
import components.CacheReq
import fixtures.TargetPlatformPMSI
import org.scalatest._
import _root_.utils.BundleLitAsBigIntHelper
import testutil.{RequestManager, RequestSendConfig}
import utils._

import scala.collection.mutable.ArrayBuffer

class PipelinedPMSIReadWriteSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "PipelinedPMSI read write"
  it should "give quadratic bound" in {
    // setup the coherence table
    val fn = TargetPlatformPMSI.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = TargetPlatformPMSI.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    val memParam = TargetPlatformPMSI.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformPMSI.coreParam,
      memParam,
      TargetPlatformPMSI.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // initialize the requests
      // (Core, Latency)

      for{ i <- 0 until masterCount } {
        c.io.core.request_channel(i).initSource().setSourceClock(c.clock)
        c.io.core.response_channel(i).initSink().setSinkClock(c.clock)
      }
      // initialize first for serveral cycles
      c.clock.step(129)
      // now construct the requests
      val reqToPokeWrite = testutil.constructRequest(c,
        0, BigInt("deadbeefdeadbeef", 16), MemoryRequestType.write)
      val timeout = 500000
      c.clock.setTimeout(timeout)
      c.clock.step()
      var enqTime = (for { _ <- 0 until masterCount } yield ArrayBuffer.empty[Int]).toArray
      var deqTime = (for { _ <- 0 until masterCount } yield ArrayBuffer.empty[Int]).toArray
      val nReq = 1000
      def doEnq(coreId: Int, ticks: Int, nreq: Int): Boolean = {
        // part 1 send the request
        if(c.io.core.request_channel(coreId).ready.peek().litToBoolean &&
          c.io.core.request_channel(coreId).valid.peek().litToBoolean &&
          enqTime(coreId).length < nreq) {
          println(s"At cycle ${ticks}, Core ${coreId}, sent a request")
          enqTime(coreId).append(ticks)
          true
        } else {
          false
        }
      }
      def doDeq(coreId: Int, ticks: Int, nreq: Int): Unit = {
        // part 1 send the request
        if(c.io.core.response_channel(coreId).ready.peek().litToBoolean &&
          c.io.core.response_channel(coreId).valid.peek().litToBoolean &&
          deqTime(coreId).length < nreq) {
          println(s"At cycle ${ticks}, Core ${coreId}, received a response")
          deqTime(coreId).append(ticks)
        }
      }

      def checkEnd(): Boolean = {
        enqTime.forall(_.length == nReq) && deqTime.forall(_.length == nReq)
      }
      for { i <- 0 until masterCount } {
        c.io.core.request_channel(i).valid.poke(true.B)
        c.io.core.request_channel(i).bits.poke(reqToPokeWrite)
        c.io.core.response_channel(i).ready.poke(true.B)
      }
      val r = new scala.util.Random(42)
      var timeNow = 0
      val addr = Array(0x00, 0x40, 0x400, 0x440)
      val read_write = Array(MemoryRequestType.read, MemoryRequestType.write)
      val complex = (for { (a, r) <- addr.zip(read_write) } yield (a, r)).toArray
      var coveredTransitions = scala.collection.mutable.Set.apply[BigInt]()
      while (!checkEnd()) {
        for {i <- 0 until masterCount } {
          val res = doEnq(i, timeNow, nReq)
          if(res) {
            val idx = r.nextInt(complex.length)
            val (a, rw) = complex(idx)
            c.io.core.request_channel(i).bits.poke(
              testutil.constructRequest(c, a, BigInt("deadbeefdeadbeef", 16), rw)
            )
          }
          doDeq(i, timeNow, nReq)
        }
        // check for coverage
        for { i <- 0 until masterCount } {
          if(c.io.query_coverage(i).valid.peek.litToBoolean) {
            coveredTransitions.add(c.io.query_coverage(i).bits.peek().litValue())
          }
        }
        c.clock.step()
        timeNow += 1
      }
      var latencies = ArrayBuffer.empty[Int]
      println("===== FINAL RESULTS =====")

      for { i <- 0 until masterCount} {
        print(s"Core: ${i}: ")

        for { a <- enqTime(i).zipWithIndex } {
          val (startTime, idx) = a
          val endTime = deqTime(i)(idx)
          print(s" Request#${idx}, Time: ${endTime - startTime} ")
          latencies.append(endTime - startTime)
        }
        println("")
      }
      println(s"===== Worst Case Latency =====\n")
      println(s"WCL: ${latencies.max}\n")

      println(s"===== Port WCL Stats =====\n")
      for { i <- 0 until masterCount } {
        println(s"Core ${i}: ${c.io.latency(i).peek().litValue()}\n")
      }

      val nonerr = TargetPlatformPMSI.coherenceSpec.getPrivateCacheTable.filter(!_._2.isErr)
      val table = nonerr.keySet.map(_.toBigInt)
      // unspecified test is fine
      println(s"This test Covered ${coveredTransitions.size} / ${table.size} transitions (${coveredTransitions.size.toFloat / table.size}) (Not counting errors)")
      for { (q, a) <- nonerr} {
        if(!coveredTransitions.contains(q.toBigInt)) {
          println(s"Uncovered Case: ${q}")
        }
      }
    }
  }
}


