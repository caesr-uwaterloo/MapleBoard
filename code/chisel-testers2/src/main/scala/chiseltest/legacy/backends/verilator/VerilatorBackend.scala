// See LICENSE for license details.

package chiseltest.legacy.backends.verilator

import chiseltest.internal.{BackendInstance, Context, ThreadedBackend}
import chiseltest.legacy.backends.verilator.Utils.unsignedBigIntToSigned
import chiseltest.{Region, TimeoutException, ClockResolutionException}
import chisel3.experimental.{DataMirror, FixedPoint, Interval, EnumType}
import chisel3.internal.firrtl.KnownWidth
import chisel3.{SInt, _}
import chisel3.tester.Pokeable

import scala.collection.mutable
import scala.math.BigInt

/** Supports Backend and Threaded traits for ex
  *
  * @param dut                  the device under test
  * @param dataNames            basically the IO ports
  * @param combinationalPaths   paths detected by CheckCombLoop
  * @param command              the simulation program to execute
  * @tparam T                   the dut's type
  */
// TODO: is Seq[CombinationalPath] the right API here? It's unclear where name -> Data resolution should go
class VerilatorBackend[T <: MultiIOModule](
  val dut: T,
  val dataNames: Map[Data, String],
  val combinationalPaths: Map[Data, Set[Data]],
  command: Seq[String]
) extends BackendInstance[T]
    with ThreadedBackend[T] {

  private[chiseltest] val simApiInterface = new SimApiInterface(dut, command)

  //
  // Debug utility functions
  //
  val verbose: Boolean = false // hard-coded debug flag
  def debugLog(str: => String) {
    if (verbose) println(str)
  }

  protected def resolveName(signal: Data): String = { // TODO: unify w/ dataNames?
    dataNames.getOrElse(signal, signal.toString)
  }

  //
  // Circuit introspection functionality
  //
  override def getSourceClocks(signal: Data): Set[Clock] = {
    throw new ClockResolutionException(
      "ICR not available on chisel-testers2 / firrtl master"
    )
  }

  override def getSinkClocks(signal: Data): Set[Clock] = {
    throw new ClockResolutionException(
      "ICR not available on chisel-testers2 / firrtl master"
    )
  }

  //
  // Everything else
  //

  def getModule: T = dut

  override def pokeClock(signal: Clock, value: Boolean): Unit = {
    // TODO: check thread ordering

    val intValue = if (value) 1 else 0
    simApiInterface.poke(dataNames(signal), intValue)
    debugLog(s"${resolveName(signal)} <- $intValue")
  }

  override def peekClock(signal: Clock): Boolean = {
    doPeek(signal, new Throwable)
    val a = simApiInterface.peek(dataNames(signal)).getOrElse(BigInt(0))
    debugLog(s"${resolveName(signal)} -> $a")
    a > 0
  }

  override def pokeBits(signal: Bits, value: BigInt): Unit = {
    doPoke(signal, value, new Throwable)
    val dataName = dataNames(signal)
    if (simApiInterface.peek(dataName).get != value) {
      idleCycles.clear()
    }
    simApiInterface.poke(dataNames(signal), value)
    debugLog(s"${resolveName(signal)} <- $value")
  }

  override def pokeElement[D <: Element: Pokeable](signal: D, value: BigInt): Unit = {
    doPoke(signal, value, new Throwable)
    val dataName = dataNames(signal)
    if (simApiInterface.peek(dataName).get != value) {
      idleCycles.clear()
    }
    simApiInterface.poke(dataNames(signal), value)
    debugLog(s"${resolveName(signal)} <- $value")
  }

  override def peekBits(signal: Bits, stale: Boolean): BigInt = {
    require(!stale, "Stale peek not yet implemented")

    doPeek(signal, new Throwable)
    val dataName = dataNames(signal)
    val a = simApiInterface.peek(dataName).get
    debugLog(s"${resolveName(signal)} -> $a")

    signal match {
      case s: SInt =>
        val width = DataMirror.widthOf(s).asInstanceOf[KnownWidth].value
        unsignedBigIntToSigned(a, width)
      case f: FixedPoint =>
        val width = DataMirror.widthOf(f).asInstanceOf[KnownWidth].value
        unsignedBigIntToSigned(a, width)
      case i: Interval =>
        val width = DataMirror.widthOf(i).asInstanceOf[KnownWidth].value
        unsignedBigIntToSigned(a, width)
      case _ => a
    }
  }

  override def peekElement[D <: Element: Pokeable](signal: D, stale: Boolean): BigInt = {
    require(!stale, "Stale peek not yet implemented")

    doPeek(signal, new Throwable)
    val dataName = dataNames(signal)
    val a = simApiInterface.peek(dataName).get
    debugLog(s"${resolveName(signal)} -> $a")

    signal match {
      case s: Bits => peekBits(s, stale)
      case e: EnumType => a
      case _ => throw BackendException("expected Pokeable types") // this should not happen as it will be captured by the compiler
    }
  }

  override def expectBits(signal: Bits,
                          value: BigInt,
                          message: Option[String],
                          stale: Boolean): Unit = {
    require(!stale, "Stale peek not yet implemented")

    debugLog(s"${resolveName(signal)} ?> $value")
    Context().env.testerExpect(
      value,
      peekBits(signal, stale),
      resolveName(signal),
      message
    )
  }

  override def expectElement[D <: Element: Pokeable](signal: D,
                          value: BigInt,
                          message: Option[String],
                          stale: Boolean): Unit = {
    require(!stale, "Stale peek not yet implemented")

    debugLog(s"${resolveName(signal)} ?> $value")
    Context().env.testerExpect(
      value,
      peekElement(signal, stale),
      resolveName(signal),
      message
    )
  }

  protected val clockCounter: mutable.HashMap[Clock, Int] = mutable.HashMap()
  protected def getClockCycle(clk: Clock): Int = {
    clockCounter.getOrElse(clk, 0)
  }
  protected def getClock(clk: Clock): Boolean =
    simApiInterface.peek(dataNames(clk)) match {
      case Some(x) if x == BigInt(1) => true
      case _                         => false
    }

  protected val lastClockValue: mutable.HashMap[Clock, Boolean] =
    mutable.HashMap()

  override def doTimescope(contents: () => Unit): Unit = {
    val createdTimescope = newTimescope()

    contents()

    closeTimescope(createdTimescope).foreach {
      case (data, valueOption) =>
        valueOption match {
          case Some(value) =>
            if (simApiInterface.peek(dataNames(data)).get != value) {
              idleCycles.clear()
            }
            simApiInterface.poke(dataNames(data), value)
            debugLog(s"${resolveName(data)} <- (revert) $value")
          case None =>
            idleCycles.clear()
            simApiInterface.poke(dataNames(data), 0) // TODO: randomize or 4-state sim
            debugLog(s"${resolveName(data)} <- (revert) DC")
        }
    }
  }

  override def step(signal: Clock, cycles: Int): Unit = {
    // TODO: maybe a fast condition for when threading is not in use?
    for (_ <- 0 until cycles) {
      // If a new clock, record the current value so change detection is instantaneous
      if (signal != dut.clock && !lastClockValue.contains(signal)) {
        lastClockValue.put(signal, getClock(signal))
      }

      val thisThread = currentThread.get
      thisThread.clockedOn = Some(signal)
      schedulerState.currentThreadIndex += 1
      scheduler()
      thisThread.waiting.acquire()
    }
  }

  override def run(testFn: T => Unit): Unit = {
    rootTimescope = Some(new RootTimescope)
    val mainThread = new TesterThread(
      () => {
        simApiInterface.poke("reset", 1)
        simApiInterface.step(1)
        simApiInterface.poke("reset", 0)

        testFn(dut)
      },
      TimeRegion(0, Region.default),
      rootTimescope.get,
      0,
      Region.default,
      None
    )
    mainThread.thread.start()
    require(allThreads.isEmpty)
    allThreads += mainThread

    try {
      while (!mainThread.done) { // iterate timesteps
        clockCounter.put(dut.clock, getClockCycle(dut.clock) + 1)

        debugLog(s"clock step")

        // TODO: allow dependent clocks to step based on test stimulus generator
        // TODO: remove multiple invocations of getClock
        // Unblock threads waiting on dependent clock
        val steppedClocks = Seq(dut.clock) ++ lastClockValue.collect {
          case (clock, lastValue)
              if getClock(clock) != lastValue && getClock(clock) =>
            clock
        }
        steppedClocks foreach { clock =>
          clockCounter.put(dut.clock, getClockCycle(clock) + 1) // TODO: ignores cycles before a clock was stepped on
        }
        lastClockValue foreach {
          case (clock, _) =>
            lastClockValue.put(clock, getClock(clock))
        }

        runThreads(steppedClocks.toSet)
        Context().env.checkpoint()

        idleLimits foreach {
          case (clock, limit) =>
            idleCycles.put(clock, idleCycles.getOrElse(clock, -1) + 1)
            if (idleCycles(clock) >= limit) {
              throw new TimeoutException(
                s"timeout on $clock at $limit idle cycles"
              )
            }
        }

        simApiInterface.step(1)
      }
    } finally {
      rootTimescope = None

      for (thread <- allThreads.clone()) {
        // Kill the threads using an InterruptedException
        if (thread.thread.isAlive) {
          thread.thread.interrupt()
        }
      }

      simApiInterface.finish() // Do this to close down the communication
    }
  }
}
