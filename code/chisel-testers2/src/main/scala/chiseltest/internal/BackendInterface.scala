// See LICENSE for license details.

package chiseltest.internal

import chiseltest.Region
import chisel3.tester.Pokeable
import chisel3._

import scala.collection.mutable

trait AbstractTesterThread

class TesterThreadList(protected val elts: Seq[AbstractTesterThread]) {
  def toSeq(): Seq[AbstractTesterThread] = elts

  def join() {
    Context().backend.doJoin(elts, None)
  }

  def joinAndStep(clock: Clock) {
    Context().backend.doJoin(elts, Some(clock))
  }

  def ++(others: TesterThreadList): TesterThreadList = {
    new TesterThreadList(elts ++ others.elts)
  }

  val fork: ForkBuilder = new ForkBuilder(None, None, elts)
}

/** Common interface definition for tester backends. Internal API.
  */
trait BackendInterface {
  /** Writes a value to a clock.
    */
  def pokeClock(signal: Clock, value: Boolean): Unit

  /** Read the value of a clock.
    */
  def peekClock(signal: Clock): Boolean

  /** Writes a value to a writable wire.
    * Throws an exception if write is not writable.
    */
  def pokeBits(signal: Bits, value: BigInt): Unit

  def pokeElement[T <: Element: Pokeable](signal: T, value: BigInt): Unit

  /** Returns the current value on a wire.
    * If stale is true, returns the current combinational value (after previous pokes have taken effect).
    * If stale is false, returns the value at the beginning of the current cycle.
    */
  def peekBits(signal: Bits, stale: Boolean): BigInt

  def peekElement[T <: Element: Pokeable](signal: T, stale: Boolean): BigInt

  def expectBits(signal: Bits, value: BigInt, message: Option[String], stale: Boolean): Unit

  def expectElement[T <: Element: Pokeable](signal: T, value: BigInt, message: Option[String], stale: Boolean): Unit

  /**
   * Sets the timeout of the clock: the number of cycles the clock can advance without
   * some non-nop poke operation.
   * Setting cycles=0 disables the timeout.
   * Setting cycles=1 means every cycle must have some non-nop poke operation.
   * Resets the idle counter associated with the specified clock.
   */
  def setTimeout(signal: Clock, cycles: Int): Unit

  /** Advances the target clock by one cycle.
    */
  def step(signal: Clock, cycles: Int): Unit

  def doFork(runnable: () => Unit, name: Option[String], region: Option[Region]): AbstractTesterThread

  def doJoin(threads: Seq[AbstractTesterThread], stepAfter: Option[Clock]): Unit

  def doTimescope(contents: () => Unit): Unit

  // Circuit introspection functionality
  //
  /** Returns set of clocks associated with sources of the signal
    */
  def getSourceClocks(signal: Data): Set[Clock]

  /** Returns set of clocks associated with sinks of the signal
    */
  def getSinkClocks(signal: Data): Set[Clock]

  // Test Instance State
  //

  /** Returns the stack trace elements of parent threads. If currently in the root thread, returns
    * empty.
    * TODO: refactor this, figure out how to do this in a structurally cleaner way
    */
  def getParentTraceElements: Seq[StackTraceElement] = Seq()

  protected val testMap = mutable.HashMap[Any, Any]()

  /** Sets the value associated with a key in a per-test map.
    */
  def setVar(key: Any, value: Any): Unit = {
    testMap.put(key, value)
  }

  /** Returns the value associated with the key in a per-test map.
    */
  def getVar(key: Any): Option[Any] = {
    testMap.get(key)
  }
}

/** Backend associated with a particular circuit, and can run tests
  */
trait BackendInstance[T <: MultiIOModule] extends BackendInterface {
  /** Runs of tests are wrapped in this, for any special setup/teardown that needs to happen.
    * Takes the test function, which takes the module used as the testing interface.
    * TesterContext setup is done externally.
    *
    * Internal API
    */
  def run(testFn: T => Unit): Unit
}
