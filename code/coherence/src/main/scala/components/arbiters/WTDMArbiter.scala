
package components.arbiters

import chisel3._
import chisel3.util._

class WTDMArbiter[T <: Data](gen: T, n: Int, val slotWidth: Int, val weightAlloc: List[Int],
                             needsLock: Option[T => Bool] = None)
  extends LockingArbiterLike[T](gen, n, 1, needsLock) with HasChoice {
  assert(weightAlloc.length == n)
  assert(weightAlloc.forall(_ > 0))
  private lazy val slotTimerCounter = Counter(true.B, slotWidth)
  private lazy val slotCounter = slotTimerCounter._1
  private lazy val slotCounterWrap = slotTimerCounter._2


  private lazy val slotOwnerCounters: List[(UInt, Bool)] =
    weightAlloc.zipWithIndex.map(x => Counter(slotCounterWrap && (slotOwner === x._2.U), x._1))
  private lazy val slotOwnerCountersWrap: Vec[Bool] = VecInit(slotOwnerCounters.map(x => x._2))

  private lazy val slotOwnerCounter = Counter(n)
  private lazy val slotOwner: UInt = slotOwnerCounter.value

  when(slotOwnerCountersWrap(slotOwner)) {
    slotOwnerCounter.inc()
  }
  // For this part, we extend the definition of beginning of a slot to be larger
  // so that a core is guaranteed to have requests issued even if it was having a previous request
  // as a result, the slot-width need to be larger than this value
  private lazy val pipeStage = 4
  assert(slotWidth > pipeStage * 2)
  private lazy val slotBeginning = slotCounter < pipeStage.U
  private lazy val requestIssued = RegInit(false.B)
  private lazy val grantMask = (0 until n).map(slotOwner === _.U && slotBeginning)

  override def grant: Seq[Bool] = {
    (0 until n).map(i => grantMask(i))
  }
  override lazy val choice = slotOwner // WireInit((n-1).asUInt)
  override def getChoice : UInt = choice

  // only enable the request in the beginning of the slot
  io.out.valid := io.in(choice).valid && slotBeginning && !requestIssued
  io.out.bits := io.in(choice).bits

  when(io.out.fire()) {
    requestIssued := true.B
  }.elsewhen(slotCounterWrap) {
    requestIssued := false.B
  }
}