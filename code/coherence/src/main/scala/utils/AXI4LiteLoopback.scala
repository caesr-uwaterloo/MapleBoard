
package utils

import chisel3._
import chisel3.util._
import components.AXI4Lite

class AXI4LiteLoopback extends MultiIOModule {
  val s_axi = IO(Flipped(new AXI4Lite(64, 64)))
  val m_axi = IO(new AXI4Lite(64, 64))
  val rAddressQ = Module(new Queue(new Bundle {
    val addr = UInt(64.W)
    val prot = UInt(3.W)
  }, 1))
  val wAddressQ = Module(new Queue(new Bundle {
    val addr = UInt(64.W)
    val prot = UInt(3.W)
  }, 1))
  val rDataQ = Module(new Queue(new Bundle {
    val data = UInt(64.W)
    val rresp = UInt(2.W)
  }, 1))
  val wDataQ = Module(new Queue(new Bundle {
    val data = UInt(64.W)
    val strb = UInt(8.W)
  }, 1))
  val bRespQ = Module(new Queue(new Bundle {
    val bresp = UInt(2.W)
  }, 1))
  // archannel
  rAddressQ.io.enq.valid := s_axi.arvalid
  rAddressQ.io.enq.bits.addr := s_axi.araddr
  rAddressQ.io.enq.bits.prot := s_axi.arprot
  s_axi.arready := rAddressQ.io.enq.ready

  m_axi.arvalid := rAddressQ.io.deq.valid
  m_axi.arprot := rAddressQ.io.deq.bits.prot
  m_axi.araddr := rAddressQ.io.deq.bits.addr
  rAddressQ.io.deq.ready := m_axi.arready

  // rdata
  rDataQ.io.enq.valid := m_axi.rvalid
  rDataQ.io.enq.bits.data := m_axi.rdata
  rDataQ.io.enq.bits.rresp := m_axi.rresp
  m_axi.rready := rDataQ.io.enq.ready

  s_axi.rdata := rDataQ.io.deq.bits.data
  s_axi.rresp := rDataQ.io.deq.bits.rresp
  s_axi.rvalid := rDataQ.io.deq.valid
  rDataQ.io.deq.ready := s_axi.rready

  // awaddr
  wAddressQ.io.enq.valid := s_axi.awvalid
  wAddressQ.io.enq.bits.addr := s_axi.awaddr
  wAddressQ.io.enq.bits.prot := s_axi.awprot
  s_axi.awready := wAddressQ.io.enq.ready

  m_axi.awvalid := wAddressQ.io.deq.valid
  m_axi.awprot := wAddressQ.io.deq.bits.prot
  m_axi.awaddr := wAddressQ.io.deq.bits.addr
  wAddressQ.io.deq.ready := m_axi.awready

  // wdata
  wDataQ.io.enq.valid := s_axi.wvalid
  wDataQ.io.enq.bits.data := s_axi.wdata + "h10".U
  wDataQ.io.enq.bits.strb := s_axi.wstrb
  s_axi.wready := wDataQ.io.enq.bits.data

  wDataQ.io.deq.ready := m_axi.wready
  m_axi.wstrb := wDataQ.io.deq.bits.strb
  m_axi.wdata := wDataQ.io.deq.bits.data
  m_axi.wvalid := wDataQ.io.deq.valid

  // bresp
  bRespQ.io.enq.valid := m_axi.bvalid
  bRespQ.io.enq.bits.bresp := m_axi.bresp
  m_axi.bready := bRespQ.io.enq.ready

  s_axi.bresp := bRespQ.io.deq.bits.bresp
  s_axi.bvalid := bRespQ.io.deq.valid
  bRespQ.io.deq.ready := s_axi.bready
}

object AXI4LiteLoopback extends App {

  chisel3.Driver.execute(args, () => new AXI4LiteLoopback())
}