
package testutil

import chisel3._
import chisel3.util._
import components.{CacheReq, CacheResp}
import params.MemorySystemParams
import chiseltest._
import scala.collection.mutable.ArrayBuffer
import chisel3.experimental.BundleLiterals._

case class RequestSendConfig(interval: Int, timeout: Int)

object RequestManager {

}
// Drive the (core) request for the design under test and keep tracks of it
class RequestManager(val dut: DUT, val m: MemorySystemParams) {
  var memory = scala.collection.mutable.Map[Int, BigInt]()
  type Line = BigInt
  private def getWord(line: Line, idx: Int): BigInt =  {
    val word = (line >> (idx * 64)) & BigInt("ffffffffffffffff", 16)
    word
  }

  // useful for checking DVI
  def checkLineMatch(startAddr: Int,
                     lineData: BigInt): Unit = {
    // lineData is a 512 bit cacheline
    for { word_idx <- 0 until 8 } {
      val word = getWord(lineData, word_idx)
      val addr = startAddr + word_idx * 8 // byte addresses
      memory.get(addr) match {
        case Some(data) => assert(data == word, "Modified data should match")
        case None => assert(word == 0, "Unmodified data should match")
      }
    }
  }
  def enrollMemory(startAddr: Int, lineData: BigInt): Unit = {
    for { word_idx <- 0 until 8 } {
      val word = getWord(lineData, word_idx)
      val addr = startAddr + word_idx * 8
      memory(addr) = word
    }
  }

  def channelFire[T <: Data](decoupled: DecoupledIO[T]) : Boolean = {
    decoupled.valid.peek().litToBoolean && decoupled.ready.peek().litToBoolean
  }

  // scalastyle:off
  def testSendRecvRequest(
                    req: Map[Int, List[CacheReq]],
                    requestSendConfig: RequestSendConfig
                  ) : scala.collection.mutable.Map[Int, scala.collection.mutable.ArrayBuffer[(Int, BigInt)]] = {
    var untilNextSend = ArrayBuffer.fill(m.masterCount)(0)
    var currentRequestToSend = ArrayBuffer.fill(m.masterCount)(0)
    var currentResponseToRecv = ArrayBuffer.fill(m.masterCount)(0)
    var resp: scala.collection.mutable.Map[Int,
    scala.collection.mutable.ArrayBuffer[(Int, BigInt)]
    ]  = scala.collection.mutable.Map(0 until m.masterCount map { i => (i, collection.mutable.ArrayBuffer[(Int, BigInt)]()) } toList : _*)
    var maxLatency = ArrayBuffer.fill(m.masterCount)(0)
    var latencyCounter = ArrayBuffer.fill(m.masterCount)(0)
    def allSent = 0.until(m.masterCount).forall(i => currentRequestToSend(i) == req(i).length)
    def allRecv = 0.until(m.masterCount).forall(i => currentResponseToRecv(i) == req(i).length)
    dut.clock.setTimeout(requestSendConfig.timeout)
    for { i <- 0 until m.masterCount } {
      dut.io.core.response_channel(i).ready.poke(true.B)
    }
    while(!(allSent && allRecv)) {

        val reqFire = for { i <- 0 until m.masterCount } yield {
        if(channelFire(dut.io.core.response_channel(i))) {
          maxLatency(i) = maxLatency(i).max(latencyCounter(i))
          currentResponseToRecv(i) += 1
          val k: collection.mutable.ArrayBuffer[(Int, BigInt)]  = resp(i)
          val addr = dut.io.core.response_channel(i).bits.address.peek().litValue()
          val data = dut.io.core.response_channel(i).bits.data.peek().litValue()
          val lat = dut.io.core.response_channel(i).bits.latency.peek().litValue()
          // System.err.print(s"${i} Lat: ")
          // System.err.println(lat)
          k.+=((addr.toInt, data))
          assert(addr == req(i)(currentResponseToRecv(i) - 1).address.litValue())
        }

        if(currentRequestToSend(i) != req(i).length && untilNextSend(i) == 0 &&
          currentRequestToSend(i) == currentResponseToRecv(i)
        ) {
          dut.io.core.request_channel(i).bits.poke(req(i)(currentRequestToSend(i)))
          dut.io.core.request_channel(i).valid.poke(true.B)
        }
        channelFire(dut.io.core.request_channel(i))
      }
      // println("TO SEND", currentRequestToSend)
      // println("TO RECV", currentResponseToRecv)
      dut.clock.step()
      for { i <- 0 until m.masterCount } {
        latencyCounter(i) += 1
        if (reqFire(i)) {
          latencyCounter(i) = 0
          currentRequestToSend(i) += 1
          dut.io.core.request_channel(i).valid.poke(false.B)
          untilNextSend(i) = requestSendConfig.interval
        }
        if(currentRequestToSend(i) == currentResponseToRecv(i)) {
          dut.io.core.response_channel(i).ready.poke(false.B)
        } else {
          dut.io.core.response_channel(i).ready.poke(true.B)
        }
      }
      0 until m.masterCount foreach { i => untilNextSend(i) = 0.max(untilNextSend(i) - 1) }
    }
    System.err.println("=== WCL Beg ===")
    for { i <- 0 until m.masterCount } {
      System.err.print(s"${i}: ")
      System.err.println(maxLatency(i))
    }
    System.err.println("=== WCL End ===")
    resp
  }

}
