
package coherences

import _root_.core.MemoryRequestType
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import chiseltest.legacy.backends.verilator.VerilatorFlags
import components._
import fixtures.TargetPlatformCARPArbiter
import org.scalatest.{FlatSpec, Matchers}
import testutil.{RequestManager, RequestSendConfig}

class PipelinedCARPArbiterReplacementSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "PipelinedCARPArbiter"
  /** checks whether the DVI is violated, based the request and response */
  def checkDVI(c: PipelinedBareMemorySubsystem[_, _, _],
               memory: collection.mutable.Map[Int, Long],
               lineData: BigInt
              ): Unit = {
    val masterCount = c.io.core.request_channel.size
    // track the response etc
  }
  type DUT = PipelinedBareMemorySubsystem[_, _, _]
  // construct request. note that the address should be less
  it should "handle replacement correctly for shared everything" in {
    val memParam = TargetPlatformCARPArbiter.memorySystemParams
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformCARPArbiter.coreParam,
      memParam,
      TargetPlatformCARPArbiter.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      // what if we dont do this initialization
      // testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040)
      val rws: List[MemoryRequestType.Type] = List(MemoryRequestType.read, MemoryRequestType.write)
      // 64bit data
      val data: List[BigInt] = List("12345678abcdcdef", "baaaaaaddeadbeef", "beefbeef00000000").map(BigInt(_, 16))
      val request_matrix = (for {
        addr <- addrs
        rws <- rws
        datum <- data
      } yield {
        (addr, rws, datum)
      }).toList
      val totalRequestPerCore = 20
      // maybe send the request after one's request is broadcasted on the bus
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, ((0 until totalRequestPerCore).map(_ => {
          val (addr, rws, datum) = request_matrix(r.nextInt(request_matrix.length))
          if(memParam.getCritFromID(i).litValue() == 5) {
            // LoCrit core only read shared data
            testutil.constructRequest(c, addr, datum, MemoryRequestType.read)
          } else {
            testutil.constructRequest(c, addr, datum, rws)
          }
        })).toList)
      }).toList
      val req = Map(requests: _*)
      val requestManager = new RequestManager(c, memParam)
      requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 25000))
    }
  }
  /*
  it should "show unbounded latency for E cores" in {
    val memParam = TargetPlatformCARP.memorySystemParams
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformCARP.coreParam,
      memParam,
      TargetPlatformCARP.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      // what if we dont do this initialization
      // testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40)
      val rws: List[MemoryRequestType.Type] = List(MemoryRequestType.read, MemoryRequestType.write)
      // 64bit data
      val data: List[BigInt] = List("12345678abcdcdef", "baaaaaaddeadbeef", "beefbeef00000000").map(BigInt(_, 16))
      val request_matrix = (for {
        addr <- addrs
        rws <- rws
        datum <- data
      } yield {
        (addr, rws, datum)
      }).toList
      val totalRequestPerCore = 300
      // maybe send the request after one's request is broadcasted on the bus
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, ((0 until totalRequestPerCore).map(rr => {
          val (addr, rws, datum) = request_matrix(r.nextInt(request_matrix.length))
          if(i == memParam.masterCount - 1) {
            // LoCrit core only read shared data
            testutil.constructRequest(c, addr, datum, MemoryRequestType.read)
          } else {
            testutil.constructRequest(c, addr, datum, rws)
          }
        })).toList)
      }).toList
      val req = Map(requests: _*)
      val requestManager = new RequestManager(c, memParam)
      requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 2000000 /* 160000 */))
    }
  }
  */
  /*
  it should "handle replacement correctly for shared everything atomic" in {

    val memParam = TargetPlatformPMSI.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformPMSI.coreParam,
      memParam,
      TargetPlatformPMSI.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40)// List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
      val rws: List[MemoryRequestType.Type] = List(MemoryRequestType.amo/*, MemoryRequestType.read*/)
      // 64bit data
      val data: List[BigInt] = List("1").map(BigInt(_, 16))
      val request_matrix = (for {
        addr <- addrs
        rws <- rws
        datum <- data
      } yield {
        (addr, rws, datum)
      }).toList
      val totalRequestPerCore = 400
      var maxVal = collection.mutable.Map[Int, BigInt]()
      // maybe send the request after one's request is broadcasted on the bus
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, ((0 until totalRequestPerCore).map(_ => {
          val (addr, rws, datum) = request_matrix(r.nextInt(request_matrix.length))
          if(rws.litValue() === MemoryRequestType.amo.litValue()) {
            val last: BigInt = maxVal.getOrElse(addr, BigInt(0))
            maxVal.update(addr, last + 1)
            testutil.constructRequest(c, addr, datum, rws, AMOOP.add)
          } else {
            testutil.constructRequest(c, addr, datum, rws)
          }
        })).toList)
      }).toList
      val req = Map(requests: _*)
      val requestManager = new RequestManager(c, memParam)
      val res = requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 2400000))
      val resAddrRes = res.foldLeft(collection.mutable.ArrayBuffer[(Int, BigInt)]()) { (prev, next) => prev ++ next._2 }
      val resMap = resAddrRes.groupBy(_._1).map { c => (c._1, c._2.map(_._2)) }.map { c => (c._1, c._2.max)}.toMap
      // println("SW Value: ", maxVal)
      // println("HW Value: ", resMap)
      // println("Raw Value: \n")
      // for { (k, v) <- res } {
      //   val req = requests(k)._2.map { r => if (r.amo_alu_op.litValue() ==  AMOOP.add.litValue()) { "add" } else { "read" } }
      //   println(">>>", k, v.zip(req))
      // }
      for { (addr, mx) <- maxVal} {
        val got = resMap.get(addr).get
        assert(mx == got + 1 || mx == got)
      }
    }
  }
  it should "handle replacement correctly for shared everything lr/sc" in {
    val fn = TargetPlatformPMSI.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = TargetPlatformPMSI.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    val memParam = TargetPlatformPMSI.memorySystemParams.copy(masterCount = 8)
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformPMSI.coreParam,
      memParam,
      TargetPlatformPMSI.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
      // Now do some lr/screquests
      val totalRequestPerCore = 1200
      var swValue = collection.concurrent.TrieMap[Int, collection.mutable.Map[Int, BigInt]]()
      var hwValue = collection.concurrent.TrieMap[Int, collection.mutable.Map[Int, BigInt]]()
      for { i <- 0 until masterCount } {
        swValue.update(i, collection.mutable.Map[Int, BigInt]())
        hwValue.update(i, collection.mutable.Map[Int, BigInt]())
      }

      c.clock.setTimeout(200000)
      ((0 until masterCount).foldLeft(fork {}) { (prev, id) =>
        prev.fork {
          for { i <- 0 until totalRequestPerCore } {
            val addr = addrs(r.nextInt(addrs.length))
            val lrreq = testutil.constructRequest(c, addr, BigInt(0), MemoryRequestType.lr, AMOOP.lr)
            assert(lrreq.is_amo.litToBoolean)
            c.io.core.request_channel(id).enqueue(lrreq)

            def vld = c.io.core.response_channel(id).valid.peek().litToBoolean

            def rdy = c.io.core.response_channel(id).ready.peek().litToBoolean

            def fire = vld && rdy

            c.io.core.response_channel(id).ready.poke(true.B)
            while (!fire) {
              c.clock.step()
            }
            var respData = c.io.core.response_channel(id).bits.data.peek().litValue()
            var respAddr = c.io.core.response_channel(id).bits.address.peek().litValue()
            val hd = hwValue(id).getOrElse(respAddr.toInt, BigInt(0))
            hwValue(id).update(respAddr.toInt, respData)
            assert(hd <= respData)
            assert(addr == respAddr)
            c.clock.step()
            c.io.core.response_channel(id).ready.poke(false.B)
            val screq = testutil.constructRequest(c, addr, respData + 1, MemoryRequestType.sc, AMOOP.sc)
            assert(screq.is_amo.litToBoolean)
            c.io.core.request_channel(id).enqueue(screq)
            c.io.core.response_channel(id).ready.poke(true.B)
            while (!fire) {
              c.clock.step()
            }
            respData = c.io.core.response_channel(id).bits.data.peek().litValue()
            respAddr = c.io.core.response_channel(id).bits.address.peek().litValue()
            assert(addr == respAddr)
            if (respData == 0) { // succ
              val sd = swValue(id).getOrElse(respAddr.toInt, BigInt(0))
              swValue(id).update(respAddr.toInt, sd + 1)
            }
            c.clock.step()
            c.io.core.response_channel(id).ready.poke(false.B)
            c.clock.step(30)
          }
        }
      }).join()
      println("LR Values: ", hwValue)
      println("SC Values: ", swValue)
      def convertToPerAddress(x: collection.concurrent.TrieMap[Int, collection.mutable.Map[Int, BigInt]],
                              op: (BigInt, BigInt) => BigInt):
      Map[Int, BigInt] =
        x.foldLeft(Seq[(Int, BigInt)]())({ (r, v) => r ++ v._2.toSeq }).groupBy(_._1).map { k =>
        (k._1, k._2.map(_._2).reduce(op))
      }.toMap
      val swValuePerAddress = convertToPerAddress(swValue, op = (a: BigInt, b: BigInt) => a + b)
      val hwValuePerAddress = convertToPerAddress(hwValue, op = (a: BigInt, b: BigInt) => a.max(b))
      assert(swValuePerAddress.size == hwValuePerAddress.size)
      for { (k, v) <- swValuePerAddress } {
        val hv = hwValuePerAddress.get(k).get
        assert(v == hv + 1)
      }
    }
  }
  it should "handle replacement correctly for shared everything atomic word" in {
    val fn = TargetPlatformPMSI.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = TargetPlatformPMSI.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    println(s"The Shared Cache Coherence Table is at ${fns}")
    val memParam = TargetPlatformPMSI.memorySystemParams.copy(masterCount = 8)
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformPMSI.coreParam,
      memParam,
      TargetPlatformPMSI.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40, 0x44,
        0x440, 0x444,
        0x840, 0x844,
        0xc40, 0xc44,
        0x1040, 0x1044,
        0x1440, 0x1444)// List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
      val rws: List[MemoryRequestType.Type] = List(MemoryRequestType.amo, MemoryRequestType.read)
      // 64bit data
      val data: List[BigInt] = List("1").map(BigInt(_, 16))
      val request_matrix = (for {
        addr <- addrs
        rws <- rws
        datum <- data
      } yield {
        (addr, rws, datum)
      }).toList
      val totalRequestPerCore = 400
      var maxVal = collection.mutable.Map[Int, BigInt]()
      // maybe send the request after one's request is broadcasted on the bus
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, ((0 until totalRequestPerCore).map(_ => {
          val (addr, rws, datum) = request_matrix(r.nextInt(request_matrix.length))
          if(rws.litValue() === MemoryRequestType.amo.litValue()) {
            val last: BigInt = maxVal.getOrElse(addr, BigInt(0))
            maxVal.update(addr, last + 1)
            testutil.constructRequest(c, addr, datum, rws, AMOOP.add, length=2)
          } else {
            testutil.constructRequest(c, addr, datum, rws, length=2)
          }
        })).toList)
      }).toList
      val req = Map(requests: _*)

      val requestManager = new RequestManager(c, memParam)
      val res = requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 2400000))
      val resAddrRes = res.foldLeft(collection.mutable.ArrayBuffer[(Int, BigInt)]()) { (prev, next) => prev ++ next._2 }
      val resMap = resAddrRes.groupBy(_._1).map { c => (c._1, c._2.map(_._2)) }.map { c => (c._1, c._2.max)}.toMap
      // println("SW Value: ", maxVal)
      // println("HW Value: ", resMap)
      // println("Raw Value: \n")
      // for { (k, v) <- res } {
      //   val req = requests(k)._2.map { r => if (r.amo_alu_op.litValue() ==  AMOOP.add.litValue()) { "add" } else { "read" } }
      //   println(">>>", k, v.zip(req))
      // }
      for { (addr, mx) <- maxVal} {
        val got = resMap.get(addr).get
        assert(mx == got + 1 || mx == got)
      }
    }
  }
  it should "handle wstrb" in {
    val fn = TargetPlatformPMSI.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = TargetPlatformPMSI.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    val memParam = TargetPlatformPMSI.memorySystemParams.copy(masterCount = 2)
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformPMSI.coreParam,
      memParam,
      TargetPlatformPMSI.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      // what if we dont do this initialization
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val wrReq = testutil.constructRequest(c, 0x40, 0x1, memType = MemoryRequestType.write, use_strb = true, strb = 0x01)
      val wrReq2 = testutil.constructRequest(c, 0x40, 0x200000000L, memType = MemoryRequestType.write, use_strb = true, strb = 0x10)
      val rdReq = testutil.constructRequest(c, 0x40, 0x1, memType = MemoryRequestType.read)
      c.io.core.request_channel(0).enqueue(wrReq)
      c.io.core.response_channel(0).ready.poke(true.B)
      while(!c.io.core.response_channel(0).valid.peek().litToBoolean) {
        c.clock.step()
      }
      c.clock.step()
      c.io.core.request_channel(0).enqueue(wrReq2)
      c.io.core.response_channel(0).ready.poke(true.B)
      while(!c.io.core.response_channel(0).valid.peek().litToBoolean) {
        c.clock.step()
      }
      c.clock.step()

      c.io.core.request_channel(1).enqueue(rdReq)

      c.io.core.response_channel(1).ready.poke(true.B)
      while(!c.io.core.response_channel(1).valid.peek().litToBoolean) {
        c.clock.step()
      }
      assert(c.io.core.response_channel(1).bits.data.peek().litValue() == 0x200000001L)
      c.clock.step()
    }
  }
  it should "handle be bounded for one reading and another requesting" in {
    val memParam = TargetPlatformPMSI.memorySystemParams.copy(masterCount = 3)
    test(new PipelinedBareMemorySubsystem(
      TargetPlatformPMSI.coreParam,
      memParam,
      TargetPlatformPMSI.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      var maxVal = collection.mutable.Map[Int, BigInt]()
      // maybe send the request after one's request is broadcasted on the bus
      val w0 = testutil.constructRequest(c, 0x00, 0, MemoryRequestType.write)
      val w4 = testutil.constructRequest(c, 0x40, 0, MemoryRequestType.write)
      val w8 = testutil.constructRequest(c, 0x80, 0, MemoryRequestType.write)
      val wc = testutil.constructRequest(c, 0xc0, 0, MemoryRequestType.write)
      // the WC will be within 2 * 3 * 3 * 128 + 128 ~= 2500
      // anything that is too off from it is problematic
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, if(i == 0) {
          List(w0) ++ List.fill(4096) { w4 }
        } else if(i == 1) {
          List(
            w8, w8, w8, w8, w8, w0
          )
        } else { // not using it
          List(
            wc, wc
          )
        })
      }).toList
      val req = Map(requests: _*)
      val requestManager = new RequestManager(c, memParam)
      val res = requestManager.testSendRecvRequest(req, RequestSendConfig(0, timeout = 2400000))
    }
  }
   */
}