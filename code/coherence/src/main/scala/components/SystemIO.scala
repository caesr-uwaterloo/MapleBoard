
package components

import chisel3._
import chisel3.util._
import params.MemorySystemParams

class GPIOControl(memorySystemParams: MemorySystemParams) extends Bundle {
  val ack: UInt = UInt((memorySystemParams.masterCount / 2).W)
  val core_id: UInt = UInt(log2Ceil(memorySystemParams.masterCount).W)
  val reg_addr: UInt = UInt(5.W)

  override def cloneType: this.type = new GPIOControl(memorySystemParams).asInstanceOf[this.type]
}
class SystemIO(memorySystemParams: MemorySystemParams) extends Bundle {
  val gpio_syscall_reg_data_o = Output(UInt(memorySystemParams.dataWidth.W))
  val gpio_syscall_en_o = Output(UInt((memorySystemParams.masterCount / 2).W))
  val gpio_syscall_ctrl_i = Input( UInt(new GPIOControl(memorySystemParams).getWidth.W) )

  val reg_wr_en_i = Input(UInt((memorySystemParams.masterCount / 2 * 1).W))
  val reg_addr_i  = Input(UInt((memorySystemParams.masterCount / 2 * 5).W))
  val reg_data_i  = Input(UInt((memorySystemParams.masterCount / 2 * memorySystemParams.dataWidth).W))

  val ir_o        = Input(UInt((memorySystemParams.masterCount / 2 * memorySystemParams.dataWidth).W))
  override def cloneType: this.type = new SystemIO(memorySystemParams).asInstanceOf[this.type]
}

// SystemIO module for interfacing with the host through GPIO
class GPIOInterface(memorySystemParams: MemorySystemParams) extends Module{
  val masterCount = memorySystemParams.masterCount
  val io = IO(new SystemIO(memorySystemParams))
  val ctrl_i = io.gpio_syscall_ctrl_i.asTypeOf(new GPIOControl(memorySystemParams))
  val shadow_registers = for {i <- 0 until memorySystemParams.masterCount / 2} yield {
    val reg_depth = 32
    val shadow_regfile = Module(new BRAMVerilog(reg_depth, memorySystemParams.dataWidth))
    val dataWidth = memorySystemParams.dataWidth
    shadow_regfile.io.clock := clock
    shadow_regfile.io.reset := reset
    shadow_regfile.io.raddr := ctrl_i.reg_addr
    shadow_regfile.io.waddr := io.reg_addr_i((i + 1) * 5-1, i * 5)
    shadow_regfile.io.we := io.reg_wr_en_i(i) && (shadow_regfile.io.waddr =/= 0.U)
    shadow_regfile.io.wdata := io.reg_data_i((i + 1) * dataWidth - 1, i * dataWidth)
    shadow_regfile
  }
  val rdata = VecInit.tabulate(memorySystemParams.masterCount / 2)(i => { shadow_registers(i).io.rdata })
  val syscall_reg_data_o = RegInit(0.U(memorySystemParams.dataWidth.W))
  val ir = io.ir_o.asTypeOf(Vec(masterCount / 2, UInt(memorySystemParams.dataWidth.W)))
  val syscall_en = RegInit(VecInit.tabulate(masterCount / 2)(_ => 0.U(1.W)))

  for { i <- 0 until masterCount / 2} {
    // OP_SYSTEM and ECALL
    when(ir(i)(6,0) === "b1110011".U && ir(i)(31, 20) === 0.U) {
      syscall_en(i) := 1.U
    }.elsewhen(ctrl_i.ack(i) === 1.U) {
      syscall_en(i) := 0.U
    }

  }

  syscall_reg_data_o := rdata(ctrl_i.core_id)
  io.gpio_syscall_en_o := syscall_en.asUInt
  io.gpio_syscall_reg_data_o := rdata(ctrl_i.core_id)
}
