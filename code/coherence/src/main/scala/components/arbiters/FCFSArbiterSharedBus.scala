
package components.arbiters

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import components.UIntHolding

/**
  * The arbiter is used for conventional coherence protocol arbitration
  * basic scheme: round robin across all the cores, using work conserving round-robin
  * also, if the response is available, prioritize the response
  * The arbitration period is still some slot width
  * ALSO FCFS of the response as well!
  */
class FCFSArbiterSharedBus[T <: Data](gen: T, n: Int, val slotWidth: Int,
                                      needsLock: Option[T => Bool] = None)
  extends LockingArbiterLike[T](gen, n, 1, needsLock) with HasChoice {

  val weightAlloc = List.fill(n)(1)  // always one core w/ one slot-like thing

  // Record which of the master arrives first, clears on TDM ending
  // This also takes into account the response as well
  // (The arbitration between write-backs and response??
  val valid = RegInit(false.B)
  // val validMaster = RegInit(0.U(log2Ceil(n).W))
  val validMaster = RegInit(VecInit(Seq.fill(n) { false.B }))
  dontTouch(validMaster)

  val request_channel_valid = Wire(Vec(n, Bool()))
  val response_channel_valid = Wire(Vec(n, Bool()))
  val response_channel_ready = Wire(Vec(n, Bool()))
  val has_response_pending = response_channel_valid.asUInt.orR
  val response_id = WireInit(0.U(log2Ceil(n).W))
  for { i <- 0 until n } {
    when(response_channel_valid(i)) {
      response_id := i.U
    }
  }
  dontTouch(response_id)
  // One way is to work as follows, queue them based on the time of request arrival, one at a time, masked with validMaster
  // if(!validMaster(i) and io.request_channel(i).valid) enqueue(i)
  // (dequeuing)
  // j = dequeue(), validMaster(j) = false else if request valid, then validMaster(i) = true
  // (and back to back if it is valid, but the arrival time will be current, so service old tasks first)
  val ownerQueue = Module(new Queue(UIntHolding(n), n, pipe = true, flow = true))
  ownerQueue.io.enq.bits := 0.U // default
  ownerQueue.io.enq.valid := false.B
  ownerQueue.io.deq.ready := io.out.fire() // <== this only valid when taking from the core
  when(io.out.fire()) { // when the
    assert(ownerQueue.io.deq.valid, "owner queue should be valid when output fires")
  }
  val assertDeq = WireInit(false.B)
  when(ownerQueue.io.deq.valid && !ownerQueue.io.deq.ready && !validMaster(ownerQueue.io.deq.bits) && ownerQueue.io.count =/= 0.U) {
    assertDeq := true.B
  }
  dontTouch(assertDeq)

  // This is prioritized, so must be if/else-if case
  var head = (1 until n).foldLeft(when(!validMaster(0) && request_channel_valid(0) /* && !io.in(0).ready */) {
    // validMaster(0) := true.B
    ownerQueue.io.enq.valid := true.B
    ownerQueue.io.enq.bits := 0.U
    // when(io.in(0).ready) {
    //   validMaster(0) := false.B
    // }
  }) {  (prev, i) =>
    prev.elsewhen(!validMaster(i) && request_channel_valid(i) /* && !io.in(i).ready */) {
      // validMaster(i) := true.B
      ownerQueue.io.enq.valid := true.B
      ownerQueue.io.enq.bits := i.U
      // when(io.in(i).ready) {
      //   validMaster(i) := false.B
      // }
    }
  }
  // (0 until n).foldLeft(when(false.B){}) { (prev, i) =>
  //   prev.elsewhen(validMaster(i) && io.in(i).fire()) {
  //     validMaster(i) := false.B // can be valid again for the next loop
  //   }
  // }
  // ----- new validMaster -----
  for { i <- 0 until n } {
    when(validMaster(i)) {
      when(ownerQueue.io.deq.fire() && ownerQueue.io.deq.bits === i.U) {
        when(ownerQueue.io.enq.fire() && ownerQueue.io.enq.bits =/= i.U || !ownerQueue.io.enq.fire()) {
          validMaster(i) := false.B
        }
      }
    }.otherwise { // !validMaster(i)
      when(ownerQueue.io.enq.fire() && ownerQueue.io.enq.bits === i.U) {
        when(ownerQueue.io.deq.fire() && ownerQueue.io.deq.bits =/= i.U || !ownerQueue.io.deq.fire()) {
          validMaster(i) := true.B
        }
      }
    }
  }
  // ----- new validMaster -----

  private lazy val slotTimerCounter = Counter(true.B, slotWidth)
  private lazy val slotCounter = slotTimerCounter._1
  private lazy val slotCounterWrap = slotTimerCounter._2


  // Determining which response channel to assert
  for { i <- 0 until n } {
    // provide initial values to pass init check
    // Note: the response channel ready is driven by io.chosen. there might be combinational loop as well...
    response_channel_valid(i) := false.B
    response_channel_ready(i) := false.B
    request_channel_valid(i) := false.B
    BoringUtils.addSink(response_channel_valid(i), s"ResponseValid${i}", disableDedup = true)
    BoringUtils.addSink(response_channel_ready(i), s"ResponseReady${i}", disableDedup = true)
    BoringUtils.addSink(request_channel_valid(i), s"RequestValid${i}", disableDedup = true)
  }

  BoringUtils.addSource(slotCounterWrap, "switchingConventional", disableDedup = true)
  val is_busy = RegInit(false.B)
  when(slotCounterWrap) {
    is_busy := false.B
  }
  when(is_busy) {
    for{i <- 0 until n } {
      when(i.U =/= slotOwner) {
        assert(!(response_channel_valid(i) && response_channel_ready(i)), "should not fire multiple response in one slot")
        assert(!io.in(i).fire(), "should not fire multiple requests in one slot")
      }
    }
  }.otherwise {
    for{i <- 0 until n } {
      when(response_channel_valid(i) && response_channel_ready(i)) {
        is_busy := true.B
      }
      when(io.in(i).fire()) {
        is_busy := true.B
      }
    }
  }
  BoringUtils.addSource(is_busy, "FCFSArbiterBusy", disableDedup = true)
  val firing = WireInit(VecInit(Seq.fill(2 * n) { false.B }))
  val anyFiring = firing.asUInt.orR()
  val firingMaster = WireInit(0.U(log2Ceil(n).W))
  for { i <- 1 until n } {
    firing(i) := response_channel_valid(i) && response_channel_ready(i)
    firing(i + n) := io.in(i).fire()
    when(firing(i)) {
      firingMaster := i.U
    }
    when(firing(i + n)) {
      firingMaster := (i + n).U
    }
  }
  assert(PopCount(firing) <= 1.U, "More than one req/resp fire at the same cycle")

  // private lazy val slotOwnerCounters: List[(UInt, Bool)] =
  //   weightAlloc.zipWithIndex.map(x => Counter(slotCounterWrap && (slotOwner === x._2.U), x._1))
  // private lazy val slotOwnerCountersWrap: Vec[Bool] = VecInit(slotOwnerCounters.map(x => x._2))

  // private lazy val slotOwnerCounter = Counter(n)
  private lazy val slotOwner: UInt = RegInit(0.U(log2Ceil(n).W)) // Mux(has_response_pending, response_id, ownerQueue.io.deq.bits)
  val slotOwnerWire: UInt = WireInit(0.U(log2Ceil(n).W))
  val slotOwnerWrapWire: Bool = WireInit(false.B)
  val isResponse: Bool = RegInit(false.B)
  // This counter is used when there is no request and no response
  val (slotRRCounter, _) = Counter(slotCounterWrap, n)
  val queued = RegInit(VecInit(Seq.fill(n) { false.B }))
  val slotQueue = Module(new Queue(UIntHolding(n), n, pipe = true, flow = true))
  slotQueue.io.enq.valid := false.B
  slotQueue.io.enq.bits := 0.U
  slotQueue.io.deq.ready := false.B
  assert(slotQueue.io.count === PopCount(queued))
  dontTouch(slotQueue.io.count)
  when((anyFiring && firingMaster =/= slotRRCounter || !anyFiring) && !queued(slotRRCounter) ) {
    slotQueue.io.enq.valid := true.B
    slotQueue.io.enq.bits := slotRRCounter
    queued(slotRRCounter) := true.B
  }
  when(slotQueue.io.deq.fire()) {
    queued(slotQueue.io.deq.bits) := false.B
  }

  slotOwnerWrapWire := slotCounterWrap
  val wrapDelayed = RegNext(slotCounterWrap)
  val unfairnessCounter = RegInit(0.U(32.W))
  val lastGranted = RegInit(0.U(32.W))
  when(slotCounterWrap) {
    lastGranted := io.chosen
  }
  when(wrapDelayed) {
    when(io.chosen === lastGranted) {
      unfairnessCounter := unfairnessCounter + 1.U
    }.otherwise {
      unfairnessCounter := 0.U
    }
  }
  val isUnfair = unfairnessCounter >= 5.U // 5 consecutive requests

  when(slotCounterWrap) {
    when(has_response_pending && !isUnfair) {
      slotOwner := response_id
      isResponse := true.B
    }.elsewhen(validMaster.asUInt.orR && !isUnfair) {
      slotOwner := ownerQueue.io.deq.bits
      isResponse := false.B
    }.otherwise {
      // assert(false.B)
      slotOwner := slotQueue.io.deq.bits
      slotQueue.io.deq.ready := true.B
    }
  }

  slotOwnerWire := slotOwner
  dontTouch(slotOwnerWire)

  // For this part, we extend the definition of beginning of a slot to be larger
  // so that a core is guaranteed to have requests issued even if it was having a previous request
  // as a result, the slot-width need to be larger than this value
  private lazy val pipeStage = 4
  assert(slotWidth > pipeStage * 2)
  private lazy val slotBeginning = slotCounter < pipeStage.U
  private lazy val requestIssued = RegInit(false.B)
  private lazy val grantMask = Wire(Vec(n, Bool()))
  for { i <- 0 until n} {
    grantMask(i) := slotOwner === i.U && slotBeginning
  }
  when(ownerQueue.io.enq.fire()) {
    assert(!validMaster(ownerQueue.io.enq.bits))
  }
  when(ownerQueue.io.deq.valid) {
    assert(validMaster(ownerQueue.io.deq.bits) || ownerQueue.io.enq.fire())
  }
  dontTouch(ownerQueue.io.count)

  val grantMaskWire = Wire(Vec(n, Bool()))
  for { i <- 0 until n } {
    grantMaskWire(i) := grantMask(i)
  }
  dontTouch(grantMaskWire)

  // assert(ownerQueue.io.count < 4.U, "Debug Only Asserts")
  // printf(p"grantMask: ${grantMask}\n")
  // printf(p"slotBeginning: ${slotBeginning}\n")
  // printf(p"slotOwner: ${slotOwner}\n")

  override def grant: Seq[Bool] = {
    (0 until n).map(i => grantMask(i))
  }
  override lazy val choice = slotOwner // WireInit((n-1).asUInt)
  override def getChoice : UInt = choice

  // only enable the request in the beginning of the slot
  io.out.valid := io.in(choice).valid && slotBeginning && !requestIssued && !isResponse && !has_response_pending
  io.out.bits := io.in(choice).bits

  assert(ownerQueue.io.count === PopCount(validMaster), "Pending Request Invariant")

  when(io.out.fire()) {
    requestIssued := true.B
  }.elsewhen(slotCounterWrap) {
    requestIssued := false.B
  }
}