
package components

import chisel3._
import chisel3.experimental._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import params.MemorySystemParams

class PendingRequestLUT(val keyWidth: Int, val tableDepth: Int, val idWidth: Int) extends BlackBox(
  Map(
    "KEY_WIDTH" -> keyWidth,
    "TABLE_DEPTH" -> tableDepth,
    "ID_WIDTH" -> idWidth
  )
) {
  override def desiredName: String = "pr_lookup_table"
  val io = IO(new Bundle {
    val clock      =  Input(Clock())
    val reset      =  Input(UInt(1.W))
    val key_i      =  Input(UInt(keyWidth.W))
    val entry_i    =  Input(UInt(new TableEntry(idWidth).getWidth.W))
    val we_i       =  Input(UInt(1.W))
    val enable_i   =  Input(UInt(1.W))
    val found_o    =  Output(UInt(1.W))
    val entry_o    =  Output(UInt(new TableEntry(idWidth).getWidth.W))
    val full_o     =  Output(UInt(1.W))
    val one_left_o =  Output(UInt(1.W))
  })
}

object PRLUTReq {
  val insert: UInt = 0.U
  val search_and_remove: UInt = 1.U
}
/*class PRLUTReq(val genKey: UInt, val genTable: TableEntry, val genReq: UInt) extends Bundle {
  val key = genKey
  val data = genTable
  val request_type = genReq // similar to we

  def this(keyWidth: Int, idWidth: Int) = this(UInt(keyWidth.W), new TableEntry(idWidth), UInt(1.W))

  override def cloneType: this.type = new PRLUTReq(genKey, genTable, genReq).asInstanceOf[this.type]
} */

class PRLUTReq(val keyWidth: Int, val idWidth: Int) extends Bundle {
  val key = UInt(keyWidth.W)
  val data = new TableEntry(idWidth)
  val request_type = UInt(1.W)// similar to we

  //def this() = this(UInt(keyWidth.W), new TableEntry(idWidth), UInt(1.W))

  //override def cloneType: this.type = new PRLUTReq(genKey, genTable, genReq).asInstanceOf[this.type]
}

class PRLUTResp(val keyWidth: Int, val idWidth: Int) extends Bundle {
  val data = new TableEntry(idWidth)
  val found = UInt(1.W)
  val last = UInt(1.W)
}

class PRLUT(val keyWidth: Int, val tableDepth: Int, val idWidth: Int, memorySystemParams: MemorySystemParams) extends Module {
  val genReq = new PRLUTReq(keyWidth, idWidth)
  val genResp = new PRLUTResp(keyWidth, idWidth)
  val genEntry = new TableEntry(idWidth)
  val io = IO(new Bundle {
    val req = Flipped(Decoupled(genReq))
    val resp = Decoupled(genResp)
  })

  val listDepth: Int = tableDepth * tableDepth
  val listAddrWidth: Int = log2Ceil(listDepth)

  val sInit :: sIdle ::  sHashReadRequest :: sHashReadResponse :: sResp :: sNotFound :: sAllocateQueue :: sDeq :: sEnq :: sHashRemoveRequest :: sHashRemoveResponse :: sDeallocateQueue :: sHashWriteResponse :: sHashWriteRequest :: Nil = Enum(14)
  val state = RegInit(sInit)

  val cam = Module(new HashTable(tableDepth, UInt(keyWidth.W), UInt(listAddrWidth.W)))
  val req_reg = Reg(genReq)
  val q_id = Reg(UInt(log2Ceil(tableDepth).W))
  // free list mangages empty lists
  val free_list = Module(new Queue(UInt(log2Ceil(tableDepth).W), tableDepth))
  val (free_list_counter, free_list_counter_wrap) = Counter(state === sInit && free_list.io.enq.fire(), tableDepth)
  val request_lists = for{ i <- 0 until tableDepth } yield {
    val request_list = Module(new BRAMQueue(2 << idWidth)(genEntry))
    request_list
  }
  val request_counters = RegInit(VecInit(Seq.fill(tableDepth)(0.U((1 << idWidth).W))))
  val request_enq = VecInit.tabulate(tableDepth)(request_lists(_).io.enq)
  val request_deq = VecInit.tabulate(tableDepth)(request_lists(_).io.deq)
  val is_one = VecInit.tabulate(tableDepth)((i: Int) => {
    request_counters(i) === 1.U
  })

  when(io.req.fire()) {
    req_reg := io.req.bits
  }



  // stops at GETM response or the last one
  // 1 -> GETM, 0 -> GETS
  io.resp.bits.last :=
    // No other entries in the queue
    is_one(q_id) ||
      // the key is not found at all
      state === sNotFound || (
      // queue top is a GETM request
      request_deq(q_id).valid && request_deq(q_id).bits.req_type === 1.U
      )
  io.resp.bits.data := request_deq(q_id).bits
  io.resp.bits.found := 1.U
  when(state === sNotFound) {
    io.resp.bits.found := 0.U
  }

  val cancelledCount = RegInit(0.U(32.W))

  io.req.ready := state === sIdle
  io.resp.valid := 0.U
  when(state === sNotFound) {
    io.resp.valid := 1.U
  }.elsewhen(state === sDeq) {
    io.resp.valid := request_deq(q_id).valid && (io.resp.bits.last.asBool ||
    (!io.resp.bits.last && !memorySystemParams.isLowCrit(io.resp.bits.data.criticality))
      )
  }
  // cancelled condition
  when(state === sDeq && request_deq(q_id).valid && !io.resp.valid) {
    cancelledCount := cancelledCount + 1.U
  }

  val request_deq_ready = WireInit(VecInit.tabulate(tableDepth)(_ => 0.U(1.W)))
  for{ i <- 0 until tableDepth } {
    request_deq(i).ready := request_deq_ready(i)
  }

  request_deq_ready(q_id) := io.resp.ready && state === sDeq

  for{ i <- 0 until tableDepth } {
    request_enq(i).bits := req_reg.data
    request_enq(i).valid := 0.U
  }
  request_enq(q_id).valid := state === sEnq

  free_list.io.deq.ready := state === sAllocateQueue
  free_list.io.enq.valid := state === sDeallocateQueue || state === sInit
  free_list.io.enq.bits := Mux(state === sInit, free_list_counter, q_id)


  switch(state) {
    is(sInit) {
      when(free_list_counter_wrap) { state := sIdle }
    }
    is(sIdle) {
      when(io.req.fire()) { state := sHashReadRequest }
    }
    is(sHashReadRequest) {
      when(cam.io.request.fire()) { state := sHashReadResponse }
    }
    is(sHashReadResponse) {
      when(cam.io.response.fire()) {
        when(cam.io.response.bits.ack === 1.U) {
          q_id := cam.io.response.bits.value
          when(req_reg.request_type === PRLUTReq.search_and_remove) {
            state := sDeq
          }.elsewhen(req_reg.request_type === PRLUTReq.insert) {
            state := sEnq
          }
        }.otherwise { // not found
          when(req_reg.request_type === PRLUTReq.search_and_remove) {
            state := sNotFound
          }.elsewhen(req_reg.request_type === PRLUTReq.insert) {
            state := sAllocateQueue
          }
        }
      }
    }
    is(sDeq) {
      // or it is skipped
      when(request_deq(q_id).fire()) {
        request_counters(q_id) := request_counters(q_id) - 1.U
        when(io.resp.bits.last === 1.U) {
          when(is_one(q_id)) { // need to remove from cam
            state := sHashRemoveRequest
          }.otherwise {
            state := sIdle
          }
        }
      }
    }
    is(sAllocateQueue) {
      when(free_list.io.deq.fire()) {
        q_id := free_list.io.deq.bits
        state := sHashWriteRequest
      }.otherwise {
        printf("[PRLUT] ERROR getting freelist\n")
      }
    }

    is(sHashWriteRequest) {
      when(cam.io.request.fire()) {
        state := sHashWriteResponse
      }
    }
    is(sHashWriteResponse) {
      when(cam.io.response.fire()) {
        state := sEnq
      }
    }

    is(sEnq) { // no need for response
      when(request_enq(q_id).fire()) {
        request_counters(q_id) := request_counters(q_id) + 1.U
        state := sIdle
      }
    }


    is(sNotFound) {
      when(io.resp.fire()) {
        state := sIdle
      }
    }

    is(sHashRemoveRequest) {
      when(cam.io.request.fire()) {
        state := sHashRemoveResponse
      }
    }
    is(sHashRemoveResponse) {
      when(cam.io.response.fire()) {
        state := sDeallocateQueue
      }
    }
    is(sDeallocateQueue) {
      when(free_list.io.enq.fire()) {
        state := sIdle
      }
    }
  }

  cam.io.request.valid := 0.U
  cam.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_READ
  cam.io.request.bits.key := 0.U
  cam.io.request.bits.value  := 0.U
  switch(state) {
    is(sHashReadRequest) {
      cam.io.request.valid := 1.U
      cam.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_READ
      cam.io.request.bits.key := req_reg.key
      cam.io.request.bits.value := 0.U
    }
    is(sHashWriteRequest) {
      cam.io.request.valid := 1.U
      cam.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_WRITE
      cam.io.request.bits.key := req_reg.key
      cam.io.request.bits.value := q_id
    }
    is(sHashRemoveRequest) {
      cam.io.request.valid := 1.U
      cam.io.request.bits.req_type := HashTableReqType.sHASH_TABLE_REMOVE
      cam.io.request.bits.key := req_reg.key
      cam.io.request.bits.value := 0.U
    }
  }
  cam.io.response.ready := state === sHashReadResponse || state === sHashWriteResponse || state === sHashRemoveResponse

  //printf(p"[PRLUT] state: ${state}\n")
  //when(io.resp.fire()) {
  //  printf(p"[PRLUT] Response: ${io.resp.bits}\n")
  //}
}
