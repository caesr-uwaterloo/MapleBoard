
package components

import chisel3.util._
import chisel3._
import params.MemorySystemParams

class PendingWritebackBuffer(private val size: Int,
                             private val m: MemorySystemParams
                            ) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(m.getGenMemReqCommand))
    val deq = Decoupled(m.getGenMemReqCommand)
    val busy = Input(Bool())
    val count = Output(UInt(log2Ceil(size + 1).W))
    val markDirty = new Bundle {
      val enable = Input(Bool())
      val address = Input(m.cacheParams.genAddress)
    }
    val cancelEntry = new Bundle { // only used to move from replacement buffer to writeback queue
      val enable = Input(Bool())
      val address = Input(m.cacheParams.genAddress)
    }
  })
  // Because this is a FIFO
  when(io.cancelEntry.enable) {
    assert(size.U === 1.U, "the cancel feature for pending write-back buffer only works for one entry")
  }
  val matchCancelDirty = WireInit(VecInit(Seq.fill(size) { false.B }))

  val doCancel = Wire(Bool())
  doCancel := io.cancelEntry.enable && matchCancelDirty.asUInt.orR()
  val (wr, wrWrap) = Counter(io.enq.fire(), size)
  val (rd, rdWrap) = Counter(io.deq.fire() | doCancel, size)
  val entries = Reg(Vec(size, m.getGenMemReqCommand))
  val valid = RegInit(VecInit(Seq.fill(size) { false.B }))
  val matchMarkDirty = WireInit(VecInit(Seq.fill(size) { false.B }))
  val length = RegInit(0.U(log2Ceil(size + 1).W))
  io.count := length
  for { i <- 0 until size } {
    matchCancelDirty(i) := valid(i) &&
      m.cacheParams.getTagAddress(entries(i).address) === m.cacheParams.getTagAddress(io.cancelEntry.address)
  }
  for { i <- 0 until size } {
    matchMarkDirty(i) := valid(i) &&
      m.cacheParams.getTagAddress(entries(i).address) === m.cacheParams.getTagAddress(io.markDirty.address)
  }
  for { i <- 0 until size } {
    when(io.markDirty.enable && matchMarkDirty(i)) {
      entries(i).dirty := true.B
    }
  }
  dontTouch(io.deq.bits.dirty)
  io.enq.ready := length < size.U
  io.deq.valid := length > 0.U && !io.busy
  when(io.enq.fire() && io.deq.fire()) {
  }.elsewhen(io.enq.fire()) {
    length := length + 1.U
  }.elsewhen(io.deq.fire()) {
    length := length - 1.U
  }.elsewhen(doCancel) {
    length := length - 1.U
  }
  when(io.enq.fire()) {
    valid(wr) := true.B
    entries(wr) := io.enq.bits
    when(io.markDirty.enable &&
      m.cacheParams.getTagAddress(io.markDirty.address) ===
      m.cacheParams.getTagAddress(io.enq.bits.address)
    ) {
      entries(wr).dirty := true.B
    }
  }
  when(io.deq.fire()) {
    valid(rd) := false.B
  }.elsewhen(doCancel) {
    for { i <- 0 until size } {
      when(matchCancelDirty(i)) {
        valid(i) := false.B
      }
    }
  }

  io.deq.bits := entries(rd)
}

// Support cancelling etc
class LoCritPendingWritebackBuffer(private val size: Int,
                             private val m: MemorySystemParams) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(m.getGenMemReqCommand))
    val deq = Decoupled(m.getGenMemReqCommand)
    val busy = Input(Bool())
    val count = Output(UInt(log2Ceil(size + 1).W))
    /*
    val markDirty = new Bundle {
      val enable = Input(Bool())
      val address = Input(m.cacheParams.genAddress)
    }
     */
    val cancelEntry = new Bundle {
      val enable = Input(Bool())
      val address = Input(m.cacheParams.genAddress)
    }
  })
  println("LCPWB size: %d", size)
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
  io.enq.ready := !initializing && freeList.io.deq.valid && globalOrder.io.ready
  freeList.io.deq.ready := io.enq.valid && !initializing && globalOrder.io.ready
  globalOrder.io.enable := false.B
  globalOrder.io.reqType := LinkedList.LinkedListOperation.PopFront
  globalOrder.io.din := 0.U
  globalOrder.io.index := 0.U
  io.count := length
  dontTouch(length)
  when(io.enq.fire()) {
    length := length + 1.U
    // must dequeue at the same time...
    assert(freeList.io.deq.fire())
    globalOrder.io.enable := true.B
    globalOrder.io.reqType := LinkedList.LinkedListOperation.PushBack
    globalOrder.io.din := freeList.io.deq.bits
    // must be always able to receive ...
    assert(globalOrder.io.vacant)

    entries(freeList.io.deq.bits) := io.enq.bits
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
  assert(!(io.enq.valid && io.cancelEntry.enable))
}
