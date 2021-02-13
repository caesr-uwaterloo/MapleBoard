
package components

import chiseltest._
import chiseltest.internal.{VerilatorBackendAnnotation, WriteVcdAnnotation}
import chiseltest.legacy.backends.verilator.VerilatorFlags
import chiseltest.experimental.TestOptionBuilder._
import components._
import chisel3._
import org.scalatest.{FlatSpec, Matchers}

object LinkedListSpec {
  class LinkedListSoftware(private val size: Int) {
    class Entry {
      var valid = false
      var data = 0
      var prev = 0
      var next = 0
    }
    type FreeList = collection.mutable.ListBuffer[Int]
    type DataList = collection.mutable.ArrayBuffer[Entry]
    var freeList: FreeList = collection.mutable.ListBuffer(((0 until size).toList) : _*)
    var dataList: DataList = collection.mutable.ArrayBuffer()
    for { i <- 0 until size } dataList.append(new Entry)
    var count = 0
    var head = 0
    var tail = 0
    def pushBack(data: Int): Int = {
      val free = freeList.head
      freeList = freeList.drop(1)

      dataList(free).prev = tail
      dataList(free).valid = true
      dataList(free).data = data
      if(count > 0) { dataList(tail).next = free }
      tail = free
      if(count == 0) { head = free }
      assert(checkValidity)
      // println("PushBack: ", data, free)
      count += 1
      free
    }
    def checkValidity: Boolean = {
      val total = dataList.zipWithIndex.filter(_._1.valid).map(_._2).toList ++ freeList.toList
      val res = total.sorted.zipWithIndex.forall(x => x._1 == x._2)
      res
    }
    def removeIdx(idx: Int): Int = {
      assert(dataList(idx).valid)
      val res = dataList(idx).data
      if(count == 1) {
        dataList(idx).valid = false
      } else if(idx == head) {
        head = dataList(idx).next
      } else if(idx == tail) {
        tail = dataList(idx).prev
      } else {
        dataList(dataList(idx).prev).next = dataList(idx).next
        dataList(dataList(idx).next).prev = dataList(idx).prev
      }
      dataList(idx).valid = false
      freeList.append(idx)
      count -= 1
      assert(checkValidity)
      // println("RemoveSW: ", res, idx)
      res
    }
    def popFront: Int = {
      assert(count > 0)
      removeIdx(head)
    }

    def getCount: Int = count
  }
}
class LinkedListSpec extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Linked List"
  implicit class HWInterface(c: LinkedList) {
    def pushBack(data: Int): Int = {
      c.io.enable.poke(true.B)
      c.io.reqType.poke(LinkedList.LinkedListOperation.PushBack)
      c.io.din.poke(data.U)
      val vac = c.io.vacant.peek().litToBoolean
      val res = c.io.nextFree.peek().litValue().toInt
      c.clock.step()
      c.io.enable.poke(false.B)
      res
    }

    def removeIdx(idx: Int): Int = {
      c.io.enable.poke(true.B)
      c.io.reqType.poke(LinkedList.LinkedListOperation.RemoveIndex)
      c.io.index.poke(idx.U)
      c.clock.step()
      c.io.enable.poke(false.B)
      val res = c.io.dout.peek().litValue().toInt
      res
    }
    def popFront(): Int = {
      c.io.enable.poke(true.B)
      c.io.reqType.poke(LinkedList.LinkedListOperation.PopFront)
      c.clock.step()
      c.io.enable.poke(false.B)
      c.io.dout.peek().litValue().toInt
    }
    def waitUntilReady(): Unit= {
      while (!c.io.ready.peek().litToBoolean) {
        c.clock.step()
      }
    }
  }
  it should "match software implementation" in {
    val size = 16
    test(new LinkedList(size, 32)) { c =>
      var ll = new LinkedListSpec.LinkedListSoftware(size)
      var r = new scala.util.Random(42)
      val swOps = List((x: Int) => ll.pushBack(x), (x: Int) => ll.popFront, (x: Int) => ll.removeIdx(x))
      val hwOps = List((x: Int) => c.pushBack(x), (x: Int) => c.popFront, (x: Int) => c.removeIdx(x))
      c.io.enable.poke(false.B)
      c.io.reqType.poke(LinkedList.LinkedListOperation.PushBack)
      c.io.index.poke(0.U)
      c.io.din.poke(0.U)
      c.waitUntilReady()
      c.clock.setTimeout(20000)
      for { i <- 0 until 10000 } {
        if (ll.getCount == 0) {
          val rdata = r.nextInt(128)
          ll.pushBack(rdata)
          c.pushBack(rdata)
        } else if(ll.getCount == size) {
          val ridx = r.nextInt(2) + 1
          val entries = ll.dataList.zipWithIndex.filter(_._1.valid).map(_._2).toList
          val dataIdx = r.nextInt(entries.size)
          if(ridx == 1) {
            assert(ll.popFront == c.popFront)
          } else {
            assert(ll.removeIdx(entries(dataIdx)) == c.removeIdx(entries(dataIdx)))
          }
        } else {
          val ridx = r.nextInt(3)
          if(ridx == 0) {
            val rdata = r.nextInt(128)
            ll.pushBack(rdata)
            c.pushBack(rdata)
          } else {
            val entries = ll.dataList.zipWithIndex.filter(_._1.valid).map(_._2).toList
            val dataIdx = r.nextInt(entries.size)
            if(ridx == 1) {
              assert(ll.popFront == c.popFront)
            } else {
              assert(ll.removeIdx(entries(dataIdx)) == c.removeIdx(entries(dataIdx)))
            }
          }
        }
      }
    }
  }
  it should "read write and remove" in {
    val size = 4
    test(new LinkedList(size = 4, 32)).withAnnotations(Seq(VerilatorBackendAnnotation)) { c =>
      var ll = new LinkedListSpec.LinkedListSoftware((size))
      c.io.enable.poke(false.B)
      c.io.reqType.poke(LinkedList.LinkedListOperation.PushBack)
      c.io.index.poke(0.U)
      c.io.din.poke(0.U)
      while (!c.io.ready.peek().litToBoolean) {
        c.clock.step()
      }
      for { i <- 0 until 4} {
        // pushing in the core id
        assert(c.io.vacant.peek().litToBoolean, "Always should be vacant")
        val resSW = ll.pushBack(i * i)
        val resHW = c.pushBack(i * i)
        assert(resSW === resHW && resHW == i, "Hardware and software result should match")
      }
      assert(!c.io.vacant.peek().litToBoolean)
      // front ---- end
      // 0 - 1 - 2 - 3
      // 0 - 1 - 4 - 9
      assert(ll.popFront == 0)
      assert(c.popFront == 0)
      assert(ll.pushBack(12) == 0)
      assert(c.pushBack(12) == 0)

      assert(!c.io.vacant.peek().litToBoolean)
      // 1 - 2 - 3 - 0
      // 1 - 4 - 9 - 12
      assert(ll.removeIdx(2) == 4)
      assert(c.removeIdx(2) == 4)
      // 1 - 3 - 0
      // 1 - 9 - 12
      assert(ll.removeIdx(3) == 9)
      assert(c.removeIdx(3) == 9)
      assert(ll.removeIdx(1) == 1)
      assert(c.removeIdx(1) == 1)
      assert(ll.popFront  == 12)
      assert(c.popFront == 12)
    }
  }
}
