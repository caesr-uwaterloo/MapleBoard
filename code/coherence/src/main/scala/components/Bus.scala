// See README.md for license details.

package components

import chisel3._
import chisel3.util._
import arbiters._
import chisel3.experimental.ChiselEnum
import chisel3.util.experimental.BoringUtils



object BusState {
  val bus_state_number = 4
  val sBUS_RESET :: sBUS_IDLE :: sBUS_PENDINGREQUEST :: sBUS_RESP_ACK :: Nil = Enum(bus_state_number)
}

// We roll out a new implmentation...
/*
abstract class SnoopyBus
                (val masterCount : Int,
                 private val genControllerReq : MemReqCommand,
                 private val genControllerResp : MemRespCommand,
                 private val genSnoopReq : SnoopReq,
                 private val genSnoopResp : SnoopResp) extends Module{
  val io = IO(new Bundle {
    val controller = new Bundle {
      val request_channel = new ArbiterIO(genControllerReq, masterCount)
      val response_channel = new Bundle {
        val in = Flipped(Vec(masterCount, Decoupled(genControllerResp)))
        val out = Vec(masterCount, Decoupled(genControllerResp))
      }
    }
    val snoop = new Bundle {
      val request_channel = Vec(masterCount, Decoupled(genSnoopReq))
      val response_channel = Flipped(Vec(masterCount, Decoupled(genSnoopResp)))
    }
    //val master_state = Output(UInt(4.W))
    //val snoop_state = Output(UInt(4.W))
    //val slave_state = Output(UInt(4.W))
  })
  val last_tick_owner = RegNext(arbiter.io.chosen, (masterCount - 1).U)
  val switching = last_tick_owner =/= arbiter.io.chosen
  def reqToSnoop(i: Int) : Unit

  protected def arbiter : LockingArbiterLike[MemReqCommand] with HasChoice
  io.controller.request_channel.out <> arbiter.io.out
  arbiter.io.in <> io.controller.request_channel.in
  /*
  for { i <- 0 until masterCount} {
    io.controller.response_channel.out(i).bits := io.controller.response_channel.in(i).bits
    io.controller.response_channel.out(i).valid := io.controller.response_channel.in(i).valid &&
      arbiter.io.chosen === i.U && !switching
    io.controller.response_channel.in(i).ready := io.controller.response_channel.out(i).ready &&
      arbiter.io.chosen === i.U && !switching
  }
  */
  io.controller.request_channel.chosen := arbiter.io.chosen


  val master_state = RegInit(BusState.sBUS_RESET)
  val snoop_state = RegInit(VecInit(Seq.fill(masterCount)(BusState.sBUS_RESET)))
  val snoop_resp_reg = Reg(Vec(masterCount, genSnoopResp))
  val slave_state = RegInit(BusState.sBUS_RESET)
  val s_resp_data_reg = Reg(genControllerResp)
  val slot_owner_req_reg = Reg(genControllerReq)

  //io.master_state := master_state
  //io.snoop_state := snoop_state
  //io.slave_state := slave_state

  //////////////////////////////////////////////////////////////////
  // slot_owner state machine
  //////////////////////////////////////////////////////////////////
  when(switching) { // only accepts request in the beginning of a slot

    // printf("===== slot separator =====\n")
    master_state := BusState.sBUS_IDLE
    when(io.controller.request_channel.in(arbiter.io.chosen).fire()) {
      master_state := BusState.sBUS_PENDINGREQUEST
      slot_owner_req_reg := io.controller.request_channel.in(arbiter.io.chosen).bits
    }
  }.otherwise {
    switch(master_state) {
      is(BusState.sBUS_RESET) { master_state := BusState.sBUS_IDLE }
      is(BusState.sBUS_IDLE) {
        when(io.controller.response_channel.out(arbiter.io.chosen).fire()) {
          master_state := BusState.sBUS_RESP_ACK
        }
      }
      is(BusState.sBUS_PENDINGREQUEST) {
        when(io.controller.response_channel.out(arbiter.io.chosen).fire()) {
          master_state := BusState.sBUS_RESP_ACK
        }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////
  // Sets the ready to the slot owner master.
  // Uses round rubin arbitration
  /////////////////////////////////////////////////////////////////////
  for { i <- 0 until masterCount } {
    io.controller.request_channel.in(i).ready := false.B
    io.controller.response_channel.out(i).bits := (0.U).asTypeOf(genControllerResp)
    io.controller.response_channel.out(i).valid := false.B
    when( (arbiter.io.chosen === i.U) && switching && !io.controller.response_channel.in(i).valid && io.controller.request_channel.out.ready) {
      io.controller.request_channel.in(i).ready := true.B
    }.elsewhen((arbiter.io.chosen === i.U) && (slave_state === BusState.sBUS_RESP_ACK)
      && (master_state =/= BusState.sBUS_RESP_ACK)){
      io.controller.response_channel.out(i).valid := true.B
      io.controller.response_channel.out(i).bits := s_resp_data_reg
    }
  }


  //////////////////////////////////////////////////////////////////
  // snoop master_state machine
  //////////////////////////////////////////////////////////////////
  for { i <- 0 until masterCount } {
    when(master_state === BusState.sBUS_IDLE || switching) {
      snoop_state(i) := BusState.sBUS_IDLE
    }.otherwise {
      switch(snoop_state(i)) {
        is(BusState.sBUS_RESET) {
          snoop_state(i) := BusState.sBUS_IDLE
        }
        is(BusState.sBUS_IDLE) {
          when(io.snoop.request_channel(i).fire()) {
            snoop_state(i) := BusState.sBUS_PENDINGREQUEST
          }
        }
        is(BusState.sBUS_PENDINGREQUEST) {
          // this state is strange, maybe one of them is unnecessary
          when(io.snoop.request_channel(i).fire()) {
            snoop_state(i) := BusState.sBUS_RESP_ACK
          }
        }
        is(BusState.sBUS_RESP_ACK) {
          snoop_state(i) := BusState.sBUS_PENDINGREQUEST
        }
      }
    }
  }
  //asserting snoop request and response control signal
  for { i <- 0 until masterCount} {
    io.snoop.response_channel(i).ready := false.B
    io.snoop.request_channel(i).bits := 0.U.asTypeOf(genSnoopReq)
    io.snoop.request_channel(i).valid := false.B
    when(snoop_state(i) === BusState.sBUS_IDLE && master_state === BusState.sBUS_PENDINGREQUEST) {
      reqToSnoop(i)
      io.snoop.request_channel(i).valid := true.B
    }.elsewhen(arbiter.io.chosen =/= i.U && snoop_state(i) === BusState.sBUS_PENDINGREQUEST) {
      io.snoop.response_channel(i).ready := true.B
    }.otherwise {
      io.snoop.response_channel(i).ready := true.B
    }
  }

  //////////////////////////////////////////////////////////////////
  // broadcast request to the slave port (memory)
  //////////////////////////////////////////////////////////////////
  when(switching) {
    when(io.controller.response_channel.in(arbiter.io.chosen).valid) {
      slave_state := BusState.sBUS_RESP_ACK
      s_resp_data_reg := io.controller.response_channel.in(arbiter.io.chosen).bits
    }.otherwise {
      slave_state := BusState.sBUS_IDLE
    }
  }.otherwise {
    switch(slave_state) {
      is(BusState.sBUS_RESET) { slave_state := BusState.sBUS_IDLE }
      is(BusState.sBUS_IDLE) {
        when(io.controller.request_channel.out.fire()) {
          slave_state := BusState.sBUS_PENDINGREQUEST
        }.elsewhen(io.controller.response_channel.in(arbiter.io.chosen).valid) {
          // NOTE: this branch is strange, but DO NOT touch as it may break the tests
          slave_state := BusState.sBUS_RESP_ACK
          s_resp_data_reg := io.controller.response_channel.in(arbiter.io.chosen).bits
        }
      }
      is(BusState.sBUS_PENDINGREQUEST) {
        when(io.controller.response_channel.in(arbiter.io.chosen).valid) {
          slave_state := BusState.sBUS_RESP_ACK
          s_resp_data_reg := io.controller.response_channel.in(arbiter.io.chosen).bits
        }
      }
    }
  }

  when(slave_state === BusState.sBUS_IDLE && master_state === BusState.sBUS_PENDINGREQUEST) {
    io.controller.request_channel.out.bits := slot_owner_req_reg
    io.controller.request_channel.out.valid := true.B
  }.otherwise {
    io.controller.request_channel.out.bits := 0.U.asTypeOf(genControllerReq)
    io.controller.request_channel.out.valid := false.B
  }

  protected def getReqWB(i: Int) = 0.U
  for { i <- 0 until masterCount } {
    // One proposal is that we dictate that a request must have a type indicating whether it is a write back
    io.controller.response_channel.in(i).ready := (arbiter.io.chosen === i.U) &&
      io.controller.response_channel.in(i).valid && !getReqWB(i)
  }

  val reqValid = Cat(for { i <- 0 until masterCount } yield {
    io.controller.request_channel.in(i).valid
  })
  val reqReady = Cat(for { i <- 0 until masterCount } yield {
    io.controller.request_channel.in(i).ready
  })

  // printf(p"[BUS] reqValid: ${Binary(reqValid.asUInt)} reqReady: ${Binary(reqReady.asUInt)} owner: ${arbiter.io.chosen}\n")

  for{i <- 0 until masterCount} {
    when(io.controller.request_channel.in(i).fire()) {
      printf(p"[BUS] ${io.controller.request_channel.in(i).bits}\n")
    }.elsewhen(io.controller.request_channel.in(i).valid) {
      // printf(p"[BUS] pending request from cc $i : ${io.controller.request_channel.in(i).bits}\n")
    }
  }
  when(io.controller.request_channel.out.fire()) {
    printf(p"[BUS] ${io.controller.request_channel.out.bits}\n")
  }
  for { i <- 0 until masterCount } {
    val regVld = RegInit(false.B)
    when(io.controller.response_channel.out(i).valid) {
      when(regVld === false.B) {
        printf(p"[BUS] ${i.U} has a pending response\n")
      }
      regVld := true.B
      when(io.controller.response_channel.out(i).ready) {
        printf(p"[BUS] And it received!!\n")
        regVld := false.B
      }
    }.otherwise {
      regVld := false.B
    }
  }

  // Bus State Machine
}
 */
object SnoopyBusStates extends ChiselEnum {
  val idle, broadcastRequest, sendLLC = Value
}
abstract class SnoopyBus
(val masterCount : Int,
 private val genControllerReq : MemReqCommand,
 private val genControllerResp : MemRespCommand,
 private val genSnoopReq : SnoopReq,
 private val genSnoopResp : SnoopResp,
 private val outOfSlotResponse: Boolean,
 private val busConf: DataBusConf,
 private val translateGetS: Boolean,
 val atomicBus: Boolean = false,
 val conventional: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val controller = new Bundle {
      val request_channel = new ArbiterIO(genControllerReq, masterCount)
      val response_channel = new Bundle {
        val in = Flipped(Vec(masterCount, Decoupled(genControllerResp)))
        val out = Vec(masterCount, Decoupled(genControllerResp))
      }
    }
    val snoop = new Bundle {
      val request_channel = Vec(masterCount, Decoupled(genSnoopReq))
      val response_channel = Flipped(Vec(masterCount, Decoupled(genSnoopResp)))
    }
    //val master_state = Output(UInt(4.W))
    //val snoop_state = Output(UInt(4.W))
    //val slave_state = Output(UInt(4.W))
    val slotOwner = Output(UIntHolding(masterCount))
    val slotStart = Output(Bool())
  })
  val last_tick_owner = RegNext(arbiter.io.chosen, (masterCount - 1).U)

  protected def switchingSame = false.B

  val switchingConventional = WireInit(false.B)
  val switching = if (!atomicBus) {
    if (conventional) {
      BoringUtils.addSink(switchingConventional, "switchingConventional", disableDedup = true)
      switchingConventional
    } else {
      last_tick_owner =/= arbiter.io.chosen
    }
  } else {
    switchingSame
  }
  // allowing the cache to broadcast the request in one period of time
  val ticker = RegInit(0.U(16.W))
  val pipeStages = 4
  val requestIssued = RegInit(false.B)
  when(arbiter.io.out.fire()) {
    requestIssued := true.B
  }
  when(switching) {
    requestIssued := false.B
    ticker := 0.U
  }.otherwise {
    ticker := ticker + 1.U
  }
  val state = RegInit(SnoopyBusStates.idle)

  def reqToSnoop(i: Int): Unit

  io.slotStart := switching
  io.slotOwner := last_tick_owner

  protected def arbiter: LockingArbiterLike[MemReqCommand] with HasChoice

  io.controller.request_channel.out <> arbiter.io.out
  arbiter.io.in <> io.controller.request_channel.in

  io.controller.request_channel.chosen := arbiter.io.chosen
  // only enables the request at the start of the slot
  val hasResponse = WireInit(VecInit(Seq.fill(masterCount) { false.B }))
  val responseFired = RegInit(VecInit(Seq.fill(masterCount) { false.B }))
  if(conventional) {
    for { i <- 0 until masterCount } {
      hasResponse(i) := io.controller.response_channel.in(i).valid
      when(io.controller.response_channel.in(i).valid) {
        responseFired(i) := true.B
      }.elsewhen(switchingConventional) {
        responseFired(i) := false.B
      }
    }
  }
  val hasAnyResponse = hasResponse.asUInt().orR()
  val anyResponseFired = responseFired.asUInt.orR

  for { i <- 0 until masterCount }  {

    /*
    val responseFiredOrAssert = WireInit(false.B)
    if(conventional) {
      responseFiredOrAssert := responseFired  || io.controller.response_channel.in(i).valid
    } else {
      responseFiredOrAssert := false.B
    }
     */
    // the has response stops request when response is valid for conventional response case
    io.controller.request_channel.in(i).ready := (switching || (!requestIssued && ticker < pipeStages.U)) && arbiter.io.in(i).ready && !anyResponseFired && !hasAnyResponse
    arbiter.io.in(i).valid := (switching || (!requestIssued && ticker < pipeStages.U)) && io.controller.request_channel.in(i).valid && !anyResponseFired && !hasAnyResponse
    if(conventional) {
      BoringUtils.addSource(io.controller.request_channel.in(i).valid, s"RequestValid${i}", disableDedup = true)
    }
  }
  val requestQueue = Module(new Queue(genControllerReq, entries=1, pipe=true))
  // used for assertive ... the response should not slide to the next slot

  val slot_owner_req_reg = WireInit(requestQueue.io.deq.bits)
  val hasMatched = Reg(Vec(masterCount, Bool()))

  requestQueue.io.enq <> arbiter.io.out

  val snoopSent = RegInit(VecInit(Seq.fill(masterCount) { false.B }))
  val snoopAck = RegInit(VecInit(Seq.fill(masterCount) { false.B }))

  switch(state) {
    is(SnoopyBusStates.idle) {
      when(requestQueue.io.deq.valid) {
        state := SnoopyBusStates.broadcastRequest
      }
    }
    is(SnoopyBusStates.broadcastRequest) {
      when(snoopSent.reduceLeft(_ && _) && snoopAck.reduceLeft(_ && _)) {
        state := SnoopyBusStates.sendLLC
        for { i <- 0 until masterCount } {
          snoopSent(i) := false.B
          snoopAck(i) := false.B
        }
      }
    }
    is(SnoopyBusStates.sendLLC) {
      assert(io.controller.request_channel.out.ready, "The memory should be able to receive a request")
      when(io.controller.request_channel.out.fire()) {
        state := SnoopyBusStates.idle
      }
    }
  }

  for { i <- 0 until masterCount } {
    reqToSnoop(i)
    io.snoop.request_channel(i).valid := state === SnoopyBusStates.broadcastRequest && !snoopSent(i)
    io.snoop.response_channel(i).ready := state === SnoopyBusStates.broadcastRequest && !snoopAck(i)
    when(io.snoop.request_channel(i).fire()) {
      snoopSent(i) := true.B
      assert(state === SnoopyBusStates.broadcastRequest, "Should only snoop in the broadcastSnoop")
    }
    when(io.snoop.response_channel(i).fire()) {
      hasMatched(i) := io.snoop.response_channel(i).bits.hasMatched
      snoopAck(i) := true.B
      assert(state === SnoopyBusStates.broadcastRequest, "Should only snoop in the broadcastSnoop")
    }
  }

  // drive request to the shared memory controller
  io.controller.request_channel.out.bits := requestQueue.io.deq.bits
  io.controller.request_channel.out.bits.hasMatched := hasMatched.asUInt
  // Only For PMESI
  if(translateGetS) {
    when(requestQueue.io.deq.bits.req_type === RequestType.GETS.U && PopCount(hasMatched) === 1.U ) {
      io.controller.request_channel.out.bits.req_type := (RequestType.PUTS + 1).U(5.W)
    }
  }
  when(state === SnoopyBusStates.sendLLC) {
    io.controller.request_channel.out.valid := requestQueue.io.deq.valid
    requestQueue.io.deq.ready := io.controller.request_channel.out.ready
  }.otherwise {
    io.controller.request_channel.out.valid := false.B
    requestQueue.io.deq.ready := false.B
  }

  // Then about the response..
  if(outOfSlotResponse) {
    // asserts
    for { i <- 0 until masterCount } {
      val responseToNextSlotCounter = RegInit(0.U(64.W))
      val runCounter = RegInit(false.B)
      val hasResponse = io.controller.response_channel.out(i).fire()
      when(hasResponse) {
        runCounter := true.B
      }.elsewhen(switching) {
        runCounter := false.B
        when(responseToNextSlotCounter < 16.U && hasResponse) {
          assert(false.B, "Too few slack left for response %d", i.U)
        }
      }
      when(runCounter) {
        responseToNextSlotCounter := responseToNextSlotCounter + 1.U
      }.otherwise {
        responseToNextSlotCounter := 0.U
      }
      io.controller.response_channel.out(i) <> io.controller.response_channel.in(i)
    }
  } else {
    val responseToNextSlotCounter = RegInit(0.U(64.W))
    val runCounter = RegInit(false.B)
    val hasResponse = io.controller.response_channel.out(arbiter.io.chosen).fire()
    when(hasResponse) {
      runCounter := true.B
    }.elsewhen(switching) {
      runCounter := false.B
      when(responseToNextSlotCounter < 16.U && hasResponse) {
        assert(false.B, "Too few slack left for response %d", arbiter.io.chosen)
      }
    }
    when(runCounter) {
      responseToNextSlotCounter := responseToNextSlotCounter + 1.U
    }.otherwise {
      responseToNextSlotCounter := 0.U
    }
    val is_busy = WireInit(false.B)
    if(conventional) {
      BoringUtils.addSink(is_busy, "FCFSArbiterBusy", disableDedup = true)
    }
    dontTouch(is_busy)
    for { i <- 0 until masterCount } {
      io.controller.response_channel.out(i).bits := io.controller.response_channel.in(i).bits
      io.controller.response_channel.out(i).valid := false.B
      io.controller.response_channel.in(i).ready := false.B
      busConf match {
        case SharedEverything => {
          when (i.U === arbiter.io.chosen) {
            io.controller.response_channel.out(i).valid := io.controller.response_channel.in(i).valid // && !is_busy
            io.controller.response_channel.in(i).ready := io.controller.response_channel.out(i).ready // && !is_busy
          }
        }
        case DedicatedDataBusTwoWay | DedicatedDataBusOneWay => {
          // always assert
          io.controller.response_channel.out(i).valid := io.controller.response_channel.in(i).valid
          io.controller.response_channel.in(i).ready := io.controller.response_channel.out(i).ready
        }
      }
    }
  }

  protected def getReqWB(i: Int) = 0.U

  /*
  val master_state = RegInit(BusState.sBUS_RESET)
  val snoop_state = RegInit(VecInit(Seq.fill(masterCount)(BusState.sBUS_RESET)))
  val snoop_resp_reg = Reg(Vec(masterCount, genSnoopResp))
  val slave_state = RegInit(BusState.sBUS_RESET)
  val s_resp_data_reg = Reg(genControllerResp)
  val slot_owner_req_reg = Reg(genControllerReq)

  //io.master_state := master_state
  //io.snoop_state := snoop_state
  //io.slave_state := slave_state

  //////////////////////////////////////////////////////////////////
  // slot_owner state machine
  //////////////////////////////////////////////////////////////////
  when(switching) { // only accepts request in the beginning of a slot

    // printf("===== slot separator =====\n")
    master_state := BusState.sBUS_IDLE
    when(io.controller.request_channel.in(arbiter.io.chosen).fire()) {
      master_state := BusState.sBUS_PENDINGREQUEST
      slot_owner_req_reg := io.controller.request_channel.in(arbiter.io.chosen).bits
    }
  }.otherwise {
    switch(master_state) {
      is(BusState.sBUS_RESET) { master_state := BusState.sBUS_IDLE }
      is(BusState.sBUS_IDLE) {
        when(io.controller.response_channel.out(arbiter.io.chosen).fire()) {
          master_state := BusState.sBUS_RESP_ACK
        }
      }
      is(BusState.sBUS_PENDINGREQUEST) {
        when(io.controller.response_channel.out(arbiter.io.chosen).fire()) {
          master_state := BusState.sBUS_RESP_ACK
        }
      }
    }
  }

  /////////////////////////////////////////////////////////////////////
  // Sets the ready to the slot owner master.
  // Uses round rubin arbitration
  /////////////////////////////////////////////////////////////////////
  for { i <- 0 until masterCount } {
    io.controller.request_channel.in(i).ready := false.B
    io.controller.response_channel.out(i).bits := (0.U).asTypeOf(genControllerResp)
    io.controller.response_channel.out(i).valid := false.B
    when( (arbiter.io.chosen === i.U) && switching && !io.controller.response_channel.in(i).valid && io.controller.request_channel.out.ready) {
      io.controller.request_channel.in(i).ready := true.B
    }.elsewhen((arbiter.io.chosen === i.U) && (slave_state === BusState.sBUS_RESP_ACK)
      && (master_state =/= BusState.sBUS_RESP_ACK)){
      io.controller.response_channel.out(i).valid := true.B
      io.controller.response_channel.out(i).bits := s_resp_data_reg
    }
  }


  //////////////////////////////////////////////////////////////////
  // snoop master_state machine
  //////////////////////////////////////////////////////////////////
  for { i <- 0 until masterCount } {
    when(master_state === BusState.sBUS_IDLE || switching) {
      snoop_state(i) := BusState.sBUS_IDLE
    }.otherwise {
      switch(snoop_state(i)) {
        is(BusState.sBUS_RESET) {
          snoop_state(i) := BusState.sBUS_IDLE
        }
        is(BusState.sBUS_IDLE) {
          when(io.snoop.request_channel(i).fire()) {
            snoop_state(i) := BusState.sBUS_PENDINGREQUEST
          }
        }
        is(BusState.sBUS_PENDINGREQUEST) {
          // this state is strange, maybe one of them is unnecessary
          when(io.snoop.request_channel(i).fire()) {
            snoop_state(i) := BusState.sBUS_RESP_ACK
          }
        }
        is(BusState.sBUS_RESP_ACK) {
          snoop_state(i) := BusState.sBUS_PENDINGREQUEST
        }
      }
    }
  }
  //asserting snoop request and response control signal
  for { i <- 0 until masterCount} {
    io.snoop.response_channel(i).ready := false.B
    io.snoop.request_channel(i).bits := 0.U.asTypeOf(genSnoopReq)
    io.snoop.request_channel(i).valid := false.B
    when(snoop_state(i) === BusState.sBUS_IDLE && master_state === BusState.sBUS_PENDINGREQUEST) {
      reqToSnoop(i)
      io.snoop.request_channel(i).valid := true.B
    }.elsewhen(arbiter.io.chosen =/= i.U && snoop_state(i) === BusState.sBUS_PENDINGREQUEST) {
      io.snoop.response_channel(i).ready := true.B
    }.otherwise {
      io.snoop.response_channel(i).ready := true.B
    }
  }

  //////////////////////////////////////////////////////////////////
  // broadcast request to the slave port (memory)
  //////////////////////////////////////////////////////////////////
  when(switching) {
    when(io.controller.response_channel.in(arbiter.io.chosen).valid) {
      slave_state := BusState.sBUS_RESP_ACK
      s_resp_data_reg := io.controller.response_channel.in(arbiter.io.chosen).bits
    }.otherwise {
      slave_state := BusState.sBUS_IDLE
    }
  }.otherwise {
    switch(slave_state) {
      is(BusState.sBUS_RESET) { slave_state := BusState.sBUS_IDLE }
      is(BusState.sBUS_IDLE) {
        when(io.controller.request_channel.out.fire()) {
          slave_state := BusState.sBUS_PENDINGREQUEST
        }.elsewhen(io.controller.response_channel.in(arbiter.io.chosen).valid) {
          // NOTE: this branch is strange, but DO NOT touch as it may break the tests
          slave_state := BusState.sBUS_RESP_ACK
          s_resp_data_reg := io.controller.response_channel.in(arbiter.io.chosen).bits
        }
      }
      is(BusState.sBUS_PENDINGREQUEST) {
        when(io.controller.response_channel.in(arbiter.io.chosen).valid) {
          slave_state := BusState.sBUS_RESP_ACK
          s_resp_data_reg := io.controller.response_channel.in(arbiter.io.chosen).bits
        }
      }
    }
  }

  when(slave_state === BusState.sBUS_IDLE && master_state === BusState.sBUS_PENDINGREQUEST) {
    io.controller.request_channel.out.bits := slot_owner_req_reg
    io.controller.request_channel.out.valid := true.B
  }.otherwise {
    io.controller.request_channel.out.bits := 0.U.asTypeOf(genControllerReq)
    io.controller.request_channel.out.valid := false.B
  }

  protected def getReqWB(i: Int) = 0.U
  for { i <- 0 until masterCount } {
    // One proposal is that we dictate that a request must have a type indicating whether it is a write back
    io.controller.response_channel.in(i).ready := (arbiter.io.chosen === i.U) &&
      io.controller.response_channel.in(i).valid && !getReqWB(i)
  }

  val reqValid = Cat(for { i <- 0 until masterCount } yield {
    io.controller.request_channel.in(i).valid
  })
  val reqReady = Cat(for { i <- 0 until masterCount } yield {
    io.controller.request_channel.in(i).ready
  })

  // printf(p"[BUS] reqValid: ${Binary(reqValid.asUInt)} reqReady: ${Binary(reqReady.asUInt)} owner: ${arbiter.io.chosen}\n")

  for{i <- 0 until masterCount} {
    when(io.controller.request_channel.in(i).fire()) {
      printf(p"[BUS] ${io.controller.request_channel.in(i).bits}\n")
    }.elsewhen(io.controller.request_channel.in(i).valid) {
      // printf(p"[BUS] pending request from cc $i : ${io.controller.request_channel.in(i).bits}\n")
    }
  }
  when(io.controller.request_channel.out.fire()) {
    printf(p"[BUS] ${io.controller.request_channel.out.bits}\n")
  }
  for { i <- 0 until masterCount } {
    val regVld = RegInit(false.B)
    when(io.controller.response_channel.out(i).valid) {
      when(regVld === false.B) {
        printf(p"[BUS] ${i.U} has a pending response\n")
      }
      regVld := true.B
      when(io.controller.response_channel.out(i).ready) {
        printf(p"[BUS] And it received!!\n")
        regVld := false.B
      }
    }.otherwise {
      regVld := false.B
    }
  }
   */

  // Bus State Machine
}
