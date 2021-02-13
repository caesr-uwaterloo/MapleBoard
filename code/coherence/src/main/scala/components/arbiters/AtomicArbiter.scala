
package components.arbiters

import chisel3._
import chisel3.util._
import chisel3.experimental._
import components.UIntHolding

abstract class AtomicArbiter[T <: Data](gen: T, n: Int, val slotWidth: Int, val weightAlloc: List[Int],
                             needsLock: Option[T => Bool] = None)
  extends LockingArbiterLike[T](gen, n, 1, needsLock) with HasChoice {
  assert(weightAlloc.length == n)
  assert(weightAlloc.forall(_ > 0))
  private val addrWidth = 64
  lazy val phase = RegInit(0.U(2.W))
  private lazy val slotTimerCounter = Counter(true.B, slotWidth)
  lazy val slotCounter = slotTimerCounter._1
  lazy val slotCounterWrap = slotTimerCounter._2


  private lazy val slotOwnerCounters: List[(UInt, Bool)] =
    weightAlloc.zipWithIndex.map(x => Counter(slotCounterWrap && (slotOwner === x._2.U) && phase === 0.U, x._1))
  private lazy val slotOwnerCountersWrap: Vec[Bool] = VecInit(slotOwnerCounters.map(x => x._2))

  private lazy val slotOwnerCounter = Counter(n)
  lazy val slotOwner: UInt = slotOwnerCounter.value
  // lazy val wbOwner: UInt = Wire(UInt(log2Ceil(n).W))
  lazy val phase0Requestor: UInt = RegInit(0.U.asTypeOf(UIntHolding(n)))
  when(slotBeginning && phase === 0.U) {
    phase0Requestor := slotOwner
  }

  when(slotOwnerCountersWrap(slotOwner)) {
    slotOwnerCounter.inc()
  }
  // For this part, we extend the definition of beginning of a slot to be larger
  // so that a core is guaranteed to have requests issued even if it was having a previous request
  // as a result, the slot-width need to be larger than this value
  // These three are the signals that needs to be driven by the main class
  def phase0NeedsWB: Bool
  def phase1IsMatchedWB: Vec[Bool]
  lazy val phase1Matched: UInt = WireInit(0.U(log2Ceil(n).W))
  lazy val pipeStage = 4
  assert(slotWidth > pipeStage * 2)
  lazy val slotBeginning = slotCounter < pipeStage.U
  lazy val requestIssued = RegInit(false.B)
  lazy val grantMask = (0 until n).map(i => {
    slotOwner === i.U && slotBeginning && phase === 0.U ||
    phase0NeedsWB && phase1IsMatchedWB(i) && slotBeginning && phase === 1.U
  })

  override def grant: Seq[Bool] = {
    (0 until n).map(i => grantMask(i))
  }
  override lazy val choice = Mux(phase === 0.U,
    slotOwner,
    Mux(phase === 1.U,
      phase1Matched,
      phase0Requestor // phase === 2.U
    )
  )// WireInit((n-1).asUInt)
  override def getChoice : UInt = choice

  // only enable the request in the beginning of the slot
  io.out.valid := io.in(choice).valid && slotBeginning && !requestIssued && phase === 0.U ||
                  io.in(choice).valid && slotBeginning && !requestIssued && phase === 1.U && phase1IsMatchedWB(choice) && phase0NeedsWB
  // note: in phase 2, we do not process requests from the host
  io.out.bits := io.in(choice).bits

  // phase 0 -> phase 1 -> phase 2
  when(slotCounterWrap) {
    when(phase === 0.U) {
      phase := 1.U
    }.elsewhen(phase === 1.U) {
      phase := 2.U
    }.otherwise {
      phase := 0.U
    }
  }

  when(io.out.fire()) {
    requestIssued := true.B
  }.elsewhen(slotCounterWrap) {
    requestIssued := false.B
  }
}
