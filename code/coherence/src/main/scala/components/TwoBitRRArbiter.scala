
package components

import chisel3._
import chisel3.util._
import params._
import chisel3.experimental._

/**
  * BlackBox module arbitrating write-back queue and request queue.
  */
class TwoBitRRArbiter extends BlackBox with HasBlackBoxInline {
  override def desiredName: String = "two_bit_round_robin_arbiter"
  val io = IO(new Bundle {
    val reset  = Input(Reset())
    val clock  = Input(Clock())
    val req    = Input(UInt(2.W))
    val ack    = Input(UInt(1.W))
    val grant  = Output(UInt(2.W))
  })
  setInline("two_bit_round_robin_arbiter.v",
    s"""
       |//two request rr arbiter
       |//author: dongjun_luo@hotmail.com
       |module two_bit_round_robin_arbiter (
       |	input        reset,
       |	input        clock,
       |	input  [1:0] req  ,
       |	input ack,
       |	output logic [1:0] grant
       |);
       |	logic       rotate_ptr ;
       |	logic [1:0] shift_req  ;
       |	logic [1:0] shift_grant;
       |	logic [1:0] grant_comb ;
       |//	logic [1:0] grant      ;
       |
       |// shift req to round robin the current priority
       |	always @ (*)
       |		begin
       |			case (rotate_ptr)
       |				1'b0 : shift_req[1:0] = req[1:0];
       |				1'b1 : shift_req[1:0] = {req[0],req[1]};
       |			endcase
       |		end
       |
       |// simple priority arbiter
       |	always @ (*)
       |		begin
       |			shift_grant[1:0] = 2'b0;
       |			if (shift_req[0])	shift_grant[0] = 1'b1;
       |			else if (shift_req[1])	shift_grant[1] = 1'b1;
       |		end
       |
       |// generate grant signal
       |	always @ (*)
       |		begin
       |			case (rotate_ptr)
       |				1'b0 : grant_comb[1:0] = shift_grant[1:0];
       |				1'b1 : grant_comb[1:0] = {shift_grant[0],shift_grant[1]};
       |			endcase
       |		end
       |
       |	always @ (posedge clock)
       |		begin
       |			if (reset)	grant[1:0] <= 2'b0;
       |			else		grant[1:0] <= grant_comb[1:0]; //& ~grant[3:0];
       |		end
       |
       |// update the rotate pointer
       |// rotate pointer will move to the one after the current granted
       |	always @ (posedge clock)
       |		begin
       |			if (reset)
       |				rotate_ptr <= 1'b0;
       |			else
       |				if(ack) begin
       |					case (1'b1) // synthesis parallel_case
       |						grant[0] : rotate_ptr <= 1'd1;
       |						grant[1] : rotate_ptr <= 1'd0;
       |					endcase
       |				end
       |		end
       |endmodule
       |
       |""".stripMargin)
}
