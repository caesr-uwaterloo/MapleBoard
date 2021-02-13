package chiseltest.tests

import chisel3._
import chiseltest._
import org.scalatest._
import org.scalatest.Matchers._

class ThreadSafetyLocatorTest extends FlatSpec with ChiselScalatestTester {
  behavior of "Testers2 thread safety checker"

  it should "locate source lines for simultaneous pokes from parallel threads" in {
    val exceptionMessage = intercept[ThreadOrderDependentException] {
      test(new InputOnlyModule(Bool())) { c =>
        fork {
          c.in.poke(true.B)
          c.clock.step(1)
        }.fork {
          c.in.poke(true.B)
          c.clock.step(1)
        }.join
      }
    }.getMessage()
    exceptionMessage should include ("ThreadSafetyLocatorTest.scala:15")
    exceptionMessage should include ("ThreadSafetyLocatorTest.scala:18")
  }
}
