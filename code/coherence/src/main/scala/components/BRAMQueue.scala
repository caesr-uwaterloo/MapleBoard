
package components

import chisel3._
import chisel3.util._

// Generates a queue that will be implemented in BRAM
class BRAMFlowQueue[T <: Data](val entries: Int)(data: => T) extends Module {
  val io = IO(new QueueIO(data, entries))
  require(entries > 1)

  io.count := 0.U

  val do_flow = Wire(Bool())
  val do_enq = io.enq.fire() && !do_flow
  val do_deq = io.deq.fire() && !do_flow

  val maybe_full = RegInit(false.B)
  val enq_ptr = Counter(do_enq, entries)._1
  val (deq_ptr, deq_done) = Counter(do_deq, entries)
  when (do_enq =/= do_deq) { maybe_full := do_enq }

  val ptr_match = enq_ptr === deq_ptr
  val empty = ptr_match && !maybe_full
  val full = ptr_match && maybe_full
  val atLeastTwo = full || enq_ptr - deq_ptr >= 2.U
  do_flow := empty && io.deq.ready

  // val ram = Mem(entries, data)
  val ram = Module(new BRAMVerilog(entries, data.getWidth))
  ram.io.clock := clock
  ram.io.reset := reset

  ram.io.we := do_enq
  ram.io.waddr := enq_ptr
  ram.io.wdata := io.enq.bits.asUInt
  // when (do_enq) { ram.write(enq_ptr, io.enq.bits) }

  val ren = io.deq.ready && (atLeastTwo || !io.deq.valid && !empty)
  val raddr = Mux(io.deq.valid, Mux(deq_done, 0.U, deq_ptr + 1.U), deq_ptr)
  val ram_out_valid = RegNext(ren)

  io.deq.valid := Mux(empty, io.enq.valid, ram_out_valid)
  io.enq.ready := !full
  // io.deq.bits := Mux(empty, io.enq.bits, RegEnable(ram.read(raddr), ren))
  ram.io.raddr := raddr
  io.deq.bits := Mux(empty, io.enq.bits, RegEnable(ram.io.rdata.asTypeOf(data), ren))
}

class BRAMQueue[T <: Data](val entries: Int)(data: => T) extends Module {
  val io = IO(new QueueIO(data, entries))

  io.count := 0.U

  val fq = Module(new BRAMFlowQueue(entries)(data))
  fq.io.enq <> io.enq
  io.deq <> Queue(fq.io.deq, 1, pipe = true)
}

/*
object BRAMQueue {
  def apply[T <: Data](enq: DecoupledIO[T], entries: Int) = {
    val q = Module((new BRAMQueue(entries)) { enq.bits.cloneType })
    q.io.enq.valid := enq.valid // not using <> so that override is allowed
    q.io.enq.bits := enq.bits
    enq.ready := q.io.enq.ready
    q.io.deq
  }
}
 */
