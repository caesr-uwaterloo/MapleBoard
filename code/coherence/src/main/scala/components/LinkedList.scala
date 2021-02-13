
package components

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._

object LinkedList {
  object LinkedListState extends ChiselEnum {
    val init, work = Value
  }

  object LinkedListOperation extends ChiselEnum {
    val PushBack, PopFront, RemoveIndex = Value
  }

  class LinkedListIO[T <: Data](private val size: Int,
                     private val dataWidth: Int
                    ) extends Bundle {
    val enable = Input(Bool())
    val reqType = Input(LinkedListOperation())
    val index = Input(UIntHolding(size))
    val din = Input(UInt(dataWidth.W))

    val ready = Output(Bool())
    val vacant = Output(Bool())
    val nextFree = Output(UIntHolding(size))
    val dout = Output(UInt(dataWidth.W))

    val count = Output(UIntHolding(size + 1))
    val headData = Output(UInt(dataWidth.W))
  }
  class LinkedListEntry(private val size: Int, private val dataWidth: Int) extends Bundle {
    val valid = Bool()
    val head = Bool()
    val tail = Bool()
    val prev = UIntHolding(size)
    val next = UIntHolding(size)
    val data = UInt(dataWidth.W)

    override def cloneType: LinkedListEntry.this.type = new LinkedListEntry(size, dataWidth).asInstanceOf[this.type]
  }
}


class LinkedList(size: Int,
                 dataWidth: Int) extends Module {
  import LinkedList._
  val io = IO(new LinkedListIO(size, dataWidth))
  val state = RegInit(LinkedListState.init)
  val freeList = Module(new Queue(UIntHolding(size), size, pipe = true))
  val isInit = state === LinkedListState.init
  val linkedListData = Reg(Vec(size, new LinkedListEntry(size, dataWidth)))

  val head = RegInit(0.U(log2Ceil(size).W))
  val tail = RegInit(0.U(log2Ceil(size).W))
  val count = RegInit(0.U(log2Ceil(size + 1).W))
  val doutReg = RegInit(0.U(dataWidth.W))

  val (freeListInitCounter,
  freeListInitCounterWrap) = Counter(freeList.io.enq.fire(), size)
  freeList.io.enq.valid := false.B
  freeList.io.enq.bits := 0.U
  // Initialize the free list
  when(isInit ) {
    printf("In Init: %d\n", freeListInitCounter)
    freeList.io.enq.bits := freeListInitCounter
    freeList.io.enq.valid := true.B
    linkedListData(freeListInitCounter).valid := false.B
    when(freeListInitCounterWrap) {
      state := LinkedListState.work
    }
  }
  val nextFree = WireInit(freeList.io.deq.bits)
  val vacant = WireInit(freeList.io.deq.valid)
  val idxToRemove = Wire(UIntHolding(size))
  // val doutReg = RegNext(linkedListData(idxToRemove).data) // RegInit(0.U(log2Ceil(size).W))
  when(io.reqType === LinkedListOperation.RemoveIndex) {
    idxToRemove := io.index
  }.otherwise {
    idxToRemove := head
  }
  val rmHead = linkedListData(idxToRemove).head
  val rmTail = linkedListData(idxToRemove).tail
  io.vacant := vacant
  io.nextFree := nextFree
  io.count := count
  io.ready := !isInit
  io.headData := linkedListData(head).data
  // read operation...
  // default dont get free entry
  freeList.io.deq.ready := false.B
  when(!isInit && io.enable) {
    when(io.reqType === LinkedListOperation.PushBack) {
      assert(vacant, "Must have free entry")
      freeList.io.deq.ready := true.B
      when(count > 0.U) {
        linkedListData(tail).next := nextFree
        linkedListData(tail).tail := false.B
      }

      // reset of the entry
      linkedListData(nextFree).prev := tail
      linkedListData(nextFree).valid := true.B
      linkedListData(nextFree).data := io.din
      linkedListData(nextFree).tail := true.B
      linkedListData(nextFree).head := false.B

      tail := nextFree
      when(count === 0.U) {
        linkedListData(nextFree).head := true.B
        head := nextFree
      }
      count := count + 1.U
    }.elsewhen(io.reqType === LinkedListOperation.PopFront || io.reqType === LinkedListOperation.RemoveIndex) {
      count := count - 1.U
      assert(linkedListData(idxToRemove).valid, "Entry to remove must be valid")
      // return the entry to the freelist
      freeList.io.enq.valid := true.B
      freeList.io.enq.bits := idxToRemove

      // invalidate the entry
      linkedListData(idxToRemove).valid := false.B

      // return the data
      doutReg := linkedListData(idxToRemove).data

      // maintain the data structure
      // printf("Removing: %d, should return: %d\n", idxToRemove, linkedListData(idxToRemove).data)
      when(count === 1.U) {
        // do nothing, simply invalidate, so it's head and tail at the same time
      }.elsewhen(rmHead) {
        head := linkedListData(idxToRemove).next
        linkedListData(linkedListData(idxToRemove).next).head := true.B
        linkedListData(linkedListData(idxToRemove).next).prev := 0.U
      }.elsewhen(rmTail) {
        tail := linkedListData(idxToRemove).prev
        linkedListData(linkedListData(idxToRemove).prev).tail := true.B
        linkedListData(linkedListData(idxToRemove).prev).next := 0.U
      }.otherwise {
        // removing in the middle
        linkedListData(linkedListData(idxToRemove).next).prev := linkedListData(idxToRemove).prev
        linkedListData(linkedListData(idxToRemove).prev).next := linkedListData(idxToRemove).next
      }
    }

  }
  io.dout := doutReg
  // printf("Content: \n")
  // for { i <- 0 until size } {
  //   printf(p"${linkedListData(i)}\n")
  // }
  // printf(p"Out: ${doutReg}\n")
}
