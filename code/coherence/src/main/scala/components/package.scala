
import chisel3._
import chisel3.util._
import chisel3.experimental._
import coherence.internal.AutoEnum
import params.SimpleCacheParams
import _root_.core.{MemoryRequestType, AMOOP}

package object components {

  trait HasModifiedWidth {
    def getWidthM: Int
  }

  class DRAMReq(addressWidth: Int, dataWidth: Int, reqTypeWidth: Int) extends Bundle with HasModifiedWidth {
    val address: UInt = UInt(addressWidth.W)
    val data: UInt = UInt(dataWidth.W)
    val length: UInt = UInt( log2Ceil(log2Ceil(dataWidth / 8)).W )
    val mem_type: UInt = UInt(reqTypeWidth.W)
    def getWidthM: Int = address.getWidth + data.getWidth + length.getWidth + mem_type.getWidth
    override def cloneType: this.type = new DRAMReq(addressWidth, dataWidth, reqTypeWidth).asInstanceOf[this.type]
    override def toPrintable: Printable = {
      p"DRAMReq(" +
      p"address=0x${Hexadecimal(address)}, " +
      p"address=0x${Hexadecimal(mem_type)})"
    }
  }

  class DRAMResp(dataWidth: Int, reqTypeWidth: Int) extends Bundle with HasModifiedWidth {
    val data: UInt = UInt(dataWidth.W)
    val length: UInt = UInt( log2Ceil(log2Ceil(dataWidth / 8)).W )
    val mem_type: UInt = UInt(reqTypeWidth.W)
    def getWidthM: Int = data.getWidth + length.getWidth + mem_type.getWidth
    override def cloneType: this.type = new DRAMResp(dataWidth, reqTypeWidth).asInstanceOf[this.type]
    override def toPrintable: Printable = {
      p"DRAMResp(" +
      p"address=0x${Hexadecimal(mem_type)})"
    }
  }
  class MemReqCommand(addressWidth: Int, reqTypeWidth: Int, n: Int) extends Bundle with HasModifiedWidth {
    val address: UInt = UInt(addressWidth.W)
    val req_type = UInt(reqTypeWidth.W)
    val requester_id = UInt(log2Ceil(n + 1).W)
    val req_wb = UInt(1.W)
    val dirty  = UInt(1.W)

    val criticality = UInt(3.W)
    val hasMatched = UInt(16.W)

    def getWidthM: Int = address.getWidth + req_type.getWidth +
      requester_id.getWidth + req_wb.getWidth + criticality.getWidth + hasMatched.getWidth

    override def cloneType: this.type = new MemReqCommand(addressWidth, reqTypeWidth, n).asInstanceOf[this.type]
    override def toPrintable: Printable = {
      p"MemReqCommand(" +
        p"address=0x${Hexadecimal(address)}, " +
        p"req_type=${Hexadecimal(req_type)}, " +
        p"id=${requester_id}, " +
        p"dirty=${dirty}, " +
        p"criticality=${criticality})"
    }

  }

  class MemReq(addressWidth: Int, dataWidth: Int, reqTypeWidth: Int, n: Int) extends Bundle with HasModifiedWidth {
    val address: UInt = UInt(addressWidth.W)
    val data: UInt = UInt(dataWidth.W)
    val req_type = UInt(reqTypeWidth.W)
    val requester_id = UInt(log2Ceil(n + 1).W)
    val req_wb = UInt(1.W)
    val dirty  = UInt(1.W)

    def getWidthM: Int = address.getWidth + data.getWidth + req_type.getWidth +
      requester_id.getWidth + req_wb.getWidth

    override def cloneType: this.type = new MemReq(addressWidth, dataWidth, reqTypeWidth, n).asInstanceOf[this.type]
    override def toPrintable: Printable = {
      p"MemReq(" +
        p"address=0x${Hexadecimal(address)}, " +
        p"req_type=${Hexadecimal(req_type)}, " +
        p"id=${requester_id}, " +
        p"dirty=${dirty})"
    }
  }
  object MemReq {
    def getCommand(req: MemReq, genMemReqCommand: MemReqCommand): MemReqCommand = {
      val command = Wire(genMemReqCommand)
      command.address := req.address
      command.req_type := req.req_type
      command.requester_id := req.requester_id
      command.req_wb := req.req_wb
      command.dirty := req.dirty
      command
    }
  }
  class MemRespCommand extends Bundle with HasModifiedWidth {
    val ack = UInt(1.W)
    val is_edata = UInt(1.W)
    val address = UInt(64.W)
    override def cloneType: this.type = new MemRespCommand().asInstanceOf[this.type]

    def getWidthM: Int = ack.getWidth + is_edata.getWidth + address.getWidth
    override def toPrintable: Printable = {
      p"MemResp(" +
        p"address=${Hexadecimal(address)}, " +
        p"ack=${ack}, " +
        p"is_edata=${is_edata})"
    }
  }
  object MemResp {
    def getCommand(resp: MemResp, genMemRespCommand: MemRespCommand): MemRespCommand = {
      val command = Wire(genMemRespCommand)
      command.ack := resp.ack
      command.is_edata := resp.is_edata
      command
    }
  }
  class MemResp(dataWidth: Int) extends Bundle with HasModifiedWidth {
    val data = UInt(dataWidth.W)
    val ack = UInt(1.W)
    val is_edata = UInt(1.W)
    val address = UInt(64.W)
    override def cloneType: this.type = new MemResp(dataWidth).asInstanceOf[this.type]

    def getWidthM: Int = data.getWidth + ack.getWidth + is_edata.getWidth + 64
    override def toPrintable: Printable = {
      p"MemResp(" +
        p"address=${address}" +
        p"ack=${ack}, " +
        p"is_edata=${is_edata}, " +
        p"data=${Hexadecimal(data)}"
    }
  }
  class SnoopReq(addressWidth: Int, reqTypeWidth: Int, n: Int) extends Bundle with HasModifiedWidth {
    val address = UInt(addressWidth.W)
    val req_type = UInt(reqTypeWidth.W)
    val requester_id = UInt(log2Ceil(n + 1).W)
    val criticality = UInt(3.W)
    override def cloneType: this.type = new SnoopReq(addressWidth, reqTypeWidth, n).asInstanceOf[this.type]

    def getWidthM: Int = address.getWidth + req_type.getWidth + requester_id.getWidth + criticality.getWidth
  }
  class SnoopResp(dataWidth: Int) extends Bundle {
    // val data = UInt(dataWidth.W) // currently we do not need to use that
    // whether a core has a cacheline
    val ack = UInt(1.W)
    val criticality = UInt(3.W)
    val hasMatched = UInt(1.W)
    override def cloneType: this.type = new SnoopResp(dataWidth).asInstanceOf[this.type]
    def getWidthM: Int = /*data.getWidth*/ + ack.getWidth + criticality.getWidth + hasMatched.getWidth
  }
  class CacheReq(val addrWidth: Int,
                 val dataWidth: Int /* this is related to the ISA, i.e. 32, 64 or 128 */
                ) extends Bundle with HasModifiedWidth {
    val address = UInt(addrWidth.W)
    val data = UInt(dataWidth.W)
    val length = UInt(2.W)
    val mem_type = MemoryRequestType()

    val is_amo = Bool()
    val amo_alu_op = AMOOP()
    val aq = UInt(1.W)
    val rl = UInt(1.W)

    val flush = Bool()
    val llcc_flush = Bool()

    val use_wstrb = Bool()
    val wstrb = UInt((dataWidth / 8).W)
    override def cloneType: this.type = new CacheReq(addrWidth, dataWidth).asInstanceOf[this.type]

    override def getWidthM: Int = address.getWidth + data.getWidth + length.getWidth + mem_type.getWidth + is_amo.getWidth
    +amo_alu_op.getWidth + aq.getWidth + rl.getWidth + flush.getWidth + llcc_flush.getWidth

    override def toPrintable: Printable = {
      p"CacheReq(" +
        p"address=0x${Hexadecimal(address)}, " +
        p"data=0x${Hexadecimal(data)}, " +
        p"mem_type=${Hexadecimal(mem_type.asUInt)}, " +
        p"len=${Hexadecimal(length)}, " +
        p"amo=${is_amo})"
    }
  }

  class CacheResp(val dataWidth: Int) extends Bundle {
    val address = UInt(dataWidth.W)
    val mem_type = MemoryRequestType()
    val length = UInt(2.W)
    val data = UInt(dataWidth.W)
    val latency = UInt(dataWidth.W)
    override def cloneType: this.type = new CacheResp(dataWidth).asInstanceOf[this.type]

    def getWidthM: Int = mem_type.getWidth + length.getWidth + data.getWidth + address.getWidth
    override def toPrintable: Printable = {
      p"CacheResp(" +
        p"address=0x${Hexadecimal(address)}, " +
        p"data=0x${Hexadecimal(data)})"
    }
  }

  class DebugCacheline(val CohS: AutoEnum, val cacheParams: SimpleCacheParams) extends Bundle with HasModifiedWidth {
    val tag = UInt(cacheParams.tagWidth.W)
    val state = UInt(CohS.getWidth.W)
    val valid = UInt(1.W)
    val dirty = UInt(1.W)
    val data = UInt(cacheParams.lineWidth.W)
    override def cloneType: this.type = new DebugCacheline(CohS, cacheParams).asInstanceOf[this.type]

    override def getWidthM: Int = tag.getWidth + state.getWidth + valid.getWidth + dirty.getWidth +
      data.getWidth
  }

  /* https://www.xilinx.com/support/documentation/ip_documentation/ug761_axi_reference_guide.pdf */
  class AXI4WriteAddress(val transactionIdWidth: Int,
                         val addrWidth: Int) extends Bundle {
    require(addrWidth >= 32)
    val AWID = UInt(transactionIdWidth.W)
    val AWADDR = UInt(addrWidth.W)
    val AWLEN = UInt(8.W)
    val AWSIZE = UInt(4.W)
    val AWBURST = UInt(2.W)
    val AWLOCK = UInt(1.W)
    val AWCACHE = UInt(4.W)
    val AWPROT = UInt(3.W)

    /* these are recommended values given in the documentation */
    def awcacheDefault: UInt = "b0011".U
    def awprotDefault: UInt = "b000".U
  }

  class AXI4WriteData(val dataWidth: Int) extends Bundle {
    require(dataWidth == 32 || dataWidth == 64 || dataWidth == 128 || dataWidth == 256)
    val WDATA = UInt(dataWidth.W)
    val WSTRB = UInt(log2Ceil(dataWidth).W)
    val WLAST = UInt(1.W)
  }

  class AXI4WriteResponse(val transactionIdWidth: Int) extends Bundle {
    val BID = UInt(transactionIdWidth.W)
    val BRESP = UInt(2.W)
  }

  class AXI4ReadAddress(val transactionIdWidth: Int,
                        val addrWidth: Int) extends Bundle {
    require(addrWidth >= 32)
    val ARID = UInt(transactionIdWidth.W)
    val ARADDR = UInt(addrWidth.W)
    val ARLEN = UInt(8.W)
    val ARSIZE = UInt(4.W)
    val ARBURST = UInt(2.W)
    val ARLOCK = UInt(1.W)
    val ARCACHE = UInt(4.W)
    val ARPROT = UInt(3.W)

    /* these are recommended values given in the documentation */
    def arcacheDefault: UInt = "b0011".U
    def arprotDefault: UInt = "b000".U
  }

  class AXI4ReadData(val transactionIdWidth: Int,
                     val dataWidth: Int) extends Bundle {
    require(dataWidth == 32 || dataWidth == 64 || dataWidth == 128 || dataWidth == 256)
    val RID = UInt(transactionIdWidth.W)
    val RDATA = UInt(dataWidth.W)
    val RRESP = UInt(2.W)
    val RLAST = UInt(1.W)
  }

  object AXI4 {
    implicit class AddMethodsToAXI4(target: AXI4) {
      def awfire(): Bool = target.awready && target.awvalid
      def wfire(): Bool = target.wready && target.wvalid
      def bfire(): Bool = target.bready && target.bvalid
      def arfire(): Bool = target.arready && target.arvalid
      def rfire(): Bool = target.rready && target.rvalid
    }
  }
  class AXI4(val transactionIdWidth: Int,
             val addrWidth: Int,
             val dataWidth: Int) extends Bundle {
    require(addrWidth >= 32)
    require(dataWidth == 32 || dataWidth == 64 || dataWidth == 128 || dataWidth == 256)
    val awid    = Output(UInt(transactionIdWidth.W))
    val awaddr  = Output(UInt(addrWidth.W))
    val awlen   = Output(UInt(8.W))
    val awsize  = Output(UInt(4.W))
    val awburst = Output(UInt(2.W))
    val awlock  = Output(UInt(1.W))
    val awcache = Output(UInt(4.W))
    val awprot  = Output(UInt(3.W))
    val awvalid = Output(Bool())
    val awready = Input(Bool())

    /* these are recommended values given in the documentation */
    def awcacheDefault: UInt = "b0011".U
    def awprotDefault: UInt = "b000".U

    val wdata  = Output(UInt(dataWidth.W))
    val wstrb  = Output(UInt((dataWidth/8).W))
    val wlast  = Output(Bool())
    val wvalid = Output(Bool())
    val wready = Input(Bool())

    val bid    = Input(UInt(transactionIdWidth.W))
    val bresp  = Input(UInt(2.W))
    val bvalid = Input(Bool())
    val bready = Output(Bool())

    val arid    = Output(UInt(transactionIdWidth.W))
    val araddr  = Output(UInt(addrWidth.W))
    val arlen   = Output(UInt(8.W))
    val arsize  = Output(UInt(4.W))
    val arburst = Output(UInt(2.W))
    val arlock  = Output(UInt(1.W))
    val arcache = Output(UInt(4.W))
    val arprot  = Output(UInt(3.W))
    val arvalid = Output(Bool())
    val arready = Input(Bool())

    /* these are recommended values given in the documentation */
    def arcacheDefault: UInt = "b0011".U
    def arprotDefault: UInt = "b000".U

    val rid    = Input(UInt(transactionIdWidth.W))
    val rdata  = Input(UInt(dataWidth.W))
    val rresp  = Input(UInt(2.W))
    val rlast  = Input(Bool())
    val rvalid = Input(Bool())
    val rready = Output(Bool())

    def defaultID: UInt = 0.U(transactionIdWidth.W)
  }

  object AXI4X {
    object Resp extends ChiselEnum {
      val OKAY, EOKAY, SLVERR, DECERR = Value
    }
    object BurstType extends ChiselEnum {
      val FIXED, INCR, WRAP, Reserved = Value
    }
    class AddressChannel(val addrWidth: Int,
                         val dataWidth: Int,
                         val transactionIdWidth: Int
                        ) extends Bundle {
      val id = UInt(transactionIdWidth.W)
      val addr = UInt(addrWidth.W)
      val len = UInt(8.W)
      val size = UInt(log2Ceil(dataWidth/8).W)
      val burst = BurstType()
      val lock = UInt(1.W)
      val cache = UInt(4.W)
      val prot = UInt(3.W)
      val region = UInt(4.W)
      val qos = UInt(4.W)
    }

    class ReadDataChannel(
                         val dataWidth: Int,
                         val transactionIdWidth: Int
                         ) extends Bundle {
      val id = UInt(transactionIdWidth.W)
      val data = UInt(dataWidth.W)
      val resp = Resp()
      val last = UInt(1.W)
    }
    class WriteDataChannel(
                           val dataWidth: Int,
                           val transactionIdWidth: Int
                         ) extends Bundle {
      val id = UInt(transactionIdWidth.W)
      val data = UInt(dataWidth.W)
      val strb = UInt((dataWidth / 8).W)
      val last = UInt(1.W)
    }

    class BChannel(val transactionIdWidth: Int) extends Bundle {
      val id = UInt(transactionIdWidth.W)
      val resp = Resp()
    }
  }
  class AXI4X(
               val addrWidth: Int,
               val dataWidth: Int,
               val transactionIdWidth: Int
             ) extends Bundle {
    val ar = Decoupled(new AXI4X.AddressChannel(addrWidth, dataWidth, transactionIdWidth))
    val r = Flipped(Decoupled(new AXI4X.ReadDataChannel(dataWidth, transactionIdWidth)))
    val aw = Decoupled(new AXI4X.AddressChannel(addrWidth, dataWidth, transactionIdWidth))
    val w = Decoupled(new AXI4X.WriteDataChannel(dataWidth, transactionIdWidth))
    val b = Flipped(Decoupled(new AXI4X.BChannel(transactionIdWidth)))
  }

  object AXI4Lite {
    implicit class AddMethodsToAXI4Lite(target: AXI4Lite) {
      def awfire(): Bool = target.awready && target.awvalid
      def wfire(): Bool = target.wready && target.wvalid
      def bfire(): Bool = target.bready && target.bvalid
      def arfire(): Bool = target.arready && target.arvalid
      def rfire(): Bool = target.rready && target.rvalid
    }
  }
  // A very very light weight AXI4 implementation, that could be hooked to the xilinx ip
  class AXI4Lite(val addrWidth: Int, val dataWidth: Int) extends Bundle {
    require(addrWidth >= 32)
    require(dataWidth == 32 || dataWidth == 64 || dataWidth == 128 || dataWidth == 256)
    val awaddr  = Output(UInt(addrWidth.W))
    val awprot  = Output(UInt(3.W))
    val awvalid = Output(Bool())
    val awready = Input(Bool())

    /* these are recommended values given in the documentation */
    def awcacheDefault: UInt = "b0011".U
    def awprotDefault: UInt = "b000".U

    val wdata  = Output(UInt(dataWidth.W))
    val wstrb  = Output(UInt((dataWidth/8).W))
    val wvalid = Output(Bool())
    val wready = Input(Bool())

    val bresp  = Input(UInt(2.W))
    val bvalid = Input(Bool())
    val bready = Output(Bool())

    val araddr  = Output(UInt(addrWidth.W))
    val arprot  = Output(UInt(3.W))
    val arvalid = Output(Bool())
    val arready = Input(Bool())

    /* these are recommended values given in the documentation */
    def arcacheDefault: UInt = "b0011".U
    def arprotDefault: UInt = "b000".U

    val rdata  = Input(UInt(dataWidth.W))
    val rresp  = Input(UInt(2.W))
    val rvalid = Input(Bool())
    val rready = Output(Bool())
  }

}
