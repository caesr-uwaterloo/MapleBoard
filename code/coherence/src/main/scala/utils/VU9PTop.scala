
package utils

import chisel3._
import chisel3.experimental.Analog
import components.AXI4X
import param.CoreParam
import params.{CoherenceSpec, MemorySystemParams}

object VU9PTop extends App {
  case class Configuration(nCore: Int, nLines: Int, protocol: String, bus: String)
  def parseArguments(args: List[String], res: Configuration = Configuration(0, 0, "", "")):
  (Configuration, List[String]) = {
    args match {
      case "--core" :: value :: tail => parseArguments(tail, res.copy(nCore = value.toInt))
      case "--line" :: value :: tail => parseArguments(tail, res.copy(nLines = value.toInt))
      case "--protocol" :: value :: tail => parseArguments(tail, res.copy(protocol = value))
      case "--bus" :: value :: tail => parseArguments(tail, res.copy(bus = value))
      case head :: tail => {
        val (conf, a) = parseArguments(tail, res)
        (conf, head :: a)
      }
      case _ => (res, args)
    }
  }
  val (res, otherOpt) = parseArguments(args.toList)
  require(res.nCore > 0)
  require(res.nLines > 0)
  require(res.protocol.length > 0)
  require(res.bus.length > 0)
  val (c, m, prot) = res match {
    case Configuration(n, l, "pmsi", "shared") => {
      val params = new platforms.TargetPlatformPMSI(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "msi", "shared") => {
      val params = new platforms.TargetPlatformMSI(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "mesi", "shared") => {
      val params = new platforms.TargetPlatformMESI(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmsi", "dedicated") => {
      val params = new platforms.TargetPlatformPMSIDedicated(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmsi", "atomic") => {
      val params = new platforms.TargetPlatformPMSIAtomic(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmesi", "shared") => {
      val params = new platforms.TargetPlatformPMESI(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmesi", "dedicated") => {
      val params = new platforms.TargetPlatformPMESIDedicated(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmesi", "atomic") => {
      val params = new platforms.TargetPlatformPMESIAtomic(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmesi", "atomicModified") => {
      val params = new platforms.TargetPlatformPMESIAtomic(n, l, true)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "pmsi", "atomicModified") => {
      val params = new platforms.TargetPlatformPMSIAtomic(n, l, true)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "carp", "shared") => {
      val params = new platforms.TargetPlatformCARP(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "carp-no-e", "shared") => {
      val params = new platforms.TargetPlatformCARPNoE(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case Configuration(n, l, "carp", "dedicated") => {
      val params = new platforms.TargetPlatformCARPDedicated(n, l)
      (params.coreParam, params.memorySystemParams, params.coherenceSpec)
    }
    case _ => {
      println("Unsupported feature")
      throw new RuntimeException("Unsupported feature")
    }
  }

  println(m.slotWidth)
  println(res.nCore)
  println(res.nLines)
  val confString = s"${res.protocol}.${res.nCore}c.${res.nLines}.${res.bus}"
  chisel3.Driver.execute(("-td" :: confString :: otherOpt).toArray, () => new VU9PTop(c, m, prot))
}

class VU9PTop[S <: Data, M <: Data, B <: Data](coreParam: CoreParam,
               memorySystemParams: MemorySystemParams,
              coherenceSpec: CoherenceSpec[S, M, B]) extends RawModule {
  val pci_width = 1
  // differential clock
  val sys = IO(new Bundle {
    val clk_p = Input(Bool())
    val clk_n = Input(Bool())
  })
  // pci-e channels
  val pci = IO(new Bundle {
    val express_x16_rxn = Input(UInt(pci_width.W))
    val express_x16_rxp = Input(UInt(pci_width.W))
    val express_x16_txn = Output(UInt(pci_width.W))
    val express_x16_txp = Output(UInt(pci_width.W))
    /*
    val express_x8_rxn = Input(UInt(8.W))
    val express_x8_rxp = Input(UInt(8.W))
    val express_x8_txn = Output(UInt(8.W))
    val express_x8_txp = Output(UInt(8.W))
    */

    /*
    val express_x1_rxn = Input(UInt(1.W))
    val express_x1_rxp = Input(UInt(1.W))
    val express_x1_txn = Output(UInt(1.W))
    val express_x1_txp = Output(UInt(1.W))
     */

  })
  val pcie = IO(new Bundle {
    val perstn = Input(Bool())
    val refclk_clk_n = Input(Bool())
    val refclk_clk_p = Input(Bool())
  })

  val sh = Module(new VU9PShell())
  val confreg_rst = Wire(Bool())
  val initPC = Wire(UInt(64.W))


  val req_valid = Wire(Bool())
  val req_ready = Wire(Bool())
  val req_bits = Wire(memorySystemParams.getGenCacheReq)

  val resp_valid = Wire(Bool())
  val resp_ready = Wire(Bool())
  val resp_bits = Wire(memorySystemParams.getGenCacheResp)

  val lat = Wire(Vec(memorySystemParams.masterCount + 1, UInt(64.W)))

  val C1_DDR4_0 = IO(new Bundle {
    val act_n = Output(UInt(1.W))
    val adr = Output(UInt(17.W))
    val ba = Output(UInt(2.W))
    val bg = Output(UInt(2.W))
    val ck_c = Output(UInt(1.W))
    val ck_t = Output(UInt(1.W))
    val cke = Output(UInt(1.W))
    val cs_n = Output(UInt(1.W))
    val dm_n = Analog(8.W)
    val dq = Analog(64.W)
    val dqs_c = Analog(8.W)
    val dqs_t = Analog(8.W)
    val odt = Output(UInt(1.W))
    val reset_n = Output(UInt(1.W))

  })
  val dimm1 = IO(new Bundle {
    val refclk_clk_n = Input(UInt(1.W))
    val refclk_clk_p = Input(UInt(1.W))
  })


  // Clock
  sh.io.sys_clk_clk_n := sys.clk_n
  sh.io.sys_clk_clk_p := sys.clk_p

  // PCI-E data channels, reset and refclk
  sh.io.pci_express_x16_rxn := pci.express_x16_rxn
  sh.io.pci_express_x16_rxp := pci.express_x16_rxp
  pci.express_x16_txn := sh.io.pci_express_x16_txn
  pci.express_x16_txp := sh.io.pci_express_x16_txp

  /*
  sh.io.pci_express_x8_rxn := pci.express_x8_rxn
  sh.io.pci_express_x8_rxp := pci.express_x8_rxp
  pci.express_x8_txn := sh.io.pci_express_x8_txn
  pci.express_x8_txp := sh.io.pci_express_x8_txp
   */

  sh.io.pcie_perstn := pcie.perstn
  sh.io.pcie_refclk_clk_n := pcie.refclk_clk_n

  sh.io.pcie_refclk_clk_p := pcie.refclk_clk_p


  // Now the configuration register
  withClockAndReset(sh.io.root_clk, sh.io.peripheral_reset_0) {
    val confreg = Module(new Confreg(memorySystemParams))

    confreg.s_axi.ar.bits.id     := 0.U
    confreg.s_axi.ar.bits.addr   := sh.io.CONF_araddr
    confreg.s_axi.ar.bits.len    := sh.io.CONF_arlen
    confreg.s_axi.ar.bits.size   := sh.io.CONF_arsize
    confreg.s_axi.ar.bits.burst  := AXI4X.BurstType(sh.io.CONF_arburst)
    confreg.s_axi.ar.bits.lock   := sh.io.CONF_arlock
    confreg.s_axi.ar.bits.cache  := sh.io.CONF_arcache
    confreg.s_axi.ar.bits.prot   := sh.io.CONF_arprot
    confreg.s_axi.ar.bits.region := 0.U // sh.io.CONF_arregion
    confreg.s_axi.ar.bits.qos    := sh.io.CONF_arqos

    sh.io.CONF_arready := confreg.s_axi.ar.ready
    confreg.s_axi.ar.valid := sh.io.CONF_arvalid

    confreg.s_axi.aw.bits.id     := 0.U
    confreg.s_axi.aw.bits.addr   := sh.io.CONF_awaddr
    confreg.s_axi.aw.bits.len    := sh.io.CONF_awlen
    confreg.s_axi.aw.bits.size   := sh.io.CONF_awsize
    confreg.s_axi.aw.bits.burst  := AXI4X.BurstType(sh.io.CONF_awburst)
    confreg.s_axi.aw.bits.lock   := sh.io.CONF_awlock
    confreg.s_axi.aw.bits.cache  := sh.io.CONF_awcache
    confreg.s_axi.aw.bits.prot   := sh.io.CONF_awprot
    confreg.s_axi.aw.bits.region := 0.U // sh.io.CONF_awregion

    confreg.s_axi.aw.bits.qos    := sh.io.CONF_awqos

    sh.io.CONF_awready := confreg.s_axi.aw.ready
    confreg.s_axi.aw.valid := sh.io.CONF_awvalid

    confreg.s_axi.b.ready := sh.io.CONF_bready
    sh.io.CONF_bresp := confreg.s_axi.b.bits.resp.asUInt
    sh.io.CONF_bvalid := confreg.s_axi.b.valid

    sh.io.CONF_rdata := confreg.s_axi.r.bits.data
    confreg.s_axi.r.ready := sh.io.CONF_rready
    sh.io.CONF_rresp := confreg.s_axi.r.bits.resp.asUInt()
    sh.io.CONF_rlast := confreg.s_axi.r.bits.last
    sh.io.CONF_rvalid := confreg.s_axi.r.valid

    confreg.s_axi.w.bits.data := sh.io.CONF_wdata
    confreg.s_axi.w.bits.id := 0.U
    sh.io.CONF_wready := confreg.s_axi.w.ready
    confreg.s_axi.w.bits.strb := sh.io.CONF_wstrb
    confreg.s_axi.w.bits.last := sh.io.CONF_wlast
    confreg.s_axi.w.valid := sh.io.CONF_wvalid

    confreg.stats.lat := lat
    dontTouch(confreg.reg)
    dontTouch(confreg.s_axi)

    confreg_rst := confreg.reg.resetReg(0)
    initPC := confreg.reg.initPC

    val axireq = Module(new AXI4ToReq(memorySystemParams))

    dontTouch(axireq.s_axi)

    // Now the slot
    axireq.s_axi.ar.bits.addr   := sh.io.SLOT_araddr
    axireq.s_axi.ar.bits.burst  := AXI4X.BurstType.INCR
    axireq.s_axi.ar.bits.cache  := 0.U // sh.io.SLOT_arcache
    axireq.s_axi.ar.bits.len    := 0.U // sh.io.SLOT_arlen
    axireq.s_axi.ar.bits.lock   := 0.U // sh.io.SLOT_arlock
    axireq.s_axi.ar.bits.prot   := sh.io.SLOT_arprot
    axireq.s_axi.ar.bits.qos    := 0.U // sh.io.SLOT_arqos
    sh.io.SLOT_arready          := axireq.s_axi.ar.ready
    axireq.s_axi.ar.bits.region := 0.U
    axireq.s_axi.ar.bits.size   := "b11".U // sh.io.SLOT_arsize
    axireq.s_axi.ar.valid       := sh.io.SLOT_arvalid
    axireq.s_axi.ar.bits.id     := 0.U

    axireq.s_axi.aw.bits.addr   := sh.io.SLOT_awaddr
    axireq.s_axi.aw.bits.burst  := AXI4X.BurstType.INCR
    axireq.s_axi.aw.bits.cache  := 0.U // sh.io.SLOT_awcache
    axireq.s_axi.aw.bits.len    := 0.U // sh.io.SLOT_awlen
    axireq.s_axi.aw.bits.lock   := 0.U // sh.io.SLOT_awlock
    axireq.s_axi.aw.bits.prot   := sh.io.SLOT_awprot
    axireq.s_axi.aw.bits.qos    := 0.U // sh.io.SLOT_awqos
    axireq.s_axi.aw.bits.region := 0.U
    axireq.s_axi.aw.bits.size   := "b11".U // sh.io.SLOT_awsize
    axireq.s_axi.aw.valid       := sh.io.SLOT_awvalid
    sh.io.SLOT_awready          := axireq.s_axi.aw.ready
    axireq.s_axi.aw.bits.id     := 0.U

    axireq.s_axi.b.ready := sh.io.SLOT_bready
    sh.io.SLOT_bresp  := axireq.s_axi.b.bits.resp.asUInt
    sh.io.SLOT_bvalid := axireq.s_axi.b.valid

    sh.io.SLOT_rdata  := axireq.s_axi.r.bits.data
    // sh.io.SLOT_rlast  := axireq.s_axi.r.bits.last
    sh.io.SLOT_rresp  := axireq.s_axi.r.bits.resp.asUInt
    sh.io.SLOT_rvalid := axireq.s_axi.r.valid

    axireq.s_axi.r.ready := sh.io.SLOT_rready

    axireq.s_axi.w.bits.id   := 0.U
    axireq.s_axi.w.bits.data := sh.io.SLOT_wdata
    axireq.s_axi.w.bits.last := 1.U // sh.io.SLOT_wlast
    axireq.s_axi.w.bits.strb := sh.io.SLOT_wstrb
    axireq.s_axi.w.valid := sh.io.SLOT_wvalid
    sh.io.SLOT_wready := axireq.s_axi.w.ready

    req_valid := axireq.io.req.valid
    req_bits := axireq.io.req.bits
    axireq.io.req.ready := req_ready

    axireq.io.resp.valid := resp_valid
    axireq.io.resp.bits  := resp_bits
    resp_ready := axireq.io.resp.ready

  }

  resp_valid := false.B
  req_ready := false.B
  resp_bits := 0.U.asTypeOf(resp_bits)

  withClockAndReset(sh.io.root_clk, confreg_rst) {
    val mem = Module(new PipelinedCoreGroupAXI(coreParam, memorySystemParams, coherenceSpec))
    mem.m.initPC := initPC
    // This part is handled in the AXI to PCIe region
    mem.m.baseAddress := 0.U

    mem.m.slot_req.valid := req_valid
    mem.m.slot_req.bits := req_bits
    req_ready := mem.m.slot_req.ready

    resp_valid := mem.m.slot_resp.valid
    resp_bits := mem.m.slot_resp.bits
    mem.m.slot_resp.ready := resp_ready

    // connect the axi interface
    sh.io.SMEM_araddr    := mem.m_axi.araddr + "h120000000".U // this part is set in the blcok design design
    sh.io.SMEM_arburst   := mem.m_axi.arburst
    sh.io.SMEM_arcache   := mem.m_axi.arcache
    sh.io.SMEM_arid      := mem.m_axi.arid
    sh.io.SMEM_arlen     := mem.m_axi.arlen
    sh.io.SMEM_arlock    := mem.m_axi.arlock
    sh.io.SMEM_arprot    := mem.m_axi.arprot
    sh.io.SMEM_arqos     := 0.U // mem.m_axi.arqos
    mem.m_axi.arready    := sh.io.SMEM_arready
    // val SMEM_arregion = Input(UInt(4.W))
    sh.io.SMEM_arsize    := mem.m_axi.arsize
    sh.io.SMEM_arvalid   := mem.m_axi.arvalid

    sh.io.SMEM_awaddr  := mem.m_axi.awaddr + "h120000000".U
    sh.io.SMEM_awburst := mem.m_axi.awburst
    sh.io.SMEM_awcache := mem.m_axi.awcache
    sh.io.SMEM_awid    := mem.m_axi.awid
    sh.io.SMEM_awlen   := mem.m_axi.awlen
    sh.io.SMEM_awlock  := mem.m_axi.awlock
    sh.io.SMEM_awprot  := mem.m_axi.awprot
    sh.io.SMEM_awqos   := 0.U // mem.m_axi.awqos
    mem.m_axi.awready  := sh.io.SMEM_awready
    // val SMEM_awegion = Input(UInt(4.W))
    sh.io.SMEM_awsize  := mem.m_axi.awsize
    sh.io.SMEM_awvalid := mem.m_axi.awvalid

    mem.m_axi.bid     := sh.io.SMEM_bid
    sh.io.SMEM_bready := mem.m_axi.bready
    mem.m_axi.bresp   := sh.io.SMEM_bresp
    mem.m_axi.bvalid  := sh.io.SMEM_bvalid

    mem.m_axi.rdata   := sh.io.SMEM_rdata
    mem.m_axi.rid     := sh.io.SMEM_rid
    mem.m_axi.rlast   := sh.io.SMEM_rlast
    sh.io.SMEM_rready := mem.m_axi.rready
    mem.m_axi.rresp   := sh.io.SMEM_rresp
    mem.m_axi.rvalid  := sh.io.SMEM_rvalid

    sh.io.SMEM_wdata  := mem.m_axi.wdata
    sh.io.SMEM_wlast  := mem.m_axi.wlast
    sh.io.SMEM_wstrb  := mem.m_axi.wstrb
    sh.io.SMEM_wvalid := mem.m_axi.wvalid
    mem.m_axi.wready  := sh.io.SMEM_wready

    lat := mem.stats.lat
  }
  /*
  // Tie-off the SMEM port and SLOT port

  // This should be connected to the...core..s
  // ----- SMEM -----
  sh.io.SMEM_araddr   := 0.U
  sh.io.SMEM_arburst  := 0.U
  sh.io.SMEM_arcache  := 0.U // Input(UInt(4.W))
  sh.io.SMEM_arid     := 0.U // Input(UInt(1.W))
  sh.io.SMEM_arlen    := 0.U // Input(UInt(8.W))
  sh.io.SMEM_arlock   := 0.U // Input(UInt(1.W))
  sh.io.SMEM_arprot   := 0.U // Input(UInt(3.W))
  sh.io.SMEM_arqos    := 0.U // Input(UInt(4.W))
  // sh.io.SMEM_arready  := 0.U // Output(UInt(1.W))
  // sh.io.SMEM_arregion := 0.U // Input(UInt(4.W))
  sh.io.SMEM_arsize   := 0.U // Input(UInt(3.W))
  sh.io.SMEM_arvalid  := 0.U // Input(UInt(1.W))
  sh.io.SMEM_awaddr   := 0.U // Input(UInt(64.W))
  sh.io.SMEM_awburst  := 0.U // Input(UInt(2.W))
  sh.io.SMEM_awcache  := 0.U // Input(UInt(4.W))
  sh.io.SMEM_awid     := 0.U // Input(UInt(1.W))
  sh.io.SMEM_awlen    := 0.U // Input(UInt(8.W))
  sh.io.SMEM_awlock   := 0.U // Input(UInt(1.W))
  sh.io.SMEM_awprot   := 0.U // Input(UInt(3.W))
  sh.io.SMEM_awqos    := 0.U // Input(UInt(4.W))
  // sh.io.SMEM_awready  := 0.U // Output(UInt(1.W))
  // sh.io.SMEM_awregion := 0.U // Input(UInt(4.W))
  sh.io.SMEM_awsize   := 0.U // Input(UInt(3.W))
  sh.io.SMEM_awvalid  := 0.U // Input(UInt(1.W))
  // sh.io.SMEM_bid      := 0.U // Output(UInt(1.W))
  sh.io.SMEM_bready   := 0.U // Input(UInt(1.W))
  // sh.io.SMEM_bresp    := 0.U // Output(UInt(2.W))
  // sh.io.SMEM_bvalid   := 0.U // Output(UInt(1.W))
  // sh.io.SMEM_rdata    := 0.U // Output(UInt(64.W))
  // sh.io.SMEM_rid      := 0.U // Output(UInt(1.W))
  // sh.io.SMEM_rlast    := 0.U // Output(UInt(1.W))
  sh.io.SMEM_rready   := 0.U // Input(UInt(1.W))
  // sh.io.SMEM_rresp    := 0.U // Output(UInt(2.W))
  // sh.io.SMEM_rvalid   := 0.U // Output(UInt(1.W))
  sh.io.SMEM_wdata    := 0.U // Input(UInt(64.W))
  sh.io.SMEM_wlast    := 0.U // Input(UInt(1.W))
  // sh.io.SMEM_wready   := 0.U // Output(UInt(1.W))
  sh.io.SMEM_wstrb    := 0.U // Input(UInt(8.W))
  sh.io.SMEM_wvalid   := 0.U // Input(UInt(1.W))

  // SLOT
  // sh.io.SLOT_araddr  := 0.U // Output(UInt(64.W))
  // sh.io.SLOT_arprot  := 0.U // Output(UInt(3.W))
  // sh.io.SLOT_arready := 0.U // Input(UInt(1.W))
  // sh.io.SLOT_arvalid := 0.U // Output(UInt(1.W))
  // sh.io.SLOT_awaddr  := 0.U // Output(UInt(64.W))
  // sh.io.SLOT_awprot  := 0.U // Output(UInt(3.W))
  // sh.io.SLOT_awready := 0.U // Input(UInt(1.W))
  // sh.io.SLOT_awvalid := 0.U // Output(UInt(1.W))
  // sh.io.SLOT_bready  := 0.U // Output(UInt(1.W))
  // sh.io.SLOT_bresp   := 0.U // Input(UInt(2.W))
  // sh.io.SLOT_bvalid  := 0.U // Input(UInt(1.W))
  // sh.io.SLOT_rdata   := 0.U // Input(UInt(64.W))
  // sh.io.SLOT_rready  := 0.U // Output(UInt(1.W))
  // sh.io.SLOT_rresp   := 0.U // Input(UInt(2.W))
  // sh.io.SLOT_rvalid  := 0.U // Input(UInt(1.W))
  // sh.io.SLOT_wdata   := 0.U // Output(UInt(64.W))
  // sh.io.SLOT_wready  := 0.U // Input(UInt(1.W))
  // sh.io.SLOT_wstrb   := 0.U // Output(UInt(8.W))
  // sh.io.SLOT_wvalid  := 0.U // Output(UInt(1.W))
   */

  sh.io.dimm1_refclk_clk_n := dimm1.refclk_clk_n
  sh.io.dimm1_refclk_clk_p := dimm1.refclk_clk_p
  C1_DDR4_0.act_n := sh.io.C1_DDR4_0_act_n
  C1_DDR4_0.adr   := sh.io.C1_DDR4_0_adr
  C1_DDR4_0.ba    := sh.io.C1_DDR4_0_ba
  C1_DDR4_0.bg    := sh.io.C1_DDR4_0_bg
  C1_DDR4_0.ck_c  := sh.io.C1_DDR4_0_ck_c
  C1_DDR4_0.ck_t  := sh.io.C1_DDR4_0_ck_t
  C1_DDR4_0.cke   := sh.io.C1_DDR4_0_cke
  C1_DDR4_0.cs_n  := sh.io.C1_DDR4_0_cs_n
  C1_DDR4_0.dm_n  <> sh.io.C1_DDR4_0_dm_n
  C1_DDR4_0.dq    <> sh.io.C1_DDR4_0_dq
  C1_DDR4_0.dqs_c <> sh.io.C1_DDR4_0_dqs_c
  C1_DDR4_0.dqs_t <> sh.io.C1_DDR4_0_dqs_t
  C1_DDR4_0.odt   := sh.io.C1_DDR4_0_odt
  C1_DDR4_0.reset_n := sh.io.C1_DDR4_0_reset_n
}
