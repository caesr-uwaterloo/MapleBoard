
package components

import chisel3._
import chisel3.experimental._

class ProcessorWrapper(val id: Int, val addrWidth: Int, val dataWidth: Int) extends BlackBox(
  Map(
    "COREID" -> id
  )
){

  override def desiredName: String = "ProcessorWrapper"
  val io = IO(new Bundle {
    val clock      = Input(Clock())
    val reset      = Input(UInt(1.W))
    val reset_pc_i = Input(UInt(dataWidth.W))
    //instruction memory interface
    // output cache_req_t              icachereq_data_o,
    /* flatten */
    val icachereq_data_o_address    = Output(UInt(addrWidth.W))
    val icachereq_data_o_data       = Output(UInt(addrWidth.W))
    val icachereq_data_o_length     = Output(UInt(2.W))
    val icachereq_data_o_mem_type   = Output(UInt(1.W))

    val icachereq_data_o_is_amo     = Output(UInt(1.W))
    val icachereq_data_o_amo_alu_op = Output(UInt(5.W))
    val icachereq_data_o_aq         = Output(UInt(1.W))
    val icachereq_data_o_rl         = Output(UInt(1.W))

    val icachereq_data_o_flush      = Output(UInt(1.W))
    val icachereq_data_o_llcc_flush = Output(UInt(1.W))
    /* flatten */
    val icachereq_valid_o           = Output(UInt(1.W))
    val icachereq_ready_i           = Input (UInt(1.W))

    /* flatten */
    val icacheresp_data_i_mem_type  = Input(UInt(1.W))
    val icacheresp_data_i_length    = Input(UInt(2.W))
    val icacheresp_data_i_data      = Input(UInt(dataWidth.W))
    /* flatten */

    val icacheresp_valid_i          = Input(UInt(1.W))
    val icacheresp_ready_o          = Output(UInt(1.W))

     /* flatten */
    val dcachereq_data_o_address    = Output(UInt(addrWidth.W))
    val dcachereq_data_o_data       = Output(UInt(addrWidth.W))
    val dcachereq_data_o_length     = Output(UInt(2.W))
    val dcachereq_data_o_mem_type   = Output(UInt(1.W))

    val dcachereq_data_o_is_amo     = Output(UInt(1.W))
    val dcachereq_data_o_amo_alu_op = Output(UInt(5.W))
    val dcachereq_data_o_aq         = Output(UInt(1.W))
    val dcachereq_data_o_rl         = Output(UInt(1.W))

    val dcachereq_data_o_flush      = Output(UInt(1.W))
    val dcachereq_data_o_llcc_flush = Output(UInt(1.W))
    /* flatten */
    val dcachereq_valid_o           = Output(UInt(1.W))
    val dcachereq_ready_i           = Input (UInt(1.W))

    /* flatten */
    val dcacheresp_data_i_mem_type  = Input(UInt(1.W))
    val dcacheresp_data_i_length    = Input(UInt(2.W))
    val dcacheresp_data_i_data      = Input(UInt(dataWidth.W))
    /* flatten */

    val dcacheresp_valid_i          = Input(UInt(1.W))
    val dcacheresp_ready_o          = Output(UInt(1.W))


    val timer_irq_i = Input(UInt(1.W))
    // these interfaces are only for debugging on board
    // all data are from write-back stage
    val reg_wr_en_o = Output(UInt(1.W))
    val reg_addr_o  = Output(UInt(5.W))
    val reg_data_o  = Output(UInt(dataWidth.W))
    val ir_o        = Output(UInt(dataWidth.W))
    val pc_o        = Output(UInt(addrWidth.W))

  })
}
