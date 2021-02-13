
package components

import chisel3.util._
import chisel3._
import params.MemorySystemParams

/*
 * The module is essentially a copy from the LoCritPendingWritebackBuffer
 * The module supports sequential tracking of both request and write-backs
 * and is used in the conventional protocols as a request Buffer
 */
class PendingBuffer(private val size: Int,
                                   private val m: MemorySystemParams) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(m.getGenMemReqCommand))
    val deq = Decoupled(m.getGenMemReqCommand)
    val busy = Input(Bool())
    val count = Output(UInt(log2Ceil(size + 1).W))
    val markDirty = new Bundle {
      val enable = Input(Bool())
      val address = Input(m.cacheParams.genAddress)
    }
    val cancelEntry = new Bundle {
      val enable = Input(Bool())
      val address = Input(m.cacheParams.genAddress)
    }
  })
  println("PB size: %d", size)
  val inputQueue = Module(new Queue(m.getGenMemReqCommand, 1, pipe=true, flow=true))
  inputQueue.io.enq <> io.enq

  val globalOrder = Module(new LinkedList(size, log2Ceil(size + 1)))
  val freeList = Module(new Queue(UInt(log2Ceil(size + 1).W), size))
  dontTouch(freeList.io.count)
  val initializing = RegInit(true.B)
  val (initCounter, initCounterWarp) = Counter(initializing && freeList.io.enq.fire(), size)
  freeList.io.enq.valid := false.B
  freeList.io.enq.bits := 0.U
  when(initializing) {
    freeList.io.enq.bits := initCounter
    freeList.io.enq.valid := true.B
  }.otherwise {
    freeList.io.enq.bits := globalOrder.io.headData
  }
  when(initCounterWarp) { initializing := false.B }
  val entries = Reg(Vec(size, m.getGenMemReqCommand))
  val entries_idx = Reg(Vec(size, UInt(log2Ceil(size + 1).W)))
  val valid = RegInit(VecInit(Seq.fill(size) { false.B }))
  val matchCancelDirty = WireInit(VecInit(Seq.fill(size) { false.B }))
  val length = RegInit(0.U(log2Ceil(size + 1).W))
  for { i <- 0 until size } {
    matchCancelDirty(i) := valid(i) &&
      m.cacheParams.getTagAddress(entries(i).address) === m.cacheParams.getTagAddress(io.cancelEntry.address)
  }
  // io.enq.ready := !initializing && freeList.io.deq.valid && globalOrder.io.ready
  inputQueue.io.deq.ready := !initializing && freeList.io.deq.valid && globalOrder.io.ready && !io.cancelEntry.enable && !io.markDirty.enable
  freeList.io.deq.ready := inputQueue.io.deq.valid && !initializing && globalOrder.io.ready && !io.cancelEntry.enable && !io.markDirty.enable
  globalOrder.io.enable := false.B
  globalOrder.io.reqType := LinkedList.LinkedListOperation.PopFront
  globalOrder.io.din := 0.U
  globalOrder.io.index := 0.U
  io.count := length
  dontTouch(length)
  // when(io.enq.fire()) {
  when(inputQueue.io.deq.fire()) {
    length := length + 1.U
    // must dequeue at the same time...
    assert(freeList.io.deq.fire())
    globalOrder.io.enable := true.B
    globalOrder.io.reqType := LinkedList.LinkedListOperation.PushBack
    globalOrder.io.din := freeList.io.deq.bits
    // must be always able to receive ...
    assert(globalOrder.io.vacant)

    // entries(freeList.io.deq.bits) := io.enq.bits
    entries(freeList.io.deq.bits) := inputQueue.io.deq.bits
    entries_idx(freeList.io.deq.bits) := globalOrder.io.nextFree
    valid(freeList.io.deq.bits) := true.B
  }
  when(io.cancelEntry.enable && !initializing) {
    assert(io.busy)
    globalOrder.io.enable := PopCount(matchCancelDirty) > 0.U
    when(PopCount(matchCancelDirty) > 0.U) {
      length := length - 1.U
    }
    globalOrder.io.reqType := LinkedList.LinkedListOperation.RemoveIndex
    assert(PopCount(matchCancelDirty) <= 1.U)
    for { i <- 0 until size } {
      when(matchCancelDirty(i)) {
        globalOrder.io.index := entries_idx(i)
        freeList.io.enq.valid := true.B
        freeList.io.enq.bits := i.U
        assert(freeList.io.enq.ready)
        valid(i) := false.B
      }
    }
  }

  when(io.markDirty.enable && !initializing) {
    assert(io.busy)
    assert(PopCount(matchCancelDirty) <= 1.U)
    for { i <- 0 until size } {
      when(matchCancelDirty(i)) {
        entries(i).dirty := true.B
      }
    }
  }
  io.deq.valid := false.B
  io.deq.bits := entries(globalOrder.io.headData)
  when(!io.busy && !initializing) {
    io.deq.valid := length > 0.U
    when(io.deq.fire()) {
      globalOrder.io.enable := true.B
      globalOrder.io.reqType := LinkedList.LinkedListOperation.PopFront
      length := length - 1.U
      freeList.io.enq.valid := true.B
      freeList.io.enq.bits := globalOrder.io.headData
      assert(freeList.io.enq.ready, "Free List should be able to receive free entry")
      valid(globalOrder.io.headData) := false.B
    }
  }

  // Cannot (should not) cancel and enqueue at the same time
  assert(!(inputQueue.io.deq.fire()&& io.cancelEntry.enable))
}
