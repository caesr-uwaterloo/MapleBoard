
package coherences

import chisel3._
import chisel3.util.Decoupled
import chiseltest._
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation
import chiseltest.legacy.backends.verilator.VerilatorFlags
import components.{AMOALUOP, CacheReq, CacheResp, ErrorMessage, MemToAXI4, MemorySubsystem}
import _root_.core.AXIMemory
import fixtures.TargetPlatformPMESI
import param.CoreParam
import params.MemorySystemParams
import chisel3.experimental.BundleLiterals._
import _root_.core.{AMOOP, MemoryRequestType}

import scala.collection.mutable.ArrayBuffer

class PMESIReadWriteSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "PMESI read write"
  it should "give quadratic bound" in {
    val memParam = TargetPlatformPMESI.memorySystemParams.copy(masterCount = 4)
    test(new BareMemorySubsystem(
      TargetPlatformPMESI.coreParam,
      memParam)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=0")))) { c =>
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
      val reqToPokeWrite = chiselTypeOf(c.io.core.request_channel(0).bits).Lit(
        _.address -> 0.U,
        _.amo_alu_op -> AMOOP.none,
        _.data -> "hdeadbeefdeadbeef".U,
        _.length -> 3.U,
        _.mem_type -> MemoryRequestType.write,
        _.is_amo -> false.B,
        _.flush  -> false.B,
        _.llcc_flush -> false.B,
        _.aq -> 0.U,
        _.rl -> 0.U)
      def getReqToPoke(address: Int, read_write: String): CacheReq = {
        val rw = read_write match {
          case "read" => MemoryRequestType.read
          case _ => MemoryRequestType.write
        }
        chiselTypeOf(c.io.core.request_channel(0).bits).Lit(
          _.address -> address.U,
          _.amo_alu_op -> AMOOP.none,
          _.data -> "hdeadbeefdeadbeef".U,
          _.length -> 3.U,
          _.mem_type -> rw,
          _.is_amo -> false.B,
          _.flush  -> false.B,
          _.llcc_flush -> false.B,
          _.aq -> 0.U,
          _.rl -> 0.U)
      }
      val timeout = 40000000
      c.clock.setTimeout(timeout)
      c.clock.step()
      var enqTime = (for { _ <- 0 until masterCount } yield ArrayBuffer.empty[Int]).toArray
      var deqTime = (for { _ <- 0 until masterCount } yield ArrayBuffer.empty[Int]).toArray
      val nReq = 20000
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
      val read_write = Array("read", "write")
      val complex = (for { (a, r) <- addr.zip(read_write) } yield (a, r)).toArray
      while (!checkEnd()) {
        for {i <- 0 until masterCount } {
          val res = doEnq(i, timeNow, nReq)
          if(res) {
            val idx = r.nextInt(complex.length)
            val (a, rw) = complex(idx)
            c.io.core.request_channel(i).bits.poke(getReqToPoke(a, rw))
          }
          doDeq(i, timeNow, nReq)
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
    }
  }
}
