
package components.arbiters

import components._
import chisel3._
import chisel3.util._

class TDMArbiter {

  class TDMArbiter[T <: Data](gen: T, n: Int, val slotWidth: Int, needsLock: Option[T => Bool] = None)
    extends LockingArbiterLike[T](gen, n, 1, needsLock) with HasChoice {
    private lazy val slotTimerCounter = Counter(true.B, slotWidth)
    private lazy val slotCounter = slotTimerCounter._1
    private lazy val slotCounterWrap = slotTimerCounter._2
    private lazy val slotOwnerCounter = Counter(slotCounterWrap, n)
    private lazy val slotOwner = slotOwnerCounter._1
    private lazy val slotBeginning = slotCounter === 0.U
    private lazy val grantMask = (0 until n).map(slotOwner === _.U && slotBeginning)

    // grant access in the beginning of the slot
    override def grant: Seq[Bool] = {
      (0 until n).map(i => grantMask(i))
    }
    override lazy val choice = slotOwner // WireInit((n-1).asUInt)
    override def getChoice : UInt = choice

    // only enable the request in the beginning of the slot
    io.out.valid := io.in(choice).valid && slotBeginning
    io.out.bits := io.in(choice).bits
  }
}
