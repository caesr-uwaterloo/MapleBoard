
package components

import chisel3._
import chisel3.util.{OHToUInt, UIntToOH, log2Ceil}
import coherence.internal.AutoEnum
import params.MemorySystemParams

object LLCCoherenceTable {
  class LLCCoherenceTableIO[+CE <: AutoEnum,
    +CohS <: AutoEnum
  ] (
    private val genEvent: CE,
    private val genState: CohS,
    private val masterCount: Int
  ) extends Bundle {
    val currentState = Input(UInt(genState.getWidth.W))
    val currentEvent = Input(UInt(genEvent.getWidth.W))
    val currentSharers = Input(UInt(masterCount.W))
    val satisfiedSharers = Input(UInt(masterCount.W))
    val satisfiedPendingModified = Input(Bool())
    val requestor = Input(UInt(log2Ceil(masterCount).W))

    val nextState = Output(UInt(genState.getWidth.W))
    // NOTE: this action is only taken into account in
    // LLC_MEMORY_WRITE state (for PUTM)
    val actionLLCStates = Output(UInt(LLCStates.getWidth.W))
    val nextSharers = Output(UInt(masterCount.W))
  }
}
abstract class LLCCoherenceTable[+CE <: AutoEnum, +CohS <: AutoEnum]
(val genCacheEvent: CE,
 val genCoherenceState: CohS,
 val masterCount: Int)
  extends Module {
  val io = IO(new LLCCoherenceTable.LLCCoherenceTableIO(genCacheEvent, genCoherenceState, masterCount))
  // We can define serveral basic actions with in the coherence table

  //type Event = (Int, Int, Option[UInt])
  trait CurrentSharer {}
  class Event(val event: Int, val state: Int,
              val hasRead: Option[Boolean],
              val hasModified: Option[Boolean],
              val currentSharer: Option[CurrentSharer]
             ) {
  }
  def AllRead = 0
  def OneWrite  = 1
  def NoPendingReq = 2

  def AddSharer = 0
  def SetSharers = 1
  def MarkOwner = 2
  def RemoveSharer = 3
  def AppendSharers = 4

  case class ConeSharer() extends CurrentSharer
  case class CmoreThanOneSharer() extends CurrentSharer
  def OneSharer = ConeSharer()
  def MoreThanOneSharer = CmoreThanOneSharer()

  object Event {
    def apply(event: Int, state: Int): Event = new Event(event, state, None, None, None)

    def apply(event: Int, state: Int, desc: Int): Event = {
      if (desc == AllRead) {
        new Event(event, state, Some(true), Some(false), None)
      } else if (desc == OneWrite) {
        // we don't care about read here
        new Event(event, state, None, Some(true), None)
      } else if (desc == NoPendingReq) {
        new Event(event, state, Some(false), Some(false), None)
      } else {
        throw new RuntimeException("Unsupported response count")
      }
    }

    def apply(event: Int, state: Int, sharer: CurrentSharer): Event =
      new Event(event, state, None, None, Some(sharer))
  }

  implicit def UInt2Option(v: UInt): Option[UInt] = Some(v)
  implicit def Int2Option(v: Int): Option[Int] = Some(v)

  class Do(val next_state: Int, val llcAction: Int, val nextSharers: Option[Int])
  object Do {
    def apply(next_state: Int, llcAction: Int): Do = new Do(next_state, llcAction, None)
    def apply(next_state: Int, llcAction: Int, nextSharersDec: Int): Do =
      new Do(next_state, llcAction, Some(nextSharersDec))
  }


  def CE = genCacheEvent
  def CohS = genCoherenceState

  io.actionLLCStates := LLCStates.LLC_IDLE.U
  io.nextSharers := 0.U//genCoherenceState.I.U
  io.nextState := 0.U

  def Valid: UInt = 1.U
  def Invalid: UInt = 0.U
  def Keep: Option[UInt] = None

  def Clean: UInt = 0.U
  def Dirty: UInt = 1.U

  def table: Map[Event, Do]

  val hasRead = io.satisfiedSharers.orR()
  val hasModified = io.satisfiedPendingModified
  val one_sharer = UIntToOH(OHToUInt(io.currentSharers)) === io.currentSharers
  val more_than_one_sharer = !one_sharer && io.currentSharers.orR.asBool


  // generate logic from previous table
  for { ((event, action), idx) <- table.zipWithIndex } {
    try {
      val event_match = event.event.U === io.currentEvent
      val state_match = event.state.U === io.currentState
      val has_sharer_match = event.hasRead match {
        case Some(v) => v.B === hasRead
        case None => true.B
      }
      val has_modified_match = event.hasModified match {
        case Some(v) => v.B === hasModified
        case None => true.B
      }
      val sharer_count_match = event.currentSharer match {
        case Some(ConeSharer()) => one_sharer
        case Some(CmoreThanOneSharer()) => more_than_one_sharer
        case None => true.B
        case _ => throw new RuntimeException("Invalid Share Count Value")
      }
      /*
      AddSharer = 0
      SetSharers = 1
      MarkOwner = 2
      RemoveSharer = 3
      AppendSharers = 4
       */
      // if(idx == 6) {
      //   printf("State: %d, Event: %d\n", io.currentState, io.currentEvent)
      //   printf("??? %b %b %b %b %b\n", event_match, state_match, has_sharer_match, has_modified_match, sharer_count_match)
      // }
      when(event_match && state_match && has_sharer_match && has_modified_match && sharer_count_match) {
        // drive the output signal
        io.nextState := action.next_state.U
        io.actionLLCStates := action.llcAction.U
        action.nextSharers match {
          case Some(/* AddSharer */ 0) => io.nextSharers := io.currentSharers | (1.U << io.requestor).asUInt
          case Some(/* SetSharers */ 1) => io.nextSharers := io.satisfiedSharers
          case Some(/* MarkOwner */ 2) => io.nextSharers := (1.U << io.requestor).asUInt
          case Some(/* RemoveSharer */ 3) => io.nextSharers := io.currentSharers ^ (1.U << io.requestor).asUInt
          case Some(/* AppendSharers */ 4) => io.nextSharers := io.currentSharers | io.satisfiedSharers
          case None => io.nextSharers := io.currentSharers
          case _ => throw new RuntimeException("Unsupported sharer modifier")
        }
        /*
        when(io.currentEvent =/= 0.U) {
          printf(s"State: ${genCoherenceState.getStateList(event.state)} " +
            s"Event: ${genCacheEvent.getStateList(event.event)},  -> " +
            s"nextState ${genCoherenceState.getStateList(action.next_state)}, " +
            s"llcAction ${LLCStates.getStateList(action.llcAction)}, " +
            s"pendingGetM %b\n", io.satisfiedPendingModified
          )
        } */
      }
    } catch {
      case e: Exception => {
        println("Exception Encountered when Processing State Transition: "
          + event.toString() + " -> " + action.toString())
        throw e
      }
    }
  }
}
