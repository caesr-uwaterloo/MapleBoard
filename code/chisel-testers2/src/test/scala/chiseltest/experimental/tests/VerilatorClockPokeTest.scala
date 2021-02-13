package chiseltest.experimental.tests

import chisel3._
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.experimental.UncheckedClockPoke._
import chiseltest.internal.{TreadleBackendAnnotation, VerilatorBackendAnnotation}
import chisel3.util._
import org.scalatest._
import treadle.executable.ClockInfo
import treadle.{ClockInfoAnnotation, WriteVcdAnnotation}

class VerilatorClockPokeTest extends FlatSpec with ChiselScalatestTester {
  behavior of "Testers2 with a clock input"

  it should "verilator-clock-poke" in {
    test(new MultiIOModule {
      val inClock = IO(Input(Clock()))
      val out = IO(Output(UInt(8.W)))

      withClock(inClock) {
        out := Counter(true.B, 8)._1
      }
    }).withAnnotations(Seq(VerilatorBackendAnnotation)) { c =>
      c.inClock.low()
      c.out.expect(0.U)

      // Main clock should do nothing
      c.clock.step()
      c.out.expect(0.U)
      c.clock.step()
      c.out.expect(0.U)

      // Output should advance on rising edge, even without main clock edge
      c.inClock.high()
      c.out.expect(1.U)

      c.clock.step()
      c.out.expect(1.U)

      // Repeated high should do nothing
      c.inClock.high()
      c.out.expect(1.U)

      // and again
      c.inClock.low()
      c.out.expect(1.U)
      c.inClock.high()
      c.out.expect(2.U)
    }
  }

  it should "clock-as-bool-verilator-clock-poke" in {
    test(new MultiIOModule {
      val inClock = IO(Input(Bool()))
      val out = IO(Output(UInt(8.W)))

      withClock(inClock.asClock()) {
        out := Counter(true.B, 8)._1
      }
    }).withAnnotations(Seq(VerilatorBackendAnnotation)) { c =>
      //      c.inClock.low()
      c.inClock.poke(false.B)
      c.out.expect(0.U)

      // Main clock should do nothing
      c.clock.step()
      c.out.expect(0.U)
      c.clock.step()
      c.out.expect(0.U)

      // Output should advance on rising edge, even without main clock edge
      c.inClock.poke(true.B)
      c.out.expect(1.U)
    }
  }

  it should "treadle-clock-poke" in {
    test(new MultiIOModule {
      val inClock = IO(Input(Clock()))
      val out = IO(Output(UInt(8.W)))

      withClock(inClock) {
        out := Counter(true.B, 8)._1
      }
    }).withAnnotations(
      Seq(VerilatorBackendAnnotation, WriteVcdAnnotation, ClockInfoAnnotation(Seq(ClockInfo(period = 2))))
    ) { c =>
      c.inClock.low()
      c.out.expect(0.U)

      // Main clock should do nothing
      c.clock.step()
      c.out.expect(0.U)
      c.clock.step()
      c.out.expect(0.U)

      // Output should advance on rising edge, even without main clock edge
      c.inClock.high()
      c.out.expect(1.U)
      c.clock.step()
      c.out.expect(1.U)

      // Repeated high should do nothing
      c.inClock.high()
      c.out.expect(1.U)

      // and again
      c.inClock.low()
      c.out.expect(1.U)
      c.inClock.high()
      c.out.expect(2.U)
    }
  }
}
