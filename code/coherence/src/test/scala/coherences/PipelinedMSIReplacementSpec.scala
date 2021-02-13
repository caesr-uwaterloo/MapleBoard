
package coherences

import _root_.core.{AMOOP, MemoryRequestType}
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import chiseltest.legacy.backends.verilator.VerilatorFlags
import components._
import fixtures.{TargetPlatformMSI => CurrentPlatform}
import org.scalatest.{FlatSpec, Matchers}
import testutil.{RequestManager, RequestSendConfig}
import chisel3._

class PipelinedMSIReplacementSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "PipelinedMSI"
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
    val fn = CurrentPlatform.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = CurrentPlatform.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    val memParam = CurrentPlatform.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      CurrentPlatform.coreParam,
      memParam,
      CurrentPlatform.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=0")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      // what if we dont do this initialization
      // testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440, 0x1840, 0x1c40, 0x80, 0x480, 0x880, 0xc80, 0x1080, 0x1480, 0x1880, 0x1c80)
      val rws: List[MemoryRequestType.Type] = List(MemoryRequestType.read, MemoryRequestType.read, MemoryRequestType.read, MemoryRequestType.write)
      // 64bit data
      val data: List[BigInt] = List("12345678abcdcdef", "baaaaaaddeadbeef", "beefbeef00000000").map(BigInt(_, 16))
      val request_matrix = (for {
        addr <- addrs
        rws <- rws
        datum <- data
      } yield {
        (addr, rws, datum)
      }).toList
      val totalRequestPerCore = 1000
      // maybe send the request after one's request is broadcasted on the bus
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, ((0 until totalRequestPerCore).map(_ => {
          val (addr, rws, datum) = request_matrix(r.nextInt(request_matrix.length))
          testutil.constructRequest(c, addr, datum, rws)
        })).toList)
      }).toList
      val req = Map(requests: _*)
      val requestManager = new RequestManager(c, memParam)
      requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 200000))
    }
  }
  it should "handle replacement correctly for shared everything atomic" in {
    val fn = CurrentPlatform.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = CurrentPlatform.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    println(s"The Shared Cache Coherence Table is at ${fns}")
    val memParam = CurrentPlatform.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      CurrentPlatform.coreParam,
      memParam,
      CurrentPlatform.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      // val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
       val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
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
      val totalRequestPerCore = 2000
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
      val res = requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 200000))// 2400000))
      val resAddrRes = res.foldLeft(collection.mutable.ArrayBuffer[(Int, BigInt)]()) { (prev, next) => prev ++ next._2 }
      val resMap = resAddrRes.groupBy(_._1).map { c => (c._1, c._2.map(_._2)) }.map { c => (c._1, c._2.max)}.toMap
      println("SW Value: ", maxVal)
      println("HW Value: ", resMap)
      println("Raw Value: \n")
      var lst = collection.mutable.ArrayBuffer[Tuple2[Tuple2[Int, BigInt], String]]()
      for { (k, v) <- res } {
        val req = requests(k)._2.map { r => if (r.amo_alu_op.litValue() ==  AMOOP.add.litValue()) { "add" } else { "read" } }
        println(">>>", k, v.zip(req).filter(s => s._1._1 == 1088 && s._2 == "add"))
        lst.append(v.zip(req).filter(s => s._1._1 == 1088 && s._2 == "add"): _*)
      }
      println(lst.sortBy(_._1._2))
      for { (addr, mx) <- maxVal} {
        val got = resMap.get(addr).get
        println("Checking: ", addr.toString())
        assert(mx == got + 1 || mx == got)
      }
    }
  }
  it should "handle replacement correctly for shared everything lr/sc" in {
    val fn = CurrentPlatform.coherenceSpec.generatePrivateCacheTableFile(None)
    val fns = CurrentPlatform.coherenceSpec.generateSharedCacheTableFile(None)
    println(s"The Private Cache Coherence Table is at ${fn}")
    val memParam = CurrentPlatform.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      CurrentPlatform.coreParam,
      memParam,
      CurrentPlatform.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
      // Now do some lr/screquests
      val totalRequestPerCore = 1000
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
    val memParam = CurrentPlatform.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      CurrentPlatform.coreParam,
      memParam,
      CurrentPlatform.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
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
      val totalRequestPerCore = 1000
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
      val res = requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 1200000))
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
  it should "give larger bound for all to 1 request" in {
    val memParam = CurrentPlatform.memorySystemParams.copy(masterCount = 4)
    test(new PipelinedBareMemorySubsystem(
      CurrentPlatform.coreParam,
      memParam,
      CurrentPlatform.coherenceSpec)).withAnnotations(Seq(VerilatorBackendAnnotation, VerilatorFlags(Seq("-DPRINTF_COND=1")), WriteVcdAnnotation)) { c =>
      val masterCount = memParam.masterCount
      // clocked for some cycles
      testutil.setUpPipelinedBareMemorySubsystem(c)
      val r = new scala.util.Random(42)
      // val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
      val addrs: List[Int] = List(0x40, 0x440, 0x840, 0xc40, 0x1040, 0x1440)
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
      val totalRequestPerCore = 2000
      val reqPhase = 10
      var maxVal = collection.mutable.Map[Int, BigInt]()
      // maybe send the request after one's request is broadcasted on the bus
      val requests: List[(Int, List[CacheReq])] = (for { i <- 0 until masterCount } yield {
        (i, ((0 until totalRequestPerCore).map(req_id => {
          if(i == 2) {
            // for core i, always get 0x40, 0x80, 0xc0, 0x100, 0x140, 0x180, 0x1c0 into modified state

            /*
            if(req_id / reqPhase % 8 <= 6) {
              val addr = 0x40 + (req_id) % 8 * 0x40
              val rws = MemoryRequestType.amo
              val datum = 1
              val last: BigInt = maxVal.getOrElse(addr, BigInt(0))
              maxVal.update(addr, last + 1)
              testutil.constructRequest(c, addr, datum, rws, AMOOP.add)
            } else {
              val addr = 0x14800
              val rws = MemoryRequestType.read
              val datum = 1
              testutil.constructRequest(c, addr, datum, rws)
            } */
            val addr = 0x40
            val rws = MemoryRequestType.amo
            val datum = 1
            val last: BigInt = maxVal.getOrElse(addr, BigInt(0))
            maxVal.update(addr, last + 1)
            testutil.constructRequest(c, addr, datum, rws, AMOOP.add)
          } else {
            // Note, this is not guaranteed to get the best possible request pattern
            // phase 0, read irrelevant data
            /*
            if(req_id / reqPhase % 8 <= 4) {
              val addr = 0x1440
              val rws = MemoryRequestType.read
              val datum = 1
              testutil.constructRequest(c, addr, datum, rws)
            } else {
              // phase 1, modify relevant data
              val addr = 0x40 + (req_id + i) % 8 * 0x40
              val rws = MemoryRequestType.amo
              val datum = 1
              val last: BigInt = maxVal.getOrElse(addr, BigInt(0))
              maxVal.update(addr, last + 1)
              testutil.constructRequest(c, addr, datum, rws, AMOOP.add)
            } */
            if(req_id  % 100  < 5 || i == 3) {
              val addr = 0x40
              val rws = MemoryRequestType.amo
              val datum = 1
              val last: BigInt = maxVal.getOrElse(addr, BigInt(0))
              maxVal.update(addr, last + 1)
              testutil.constructRequest(c, addr, datum, rws, AMOOP.add)
            } else {
              // phase 1, modify irrelevant data to saturate the network
              val addr = 0x40 + (req_id + i) * 0x40
              val rws = MemoryRequestType.read
              val datum = 1
              testutil.constructRequest(c, addr, datum, rws)
            }
          }
        })).toList)
      }).toList
      val req = Map(requests: _*)
      val requestManager = new RequestManager(c, memParam)
      val res = requestManager.testSendRecvRequest(req, RequestSendConfig(30, timeout = 2400000))
      val resAddrRes = res.foldLeft(collection.mutable.ArrayBuffer[(Int, BigInt)]()) { (prev, next) => prev ++ next._2 }
      val resMap = resAddrRes.groupBy(_._1).map { c => (c._1, c._2.map(_._2)) }.map { c => (c._1, c._2.max)}.toMap
      println("SW Value: ", maxVal)
      println("HW Value: ", resMap)
      println("Raw Value: \n")
      var lst = collection.mutable.ArrayBuffer[Tuple2[Tuple2[Int, BigInt], String]]()
      for { (k, v) <- res } {
        val req = requests(k)._2.map { r => if (r.amo_alu_op.litValue() ==  AMOOP.add.litValue()) { "add" } else { "read" } }
        println(">>>", k, v.zip(req).filter(s => s._1._1 == 1088 && s._2 == "add"))
        lst.append(v.zip(req).filter(s => s._1._1 == 1088 && s._2 == "add"): _*)
      }
      println(lst.sortBy(_._1._2))
      for { (addr, mx) <- maxVal} {
        val got = resMap.get(addr).get
        println("Checking: ", addr.toString())
        assert(mx == got + 1 || mx == got)
      }
    }
  }

}