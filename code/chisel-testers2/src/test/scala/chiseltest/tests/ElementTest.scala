package chiseltest.tests

import org.scalatest._
import chisel3._
import chisel3.experimental._
import chiseltest._

class ElementTest extends FlatSpec with ChiselScalatestTester {
  behavior of "Testers2 with Element types"

  // TODO: automatically detect overflow conditions and error out

  it should "work with UInt" in {
    test(new Module {
      val io = IO(new Bundle {
        val in1 = Input(UInt(8.W))
        val in2 = Input(UInt(8.W))
        val out = Output(UInt(8.W))

        def expect(in1Val: UInt, in2Val: UInt, outVal: UInt) {
          in1.poke(in1Val)
          in2.poke(in2Val)
          out.expect(outVal)
        }
      })
      io.out := io.in1 + io.in2
    }) { c =>
      c.io.expect(0.U, 0.U, 0.U)
      c.io.expect(1.U, 0.U, 1.U)
      c.io.expect(0.U, 1.U, 1.U)
      c.io.expect(1.U, 1.U, 2.U)
      c.io.expect(254.U, 1.U, 255.U)
      c.io.expect(255.U, 1.U, 0.U)  // overflow behavior
      c.io.expect(255.U, 255.U, 254.U)  // overflow behavior
    }
  }

  it should "work with SInt" in {
    test(new Module {
      val io = IO(new Bundle {
        val in1 = Input(SInt(8.W))
        val in2 = Input(SInt(8.W))
        val out = Output(SInt(8.W))

        def expect(in1Val: SInt, in2Val: SInt, outVal: SInt) {
          in1.poke(in1Val)
          in2.poke(in2Val)
          out.expect(outVal)
        }
      })
      io.out := io.in1 + io.in2
    }) { c =>
      c.io.expect(0.S, 0.S, 0.S)
      c.io.expect(1.S, 0.S, 1.S)
      c.io.expect(0.S, 1.S, 1.S)
      c.io.expect(1.S, 1.S, 2.S)

      c.io.expect(127.S, -1.S, 126.S)
      c.io.expect(127.S, -127.S, 0.S)
      c.io.expect(-128.S, 127.S, -1.S)
      c.io.expect(-126.S, 127.S, 1.S)

      c.io.expect(127.S, 1.S, -128.S)
      c.io.expect(-128.S, -1.S, 127.S)
      c.io.expect(127.S, 127.S, -2.S)
      c.io.expect(-128.S, -128.S, 0.S)
    }
  }

  it should "work with Bool" in {
    test(new Module {
      val io = IO(new Bundle {
        val in1 = Input(Bool())
        val in2 = Input(Bool())
        val outAnd = Output(Bool())
        val outOr = Output(Bool())

        def expect(in1Val: Bool, in2Val: Bool, andVal: Bool, orVal: Bool) {
          in1.poke(in1Val)
          in2.poke(in2Val)
          outAnd.expect(andVal)
          outOr.expect(orVal)
        }
      })
      io.outAnd := io.in1 && io.in2
      io.outOr := io.in1 || io.in2
    }) { c =>
      c.io.expect(true.B, true.B, true.B, true.B)
      c.io.expect(false.B, false.B, false.B, false.B)
      c.io.expect(true.B, false.B, false.B, true.B)
      c.io.expect(false.B, true.B, false.B, true.B)
    }
  }

  it should "work with FixedPoint" in {
    import chisel3.experimental.FixedPoint
    test(new Module {
      val io = IO(new Bundle {
        val in1 = Input(FixedPoint(8.W, 2.BP))
        val in2 = Input(FixedPoint(8.W, 2.BP))
        val out = Output(FixedPoint(8.W, 2.BP))

        def expect(in1Val: FixedPoint, in2Val: FixedPoint, outVal: FixedPoint) {
          in1.poke(in1Val)
          in2.poke(in2Val)
          out.expect(outVal)
        }
      })
      io.out := io.in1 + io.in2
    }) { c =>
      c.io.expect(0.F(2.BP), 0.F(2.BP), 0.F(2.BP))
      c.io.expect(1.F(2.BP), 1.F(2.BP), 2.F(2.BP))
      c.io.expect(0.5.F(2.BP), 0.5.F(2.BP), 1.F(2.BP))
      c.io.expect(0.5.F(2.BP), -0.5.F(2.BP), 0.F(2.BP))

      // Overflow test, treating it as a 6-bit signed int
      c.io.expect(31.F(2.BP), 1.F(2.BP), -32.F(2.BP))
      c.io.expect(-32.F(2.BP), -1.F(2.BP), 31.F(2.BP))

      c.io.expect(31.F(2.BP), 31.F(2.BP), -2.F(2.BP))
      c.io.expect(-32.F(2.BP), -32.F(2.BP), 0.F(2.BP))

      // Overflow test with decimal component
      c.io.expect(31.75.F(2.BP), 31.75.F(2.BP), -0.5.F(2.BP))
      c.io.expect(31.75.F(2.BP), 0.25.F(2.BP), -32.F(2.BP))
    }
  }

  it should "work with Interval" in {

    val inputRange = range"[-6, 6].2"
    val outputRange = range"[-4.5,4.5].2"

    test(new Module {
      val io = IO(new Bundle {
        val input = Input(Interval(inputRange))

        val out1 = Output(Interval(inputRange))
        val out1Clip = Output(Interval(outputRange))
        val out1Squeeze = Output(Interval(outputRange))
        val out1Wrap = Output(Interval(outputRange))
      })
      io.out1 := io.input
      io.out1Clip := io.input.clip(io.out1Clip)
      io.out1Squeeze := io.input.squeeze(io.out1Squeeze)
      io.out1Wrap := io.input.wrap(io.out1Wrap)
    }) { c =>

      def checkOutcome(input: Interval, clipped: Interval, wrapped: Interval): Unit = {
        c.io.input.poke(input)

        c.io.out1.expect(input)
        c.io.out1Clip.expect(clipped)
        c.io.out1Wrap.expect(wrapped)
      }

      checkOutcome((-6.0).I(inputRange), (-4.5).I(outputRange), 3.25.I(outputRange))
      checkOutcome((-3.25).I(inputRange), (-3.25).I(outputRange), (-3.25).I(outputRange))
      checkOutcome(0.I(inputRange), 0.I(outputRange), 0.I(outputRange))
      checkOutcome(4.5.I(inputRange), 4.5.I(outputRange), 4.5.I(outputRange))
      checkOutcome(4.75.I(inputRange), 4.5.I(outputRange), (-4.5).I(outputRange))
    }
  }
}
