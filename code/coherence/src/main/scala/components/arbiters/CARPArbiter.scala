
package components.arbiters

import chisel3._
import chisel3.util._
import chisel3.experimental._

object CARPPhase extends ChiselEnum {
  val tdm, rr, rrslot = Value
}
class CARPArbiter[T <: Data](gen: T, n: Int, val slotWidth: Int, val weightAlloc: List[Int],
                             needsLock: Option[T => Bool] = None,
                             val RRWidth: Int,
                             val RRCores: List[Int])
  extends LockingArbiterLike[T](gen, n, 1, needsLock) with HasChoice {
  require(RRWidth > slotWidth)
  assert(weightAlloc.length + RRCores.length == n)
  assert(weightAlloc.forall(_ > 0))
  lazy val phase = RegInit(CARPPhase.tdm)
  lazy val slotTimerCounter = Counter(phase === CARPPhase.tdm, slotWidth)
  lazy val rrPeriod = Reg(UInt(log2Ceil(RRWidth).W))
  lazy val rrAccept = rrPeriod < (RRWidth - slotWidth).U
  lazy val rrSlotCounter = Counter(phase === CARPPhase.rrslot, slotWidth)
  lazy val slotCounter = slotTimerCounter._1
  lazy val slotCounterWrap = slotTimerCounter._2
  lazy val rrAlloc = WireInit(VecInit(RRCores.map(_.U)))
  lazy val rrCounter = Counter(phase === CARPPhase.rr, RRCores.length)
  lazy val tdmSlotCounter = Counter(slotTimerCounter._2, weightAlloc.sum)
  lazy val lastRRCounter = Reg(UInt(log2Ceil(n).W))

  when(phase === CARPPhase.rr) {
    lastRRCounter := rrAlloc(rrCounter._1)
  }

  when(tdmSlotCounter._2) {
    phase := CARPPhase.rr
  }.elsewhen(phase === CARPPhase.rr &&  rrPeriod === (RRWidth - 1).U && !io.out.fire) {
    phase := CARPPhase.tdm
  }.elsewhen(phase === CARPPhase.rr && io.out.fire) {
    phase := CARPPhase.rrslot
  }.elsewhen(phase === CARPPhase.rrslot && rrSlotCounter._2) {
    when(!rrAccept) {
      phase := CARPPhase.tdm // if it is the request in the last cycle
    }.otherwise {
      phase := CARPPhase.rr // otherwise, goto rr again
    }
  }
  when(phase === CARPPhase.rr || phase === CARPPhase.rrslot) {
    rrPeriod := rrPeriod + 1.U
  }.otherwise {
    rrPeriod := 0.U
  }

  private lazy val slotOwnerCounters: List[(UInt, Bool)] =
    weightAlloc.zipWithIndex.map(x => Counter(slotCounterWrap && (slotOwner === x._2.U), x._1))
  private lazy val slotOwnerCountersWrap: Vec[Bool] = VecInit(slotOwnerCounters.map(x => x._2))

  private lazy val slotOwnerCounter = Counter(weightAlloc.length)
  private lazy val slotOwner: UInt = slotOwnerCounter.value

  when(slotOwnerCountersWrap(slotOwner)) {
    slotOwnerCounter.inc()
  }
  // For this part, we extend the definition of beginning of a slot to be larger
  // so that a core is guaranteed to have requests issued even if it was having a previous request
  // as a result, the slot-width need to be larger than this value
  private lazy val pipeStage = 4
  assert(slotWidth > pipeStage * 2)
  private lazy val slotBeginning = slotCounter < pipeStage.U && phase === CARPPhase.tdm
  private lazy val requestIssued = RegInit(false.B)
  private lazy val grantMask = (0 until n).map(i => slotOwner === i.U && slotBeginning ||
    // if selected in the rr phase
    phase === CARPPhase.rr && rrAlloc(rrCounter._1) === i.U
  )

  override def grant: Seq[Bool] = {
    (0 until n).map(i => grantMask(i))
  }
  override lazy val choice = Mux(phase === CARPPhase.tdm, slotOwner,
    Mux(phase === CARPPhase.rr, rrAlloc(rrCounter._1), lastRRCounter)
    ) // WireInit((n-1).asUInt)
  override def getChoice : UInt = choice

  // only enable the request in the beginning of the slot
  io.out.valid := io.in(choice).valid && slotBeginning && !requestIssued && phase === CARPPhase.tdm ||
    io.in(choice).valid && !requestIssued && phase === CARPPhase.rr
  io.out.bits := io.in(choice).bits

  when(io.out.fire()) {
    requestIssued := true.B
  }.elsewhen(slotCounterWrap) {
    requestIssued := false.B
  }.elsewhen(rrSlotCounter._2) { // if the counter fired
    requestIssued := false.B
  }
}