// See README.md for license details.

package components

import chisel3._
import chisel3.util._
import java.io.File

import coherences.{PMESI=>CoherenceState}
object HashTableState {
  val sHASH_INIT :: sHASH_IDLE :: sHASH_WRITE :: sHASH_READ :: sHASH_WAIT :: sHASH_FAILED :: Nil = Enum(6)
}
object HashTableReqType {
  val sHASH_TABLE_READ :: sHASH_TABLE_WRITE :: sHASH_TABLE_REMOVE :: Nil = Enum(3)
}

class HashTableReq(private val genKey: UInt, private val genValue: UInt,
                                         private val req: UInt = UInt(HashTableReqType.sHASH_TABLE_READ.getWidth.W))
  extends Bundle {
  val key : UInt = genKey
  val value : UInt = genValue
  val req_type : UInt = req

  //override def cloneType: this.type = new HashTableReq(genKey, genValue, req).asInstanceOf[this.type]
  override def toPrintable: Printable = {
    p"HashTableReq(" +
      p"key=0x${Hexadecimal(Cat(key.asUInt(), 0.U(6.W)))}(raw=${Hexadecimal(key.asUInt())}), " +
      p"data=0x${Hexadecimal(value.asUInt())}, " +
      p"type=${Hexadecimal(req_type)})"
  }
}


class HashTableResp[K <: Data, V <: Data](private val genKey: K, private val genValue: V,
                                          private val req: UInt = UInt(HashTableReqType.sHASH_TABLE_READ.getWidth.W),
                                          private val ackVal: UInt = UInt(1.W)
                                         )
  extends Bundle {
  val key : K = genKey
  val value : V = genValue
  val req_type : UInt = req
  val ack : UInt = ackVal

  //override def cloneType: this.type = new HashTableResp(genKey, genValue, req, ackVal).asInstanceOf[this.type]
  override def toPrintable: Printable = {
    p"HashTableResp(key=${Hexadecimal(Cat(key.asUInt(), 0.U(6.W)))}(raw=${Hexadecimal(key.asUInt())}), " +
    p"data=${Hexadecimal(value.asUInt())}, ack=$ack)"
  }
}


class HashTableIO(private val genKey: UInt, private val genValue: UInt) extends Bundle {
  val request = Flipped(Decoupled(new HashTableReq(genKey, genValue)))
  val response = Decoupled(new HashTableResp(genKey, genValue))

  //override def cloneType: this.type = new HashTableIO(genKey, genValue).asInstanceOf[this.type]
}

class HashTable(private val depth : Int,
                private val genKey: UInt,
                private val genValue: UInt) extends Module {
  val io = IO(new HashTableIO(genKey, genValue))


  val state = RegInit(HashTableState.sHASH_INIT)
  val q = Module(new Queue(UInt(depth.W), depth))
  val hash_full = WireInit(true.B)
  val req_type_i = Wire(UInt(HashTableReqType.sHASH_TABLE_READ.getWidth.W))
  val req_key_i = Wire(genKey)
  val req_value_i = Wire(genValue)
  val req_reg = Reg(new HashTableReq(genKey, genValue))
  val match_lines = WireInit(VecInit.tabulate(depth)(_ => false.B))
  val ram_key = RegInit(VecInit.tabulate(depth)(_ => (-1).S.asTypeOf(genKey)))
  val ram_value = RegInit(VecInit.tabulate(depth)(_ => (-1).S.asTypeOf(genValue)))
  val valid = RegInit(VecInit.tabulate(depth)(_ => false.B))
  val enable_read = WireInit(false.B)
  val enable_write = WireInit(false.B)
  val enable_remove = WireInit(false.B)
  val has_match_line = WireInit(false.B)
  val read_found_reg = Reg(new HashTableResp(genKey, genValue))
  val remove_found_reg = RegInit(false.B)
  val (init_counter, init_counter_wrap) = Counter(q.io.enq.fire(), depth)

  req_type_i := io.request.bits.req_type
  req_key_i := io.request.bits.key
  req_value_i := io.request.bits.value
  match_lines := valid zip ram_key map {
    case (v, k) =>  v && (req_key_i.asUInt() === k.asUInt())
  }

  // printf(s"in: ready %d valid %d req_type %d enable write %d -- State: %d has_match %d -- out: ready %d valid %d\n",
  //   io.request.ready, io.request.valid, io.request.bits.req_type, enable_write,
  //   state, has_match_line,
  //   io.response.ready, io.response.valid)


  when(io.request.fire()) {
    req_reg := io.request.bits
    //printf(p"[HT] ${io.request.bits}\n")
  }

  when(io.response.fire()) {
    //printf(p"[HT] ${io.response.bits}\n")
  }


  io.request.ready := state === HashTableState.sHASH_IDLE && (!io.request.valid ||
    io.request.valid && io.request.bits.req_type === HashTableReqType.sHASH_TABLE_WRITE && q.io.deq.valid ||
    io.request.valid && io.request.bits.req_type === HashTableReqType.sHASH_TABLE_WRITE && has_match_line ||
    io.request.valid && io.request.bits.req_type =/= HashTableReqType.sHASH_TABLE_WRITE)
  has_match_line := match_lines.reduceLeft[Bool] { case (b, m) => b || m }
  when(io.request.fire()) {
    read_found_reg.req_type := io.request.bits.req_type
    when(has_match_line) {
      read_found_reg.key := ram_key(OHToUInt(match_lines))
      read_found_reg.value := ram_value(OHToUInt(match_lines))
    }.otherwise {
      read_found_reg.key := req_reg.key
      read_found_reg.value := 0.U
    }
    read_found_reg.ack := has_match_line
  }

  // the usage of !hash_full could be replaced with q.io.deq.fire()
  hash_full := !q.io.deq.valid
  enable_write := (state === HashTableState.sHASH_IDLE
    && req_type_i === HashTableReqType.sHASH_TABLE_WRITE
    && io.request.fire())
  enable_read := (state === HashTableState.sHASH_IDLE
    && req_type_i === HashTableReqType.sHASH_TABLE_READ
    && io.request.fire())
  enable_remove := (state === HashTableState.sHASH_IDLE
    && req_type_i === HashTableReqType.sHASH_TABLE_REMOVE
    && io.request.fire())

  when(enable_remove) {
    remove_found_reg := has_match_line
  }

  q.io.deq.ready := enable_write && !has_match_line
  q.io.enq.valid := enable_remove || state === HashTableState.sHASH_INIT
  when(state === HashTableState.sHASH_INIT) {
    q.io.enq.bits := init_counter
  }.otherwise {
    q.io.enq.bits := OHToUInt(match_lines)
  }



  when(state === HashTableState.sHASH_WAIT) {
    io.response.valid := true.B
    io.response.bits.req_type := req_reg.req_type
    io.response.bits.key := req_reg.key
    io.response.bits.value := req_reg.value
    io.response.bits.req_type := req_reg.req_type
    when(req_reg.req_type === HashTableReqType.sHASH_TABLE_REMOVE && !remove_found_reg) {
      io.response.bits.ack := 0.U
    }.otherwise {
      io.response.bits.ack := 1.U
    }
  }.elsewhen(state === HashTableState.sHASH_READ) {
    io.response.valid := true.B
    io.response.bits := read_found_reg
  }.otherwise {
    io.response.valid := false.B
    io.response.bits.key := DontCare
    io.response.bits.value := DontCare
    io.response.bits.req_type := HashTableReqType.sHASH_TABLE_READ
    io.response.bits.ack := 0.U
  }
  when(io.request.valid && req_type_i === HashTableReqType.sHASH_TABLE_WRITE && !(!hash_full || has_match_line)) {
    // printf("[HT] No matched entry found, and no space\n")
    state := HashTableState.sHASH_FAILED
  }
  switch (state) {
    is(HashTableState.sHASH_INIT) {
      // printf(p"[HT] counter: $init_counter, depth $depth\n")
      when(init_counter === (depth - 1).U) {
        // printf(p"[HT] counter: $init_counter, depth $depth\n")
        state := HashTableState.sHASH_IDLE
      }
    }
    is(HashTableState.sHASH_IDLE) {
      when(io.request.fire()) {
        when(req_type_i === HashTableReqType.sHASH_TABLE_WRITE && (!hash_full || has_match_line)) {
          when(!has_match_line) {
            state := HashTableState.sHASH_WRITE
            ram_key(q.io.deq.bits.asUInt()) := req_key_i
            ram_value(q.io.deq.bits.asUInt()) := req_value_i
            valid(q.io.deq.bits.asUInt()) := true.B
          }.elsewhen(has_match_line) { // be careful, here we overwrite the existing entry, could be a problem
            state := HashTableState.sHASH_WRITE
            ram_key(OHToUInt(match_lines)) := req_key_i
            ram_value(OHToUInt(match_lines)) := req_value_i
          }
          /*
          when(has_match_line) {
            // User should first perform read operation and check whether the key has already been inserted
            state := HashTableState.sHASH_FAILED
          }
           */
        }.elsewhen(req_type_i === HashTableReqType.sHASH_TABLE_READ) {
          state := HashTableState.sHASH_READ
        }.elsewhen(req_type_i === HashTableReqType.sHASH_TABLE_REMOVE) {
          when(has_match_line) {
            state := HashTableState.sHASH_WAIT
            ram_value(OHToUInt(match_lines)) := req_value_i
            valid(q.io.enq.bits) := false.B
          }.otherwise {
            state := HashTableState.sHASH_FAILED
          }
        }
      }
    }

    is(HashTableState.sHASH_WRITE) {
      state := HashTableState.sHASH_WAIT
    }

    is(HashTableState.sHASH_READ) {
      when(io.response.fire()) {
        state := HashTableState.sHASH_IDLE
      }
    }

    is(HashTableState.sHASH_WAIT) {
      when(io.response.fire()) {
        state := HashTableState.sHASH_IDLE
      }
    }

    is(HashTableState.sHASH_FAILED) {
      printf("[HT] Failed to perform operation\n")
    }

  }
  //printf(p"[HT] $state \n")

}
