
package components

import chisel3._
import chisel3.iotesters.{ChiselFlatSpec, PeekPokeTester}
import chisel3.util.{LockingArbiterLike, Queue}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/*
class PRLUTUnitTester(c: PRLUT, source: Array[List[Int]], sink: Array[List[Int]]) extends PeekPokeTester(c) {
  val timeout = 100
  var source_ptr = 0
  var sink_ptr = 0
  var time = 0

  def pokeRequest(req: List[Int]): Unit = {
    val key :: req_type :: tbe_req_type :: tbe_req_id :: Nil = req
    poke(c.io.req.bits.key, key)
    poke(c.io.req.bits.request_type, req_type)
    poke(c.io.req.bits.data.req_type, tbe_req_type)
    poke(c.io.req.bits.data.requester_id, tbe_req_id)
  }

  def peekResponse(): List[Int] = {
    val found = peek(c.io.resp.bits.found).toInt
    val last = peek(c.io.resp.bits.last).toInt
    val req_type = peek(c.io.resp.bits.data.req_type).toInt
    val requester_id = peek(c.io.resp.bits.data.requester_id).toInt
    found :: last :: req_type :: requester_id :: Nil
  }

  def matchResponse(truth: List[Int], resp: List[Int]): Boolean = {
    truth.zip(resp).forall(p => p._1 == p._2)
  }

  poke(c.io.resp.ready, 1)

  while(source_ptr < source.length || sink_ptr < sink.length) {
    poke(c.io.req.valid, 0)
    if(source_ptr < source.length) {
      poke(c.io.req.valid, 1)
      pokeRequest(source(source_ptr))
    }
    if(peek(c.io.req.valid) == 1 && peek(c.io.req.ready) == 1) {
      source_ptr += 1
    }



    step(1)

    if(peek(c.io.resp.valid) == 1 && peek(c.io.resp.ready) == 1) {
      val msg = s"found ${peek(c.io.resp.bits.found)} last ${peek(c.io.resp.bits.last)}, req_type ${peek(c.io.resp.bits.data.req_type)} req_id ${peek(c.io.resp.bits.data.requester_id)}"
      val exp = s"expect ${sink(sink_ptr)}"
      expect(matchResponse(sink(sink_ptr), peekResponse()), s"response does not match: $msg, expected $exp")
      sink_ptr += 1
    }

    time += 1
    if( time > timeout ) {
      expect(false, "timeout")
      assert(false)
    }

  }

}

class PRLUTTester extends ChiselFlatSpec {
  private val backendNames = if (firrtl.FileUtils.isCommandAvailable(Seq("verilator", "--version"))) {
    Array("firrtl", "verilator")
  }
  else {
    Array("firrtl")
  }

  val keyWidth = 32
  val tableDepth = 16
  val idWidth = 4
  val fakeKey = 0x12345678

  def mkRequest(key: Int, req_type: Int, mem_req_type: Int, requestor_id: Int): List[Int] = {
    key :: req_type :: mem_req_type :: requestor_id :: Nil
  }
  def mkResponse(last: Int, found: Int, mem_req_type: Int, requestor_id: Int): List[Int] = {
    found :: last :: mem_req_type :: requestor_id :: Nil
  }

  "PRLUT" should "initialize" in {
     iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
       c => new PRLUTUnitTester(c, Array(), Array())
     } should be(true)
  }
  it should "insert" in {
    // val tbe = new TableEntry(0.U(1.W), 0.U(4.W))
    // val a = new PRLUTReq(0x12345678.U(32.W), tbe, 0.U(1.W))

    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    arr_req.append(mkRequest(
      key =fakeKey,
      req_type = PRLUTReq.insert.litValue().toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))

    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue().toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))

    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    arr_resp.append(mkResponse(
      last = 1,
      found = 1,
      mem_req_type =  0,
      requestor_id =  0
    ))
    iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
      c => new PRLUTUnitTester(c, arr_req.toArray, arr_resp.toArray)
    } should be(true)
  }
  it should "not find removed requests" in {
    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.insert.litValue.toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue.toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue.toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))
    arr_resp.append(mkResponse(
      found = 1,
      last = 1,
      mem_req_type = 0,
      requestor_id = 0
    ))
    arr_resp.append(mkResponse(
      found = 0,
      last = 1,
      mem_req_type = 0,
      requestor_id = 0
    ))
  }
  it should "remove multiple read requests and keep the order of requests" in {
    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    for { i <- 0 until 3 } {
      arr_req.append(mkRequest(
        key = fakeKey,
        req_type = PRLUTReq.insert.litValue.toInt,
        mem_req_type = 0,
        requestor_id = i
      ))
      arr_resp.append(mkResponse(
        found = 1,
        last = if(i == 2) { 1 } else { 0 },
        mem_req_type = 0,
        requestor_id = i
      ))
    }
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue.toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))


    iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
      c => new PRLUTUnitTester(c, arr_req.toArray, arr_resp.toArray)
    } should be(true)
  }
  it should "remove one write request and keep the order of requests" in {
    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    for { i <- 0 until 3 } {
      arr_req.append(mkRequest(
        key = fakeKey,
        req_type = PRLUTReq.insert.litValue.toInt,
        mem_req_type = if(i == 0) { 1 } else { 0 },
        requestor_id = i
      ))
      arr_resp.append(mkResponse(
        found = 1,
        last = if(i == 0 || i == 2) { 1 } else { 0 },
        mem_req_type = if(i == 0) { 1 } else { 0 },
        requestor_id = i
      ))
    }
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue.toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue.toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))


    iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
      c => new PRLUTUnitTester(c, arr_req.toArray, arr_resp.toArray)
    } should be(true)
  }
  it should "raise not found for requests not inserted" in {
    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    for { i <- 0 until 3 } {
      arr_req.append(mkRequest(
        key = fakeKey + i * 4,
        req_type = PRLUTReq.search_and_remove.litValue.toInt,
        mem_req_type = 0,
        requestor_id = 0
      ))
      arr_resp.append(mkResponse(
        found = 0,
        last = 1,
        mem_req_type = 0,
        requestor_id = 0
      ))
    }

    iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
      c => new PRLUTUnitTester(c, arr_req.toArray, arr_resp.toArray)
    } should be(true)
  }
  it should "insert and remove multiple keys" in {
    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    for { i <- 1 until 2 } {
      for { j <- 0 until 4} {
        arr_req.append(mkRequest(
          key = fakeKey + i,
          req_type = PRLUTReq.insert.litValue().toInt,
          mem_req_type = if (j == i % 4) 1 else 0,
          requestor_id = j + 2
        ))
      }
    }

    for{ i <- (1 until 2).reverse } {
      val write_index = i % 4
      arr_req.append(mkRequest(
        key = fakeKey + i,
        req_type = PRLUTReq.search_and_remove.litValue().toInt,
        mem_req_type = 0,
        requestor_id = 0
      ))

      // read responses
      for {idx <- 0 until write_index} {
        arr_resp.append(mkResponse(
          last = 0,
          found = 1,
          mem_req_type = 0,
          requestor_id = idx + 2
        ))
      }

      arr_resp.append(mkResponse(
        last = 1,
        found = 1,
        mem_req_type = 1,
        requestor_id = write_index + 2
      ))

      // final requests
      if(write_index != 3) {
        // read requests
        arr_req.append(mkRequest(
          key = fakeKey + i,
          req_type = PRLUTReq.search_and_remove.litValue().toInt,
          mem_req_type = 0,
          requestor_id = 0
        ))
        // read responses
        for {idx <- (write_index + 1) until 4} {
          arr_resp.append(mkResponse(
            last = if(idx == 3) 1 else 0,
            found = 1,
            mem_req_type = 0,
            requestor_id = idx + 2
          ))
        }
      }
    }
    iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
      c => new PRLUTUnitTester(c, arr_req.toArray, arr_resp.toArray)
    } should be(true)
  }

  it should "return requests of the RW RW pattern" in {
    val arr_req: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    val arr_resp: ArrayBuffer[List[Int]] = new ArrayBuffer(1)
    for { i <- 0 until 2 } {
      for { j <- 0 until 2 } {
        arr_req.append(mkRequest(
          key=fakeKey,
          req_type= PRLUTReq.insert.litValue().toInt,
          mem_req_type = j,
          requestor_id = i))
        arr_resp.append(mkResponse(
          last = if(j==1) 1 else 0,
          found=1,
          mem_req_type=j,
          requestor_id = i
        ))
      }
    }
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue().toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))
    arr_req.append(mkRequest(
      key = fakeKey,
      req_type = PRLUTReq.search_and_remove.litValue().toInt,
      mem_req_type = 0,
      requestor_id = 0
    ))

    iotesters.Driver.execute(Array(), () => new PRLUT(keyWidth, tableDepth, idWidth)) {
      c => new PRLUTUnitTester(c, arr_req.toArray, arr_resp.toArray)
    } should be(true)
  }
}

 */
