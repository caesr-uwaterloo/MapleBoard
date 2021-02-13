
package components

import chisel3._
import chisel3.util._
import params._
import chisel3.experimental._
import coherence.internal.AutoEnum
import coherences.{CoherenceStates, PMSI}

import scala.language.implicitConversions

class CoherenceTableIO(genCacheEvent: AutoEnum, genCoherenceState: AutoEnum, genRequestType: AutoEnum)
  extends Bundle {
  val cache_line_event_i        = Input(UInt(genCacheEvent.getWidth.W))
  val cache_line_state_i        = Input(UInt(genCoherenceState.getWidth.W))
  val cache_line_tag_match_i    = Input(UInt(1.W))
  val cache_line_dirty_i        = Input(UInt(1.W))
  val cache_line_valid_i        = Input(UInt(1.W))
  val cache_ctr_state_i         = Input(UInt(CacheControllerState.getWidth.W))
  val crit_diff                 = Input(UInt(3.W))
  val cache_ctr_next_state_o    = Output(UInt(CacheControllerState.getWidth.W))
  val cache_line_next_state_o   = Output(UInt(genCoherenceState.getWidth.W))
  val cache_line_next_dirty_o   = Output(UInt(1.W))
  val cache_line_next_valid_o   = Output(UInt(1.W))
  val cache_line_broadcast_type = Output(UInt(genRequestType.getWidth.W))
}

class CoherenceTableIOSync(genCacheEvent: AutoEnum, genCoherenceState: AutoEnum, genRequestType: AutoEnum)
  extends Bundle {
  val clock = Input(Clock())
  val reset_i = Input(UInt(1.W))
  val cache_line_event_i        = Input(UInt(genCacheEvent.getWidth.W))
  val cache_line_state_i        = Input(UInt(genCoherenceState.getWidth.W))
  val cache_line_tag_match_i    = Input(UInt(1.W))
  val cache_line_dirty_i        = Input(UInt(1.W))
  val cache_line_valid_i        = Input(UInt(1.W))
  val crit_diff                 = Input(UInt(3.W))
  val cache_ctr_next_state_o    = Output(UInt(CacheControllerState.getWidth.W))
  val cache_line_next_state_o   = Output(UInt(genCoherenceState.getWidth.W))
  val cache_line_next_dirty_o   = Output(UInt(1.W))
  val cache_line_next_valid_o   = Output(UInt(1.W))
  val cache_line_broadcast_type = Output(UInt(genRequestType.getWidth.W))
}

class CoherenceTableBB extends BlackBox() with HasBlackBoxResource {

  override def desiredName: String = "coherence_table"
  val io = IO(new CoherenceTableIOSync(new CacheEvent {}, new PMSI{}, new RequestType {}))

  setResource("/coherence_table.v")
}

abstract class CoherenceTableGenerator[+CE <: AutoEnum, +CohS <: AutoEnum, +RequestType <: AutoEnum]
(val genCacheEvent: CE,
 val genCoherenceState: CohS,
 val genRequestType: RequestType)
  extends Module {
  val io = IO(new CoherenceTableIO(genCacheEvent, genCoherenceState, genRequestType))
  // We can define serveral basic actions with in the coherence table

  //type Event = (Int, Int, Option[UInt])
  class Event(val event: Int, val state: Int, val critDiff: Option[Int], val tag_match: Option[UInt]) {
  }
  object Event {
    @deprecated("EventEncoder should handle the tag_match, this interface should not be used")
    def apply(event: Int, state: Int, tag_match: Option[UInt]): Event = new Event(event, state, CriticalityDiff.SAMECRIT, tag_match)
    def apply(event: Int, state: Int): Event = new Event(event, state, None, None)
    def apply(event: Int, state: Int, critDiff: Int): Event = new Event(event, state, Some(critDiff), None)
  }
  implicit def UInt2Option(v: UInt): Option[UInt] = Some(v)
  implicit def Int2Option(v: Int): Option[Int] = Some(v)

  class Do(val next_state: Int, val action: String,
           val valid: Option[UInt], val dirty: Option[UInt], val broadcast: Option[Int]) {
    def broadcast(bcast: Option[Int]): Do = {
      new Do(next_state, action, valid, dirty, bcast)
    }

    @deprecated
    def invalidate: Do = {
      new Do(next_state, action, Invalid, dirty, broadcast)
    }

    def markInvalid: Do = invalidate

    @deprecated
    def validate: Do = {
      new Do(next_state, action, Valid, dirty, broadcast)
    }

    def markValid: Do = validate

    @deprecated
    def dirtify: Do = {
      new Do(next_state, action, valid, Dirty, broadcast)
    }

    def markDirty: Do = dirtify

    @deprecated
    def clean: Do = {
      new Do(next_state, action, valid, Clean, broadcast)
    }

    def markClean: Do = clean

    def reset: Do = {
      new Do(next_state, action, Invalid, Clean, broadcast)
    }
  }
  object Do {
    def apply(next_state: Int, action: String, valid: Option[UInt], dirty: Option[UInt], broadcast: Option[Int]): Do = {
      new Do(next_state, action, valid, dirty, broadcast)
    }
    def apply(next_state: Int, action: String): Do = {
      new Do(next_state, action, Keep, Keep, None)
    }
  }
  implicit def String2Option(s: String): Option[String] = Some(s)


  io.cache_ctr_next_state_o    := CacheControllerState.IDLE
  io.cache_line_next_state_o   := 0.U//genCoherenceState.I.U
  io.cache_line_next_valid_o   := io.cache_line_valid_i
  io.cache_line_next_dirty_o   := io.cache_line_dirty_i
  io.cache_line_broadcast_type := 0.U//genRequestType.NONE_EVENT.U

  @deprecated
  def Hit: UInt = 1.U
  @deprecated
  def Miss: UInt = 0.U

  def Valid: UInt = 1.U
  def Invalid: UInt = 0.U
  def Keep: Option[UInt] = None

  def Clean: UInt = 0.U
  def Dirty: UInt = 1.U

  def table: Map[Event, Do]

  def CE = genCacheEvent
  def CohS = genCoherenceState
  def R = genRequestType

  // generate logic from previous table
  for { (event, action) <- table } {
    try {
      val event_match = event.event.U === io.cache_line_event_i
      val state_match = event.state.U === io.cache_line_state_i
      val tag_match = event.tag_match match {
        case Some(v) => v === io.cache_line_tag_match_i
        case None => true.B
      }
      val crit_diff_match = event.critDiff match {
        case Some(v) => io.crit_diff === v.U
        case None => true.B
      }

      when(event_match && state_match && tag_match && crit_diff_match) {
        // drive the output signal
        io.cache_line_next_state_o := action.next_state.U
        io.cache_ctr_next_state_o := CacheControllerState.selectDynamic(action.action)
        action.valid match {
          case Some(v) => io.cache_line_next_valid_o := v
          case None => Unit
        }

        action.dirty match {
          case Some(v) => io.cache_line_next_dirty_o := v
          case None => Unit
        }

        action.broadcast match {
          case Some(v) => io.cache_line_broadcast_type := v.U
          case None => Unit
        }

      }
    } catch {
      case e: Exception => {
        println("Exception Encountered when Processing State Transition: "
          + event.toString() + " -> " + action.toString())
        throw e
      }
    }
  }

  def getCoherenceWBStates: List[Int]

}
