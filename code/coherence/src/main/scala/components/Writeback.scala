
package components

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.experimental.BoringUtils
import params.{CoherenceSpec, MemorySystemParams}

object Writeback {
  class WritebackIO[S <: Data, M <: Data, B <: Data](
                                                      m: MemorySystemParams,
                                                      cohSpec: CoherenceSpec[S, M, B]) extends Bundle {
    val pipe_in = Flipped(Decoupled(new PipeData(m, cohSpec)))
    val bus_request_channel = Decoupled(m.getGenMemReqCommand)
    val dedicated_bus_request_channel = Decoupled(m.getGenMemReqCommand)
    val snoop_response_channel = Decoupled(m.getGenSnoopResp)
    val data_out = Decoupled(UInt(m.busDataWidth.W))
    // data array
    val data_array = new Bundle {
      val wen = Output(Bool())
      val wdata = Output(m.cacheParams.genCacheLine)
      val waddr = Output(m.cacheParams.genSet)
      val wway = Output(m.cacheParams.genWay)
    }
    val tag_array_ctrl = new Bundle {
      val wen = Output(Bool())
      val remove = Output(Bool())
      val insert = Output(Bool())
      val wtag = Output(new TagEntry(m, cohSpec))
      val waddr = Output(m.cacheParams.genSet)
      val wway = Output(m.cacheParams.genWay)
    }
    val pending_mem_ctrl = new Bundle {
      val wen = Output(Bool())
      val waddr = Output(UIntHolding(m.pendingMemSize))
      val insert = Output(Bool())
      val remove = Output(Bool())
      val wdata = Output(new PendingMemoryRequestEntry(m, cohSpec))
    }

    val id = Input(UIntHolding(m.masterCount + 1))
    val time = Input(UIntHolding(128))
    val busy = Input(Bool())
    val sink = Output(Bool())

    val releaseReplay = Output(Bool())
    val criticality = Input(m.genCrit())
    override def cloneType: this.type = new WritebackIO(m, cohSpec).asInstanceOf[this.type]
  }
}

class Writeback[S <: Data, M <: Data, B <: Data](m: MemorySystemParams,
                                                 cohSpec: CoherenceSpec[S, M, B],
                                                 coreid: Int) extends Module {
  val io = IO(new Writeback.WritebackIO(m, cohSpec))
  // this is the sink so it will always be available
  val pipe_data = io.pipe_in.bits
  val coh_resp = pipe_data.coh_resp
  val set = m.cacheParams.getLineAddress(pipe_data.address)
  val way = pipe_data.tr.way
  val newTag = WireInit(pipe_data.tr.tagEntry)
  val newPendingMem = WireInit(pipe_data.tr.pendingMemEntry)
  val busPhase = Wire(UInt(2.W))  // only used in modified atomic
  if(m.useAtomicBusModified) {
    busPhase := 0.U
    BoringUtils.addSink(busPhase, s"CC${coreid}Phase", true)
  } else {
    busPhase := 0.U
  }
  // PWB
  // val pendingWriteback = Module(new Queue(m.getGenMemReqCommand,
  //   entries = m.masterCount * m.pendingMemSize * 2,
  //   pipe = true))
  // if we use atomic bus modified, then the pending memory buffer should only has one entry
  val pendingWriteback = Module(
    if(m.isConventionalProtocol && !m.conventionalSplit) {
      new PendingBuffer( if (m.useAtomicBusModified) { 2 } else { m.masterCount * m.pendingMemSize * 2 },
        m )
    } else {
      new PendingWritebackBuffer( if (m.useAtomicBusModified) { 2 } else { m.masterCount * m.pendingMemSize * 2 },
        m)
    }
  )
  // the replacement buffer, when not atomicModified, should be disabled
  val pendingReplacementWriteback = Module(
    new PendingWritebackBuffer(
      if(m.useAtomicBusModified) { m.pendingMemSize }
      else { 1 } ,
      m)
  )
  if(!m.useAtomicBusModified) {
    assert(!pendingReplacementWriteback.io.enq.valid, "should not use replacement buffer in non atomicModified case")
    assert(!pendingReplacementWriteback.io.cancelEntry.enable, "should not use replacement buffer in non atomicModified case")
  }
  pendingWriteback.io.busy := io.busy
  pendingWriteback.io.markDirty.enable := false.B
  pendingWriteback.io.markDirty.address := pipe_data.tr.tagEntry.tag
  pendingWriteback.io.cancelEntry.enable := false.B
  pendingWriteback.io.cancelEntry.address := 0.U

  pendingReplacementWriteback.io.busy := io.busy
  pendingReplacementWriteback.io.markDirty.enable := false.B
  pendingReplacementWriteback.io.markDirty.address := pipe_data.tr.tagEntry.tag
  pendingReplacementWriteback.io.cancelEntry.enable := false.B
  pendingReplacementWriteback.io.cancelEntry.address := pipe_data.tr.tagEntry.tag


  val locritPendingWriteback = Module(new LoCritPendingWritebackBuffer(m.masterCount * m.pendingMemSize * 2, m))
  locritPendingWriteback.io.busy := io.busy
  locritPendingWriteback.io.cancelEntry.enable := false.B
  // we have to use this. the only occasion where
  // it is cancelled is for snoop
  locritPendingWriteback.io.cancelEntry.address := pipe_data.address

  // PR
  val pendingRequest = Module(new Queue(m.getGenMemReqCommand, entries = m.pendingMemSize, pipe = true))
  val pendingRequestConventional = pendingWriteback
  val clearingPipeIn = io.pipe_in.fire()
  io.sink := io.pipe_in.fire()
  newTag.state := coh_resp.nextState
  io.pipe_in.ready := true.B

  assert(!(clearingPipeIn && coh_resp.defined && coh_resp.broadcastWB && coh_resp.broadcastLoCritWB))
  // PWB
  when(clearingPipeIn && coh_resp.defined) {
    when(coh_resp.markDirty && coh_resp.updateTag && pipe_data.tr.result === TagCheckResult.hit) {
      pendingWriteback.io.markDirty.enable := true.B
      if(m.useAtomicBusModified) {
        pendingReplacementWriteback.io.markDirty.enable := true.B
      }
    }
    when(coh_resp.cancelLoCritPWB) {
      assert(pipe_data.src === ESource.snoop)
      locritPendingWriteback.io.cancelEntry.enable := true.B
    }
  }

  when(clearingPipeIn) {
    io.releaseReplay := coh_resp.releaseReplay
    if(m.useAtomicBusModified) {
      io.releaseReplay := pendingReplacementWriteback.io.count === 0.U && coh_resp.releaseReplay
    }
  }.otherwise {
    io.releaseReplay := false.B
  }


  when(io.pipe_in.valid) {
    when(coh_resp.markDirty) {
      newTag.dirty := true.B
      newPendingMem.dirty := true.B
    }.elsewhen(coh_resp.markClean) {
      newTag.dirty := false.B
      newPendingMem.dirty := false.B
    }
    when(coh_resp.insertTag) {
      newTag.tag := m.cacheParams.getTagAddress(pipe_data.address)
    }
  }

  newPendingMem.state := coh_resp.nextState
  io.pending_mem_ctrl.wen := false.B
  io.pending_mem_ctrl.waddr := pipe_data.freePendingMem
  // we need something for the write addr
  io.pending_mem_ctrl.wdata := newPendingMem
  io.pending_mem_ctrl.remove := false.B
  io.pending_mem_ctrl.insert := false.B
  when(io.sink) {
    /*
    when(coh_resp.broadcastReq) {
      io.pending_mem_ctrl.insert := true.B
      io.pending_mem_ctrl.wdata.valid := true.B
    } */
    when(coh_resp.removePendingMem) {
      io.pending_mem_ctrl.remove := true.B
      io.pending_mem_ctrl.wdata.valid := false.B
      printf(p"Pending Mem Removal...\n")
    }
    when(coh_resp.updatePendingMem) {
      io.pending_mem_ctrl.wen := true.B
      printf(p"Pending Mem To update : ")
      utils.printbundle(newPendingMem)
      printf(p"\n")
      assert(pipe_data.tr.result === TagCheckResult.hitPendingMem, "Update of PendingMem should only happens when the request hits there")
    }

    when(coh_resp.broadcastReq) {
      // this is insert
      when(pipe_data.tr.result === TagCheckResult.missVacant ||
        (pipe_data.tr.result === TagCheckResult.hit &&
          pipe_data.coh_resp.broadcast.asTypeOf(BusRequestType()) === BusRequestType.GetM)) {
        io.pending_mem_ctrl.insert := true.B
        printf(p"Pending Mem To Write: ")
        utils.printbundle(newPendingMem)
        printf(p"\n")
      }.otherwise {
        assert(pipe_data.tr.result === TagCheckResult.hit && (pipe_data.coh_resp.broadcast.asTypeOf(BusRequestType()) === BusRequestType.Upg ||
          pipe_data.coh_resp.broadcast.asTypeOf(BusRequestType()) === BusRequestType.GetM)
          , "Only Upg does not require pushing into the memory")
      } // otherwise, it is, for example, an Upgrade, and the state is in the cache <- not true, we also have it in the pending mem entry...
      // this is because later on it may be changed to IM_W
      // And it is not a hit
      newPendingMem.issued := true.B
      newPendingMem.valid := true.B
      newPendingMem.state := coh_resp.nextState
      // if we know that this request is generated by a Store, we can actually mark it as dirty
      newPendingMem.busRequestType := coh_resp.broadcast
      newPendingMem.tag := m.cacheParams.getTagAddress(io.pipe_in.bits.address)
      // we need to specify the way to write (or way to replace!)
      newPendingMem.way := io.pipe_in.bits.tr.way
    }
  }
  io.pending_mem_ctrl.wdata := newPendingMem

  // for snoop, simply do the transition and respond
  io.snoop_response_channel.bits.criticality := io.criticality
  io.snoop_response_channel.bits.hasMatched := (pipe_data.tr.result === TagCheckResult.hitPendingMem || pipe_data.tr.result === TagCheckResult.hit).asUInt
  io.snoop_response_channel.valid := false.B
  // This shows the owners...
  io.snoop_response_channel.bits.ack := pipe_data.tr.result === TagCheckResult.hit
  when(io.pipe_in.fire()) {
    when(pipe_data.src === ESource.snoop && !pipe_data.isDedicatedWB) {
      io.snoop_response_channel.valid := true.B
      assert(io.snoop_response_channel.ready, "snoop queue does not have enough buffer space")
    }
  }


  /* pending writeback buffer ctrl */
  // We move this ahead of pendingRequest is because some logic there might operate on the interface
  pendingWriteback.io.enq.valid := false.B
  pendingWriteback.io.enq.bits.address := m.cacheParams.alignToCacheline(pipe_data.address)
  pendingWriteback.io.enq.bits.req_type := messageConversion(coh_resp.broadcast)
  pendingWriteback.io.enq.bits.requester_id := io.id
  pendingWriteback.io.enq.bits.req_wb := true.B
  pendingWriteback.io.enq.bits.hasMatched := 0.U
  when(pipe_data.isReplacement) {
    pendingWriteback.io.enq.bits.address := m.cacheParams.tagAddrToLineAddr(pipe_data.tr.tagEntry.tag)
  }

  /* for the two buffers, we always assume that they have enough buffer space */
  /* pending Request buffer ctrl */
  pendingRequest.io.enq.valid := false.B
  pendingRequest.io.enq.bits.address := m.cacheParams.alignToCacheline(pipe_data.address)
  pendingRequest.io.enq.bits.req_type := messageConversion(coh_resp.broadcast)
  pendingRequest.io.enq.bits.requester_id := io.id
  pendingRequest.io.enq.bits.req_wb := false.B
  pendingRequest.io.enq.bits.dirty := false.B
  pendingRequest.io.enq.bits.criticality := io.criticality
  pendingRequest.io.enq.bits.hasMatched := 0.U

  when(clearingPipeIn && coh_resp.broadcastReq) {
    if(m.isConventionalProtocol && !m.conventionalSplit) {
      pendingRequestConventional.io.enq.valid := true.B
      pendingRequestConventional.io.enq.bits.req_wb := false.B
      assert(pendingRequestConventional.io.enq.ready, "Not enough buffer space for FIFO")
    } else {
      pendingRequest.io.enq.valid := true.B
      assert(pendingRequest.io.enq.ready, "Not enough buffer space for pending request")
    }
  }.elsewhen(clearingPipeIn && coh_resp.prResend) {
    assert(!(m.isConventionalProtocol && !m.conventionalSplit).B, "the PR.resend() is not supported in conventional protocols")
    // resend, only take place for the lo crit core
    pendingRequest.io.enq.valid := true.B
    pendingRequest.io.enq.bits.address := m.cacheParams.tagAddrToLineAddr(pipe_data.tr.pendingMemEntry.tag)
    pendingRequest.io.enq.bits.req_type := messageConversion(pipe_data.tr.pendingMemEntry.busRequestType)
    pendingRequest.io.enq.bits.requester_id := io.id
    pendingRequest.io.enq.bits.req_wb := false.B
    pendingRequest.io.enq.bits.dirty := false.B
    pendingRequest.io.enq.bits.criticality := io.criticality
    pendingRequest.io.enq.bits.hasMatched := 0.U
  }

  // NOTE: this is strange because of something like IM_DI
  pendingWriteback.io.enq.bits.dirty := true.B
  when(pipe_data.tr.result === TagCheckResult.hitPendingMem) {
    pendingWriteback.io.enq.bits.dirty := pipe_data.tr.pendingMemEntry.dirty
  }.elsewhen(pipe_data.tr.result === TagCheckResult.hit || pipe_data.tr.result === TagCheckResult.missFull) {
    // replacement or snoop eviction
    pendingWriteback.io.enq.bits.dirty := pipe_data.tr.tagEntry.dirty
  }
  pendingWriteback.io.enq.bits.criticality := io.criticality
  when(clearingPipeIn) {
    when(coh_resp.markDirty) {
      pendingWriteback.io.enq.bits.dirty := true.B
      pendingWriteback.io.markDirty.enable := true.B
      pendingWriteback.io.markDirty.address := pipe_data.address
    }.elsewhen(coh_resp.markClean) {
      pendingWriteback.io.enq.bits.dirty := false.B
    }
  }
  // only enqueue PWB if it is not a replacement when modified
  when(clearingPipeIn && coh_resp.broadcastWB && (if(m.useAtomicBusModified) { !pipe_data.isReplacement } else { true.B })) {
    pendingWriteback.io.enq.valid := true.B
    assert(pendingWriteback.io.enq.ready, "Not enough buffer space for pending writeback (cache %d)", io.id)
    // we still need to drive the data somewhere
  }

  /* only for atomic replacement */
  pendingReplacementWriteback.io.enq.valid := false.B
  pendingReplacementWriteback.io.enq.bits.address := m.cacheParams.alignToCacheline(pipe_data.address)
  pendingReplacementWriteback.io.enq.bits.req_type := messageConversion(coh_resp.broadcast)
  pendingReplacementWriteback.io.enq.bits.requester_id := io.id
  pendingReplacementWriteback.io.enq.bits.req_wb := true.B
  pendingReplacementWriteback.io.enq.bits.hasMatched := 0.U
  when(pipe_data.isReplacement) {
    pendingReplacementWriteback.io.enq.bits.address := m.cacheParams.tagAddrToLineAddr(pipe_data.tr.tagEntry.tag)
  }
  // NOTE: this is strange because of something like IM_DI
  pendingReplacementWriteback.io.enq.bits.dirty := true.B
  when(pipe_data.tr.result === TagCheckResult.hitPendingMem) {
    pendingReplacementWriteback.io.enq.bits.dirty := pipe_data.tr.pendingMemEntry.dirty
  }.elsewhen(pipe_data.tr.result === TagCheckResult.hit || pipe_data.tr.result === TagCheckResult.missFull) {
    // replacement or snoop eviction
    pendingReplacementWriteback.io.enq.bits.dirty := pipe_data.tr.tagEntry.dirty
  }
  pendingReplacementWriteback.io.enq.bits.criticality := io.criticality
  when(clearingPipeIn) {
    when(coh_resp.markDirty) {
      pendingReplacementWriteback.io.enq.bits.dirty := true.B
      pendingReplacementWriteback.io.markDirty.enable := true.B
      pendingReplacementWriteback.io.markDirty.address := pipe_data.address
    }.elsewhen(coh_resp.markClean) {
      pendingReplacementWriteback.io.enq.bits.dirty := false.B
    }
  }
  // only for replacement
  if(m.useAtomicBusModified) {
    when(clearingPipeIn && coh_resp.broadcastWB && pipe_data.isReplacement) {
      pendingReplacementWriteback.io.enq.valid := true.B
      assert(pendingReplacementWriteback.io.enq.ready, "[CC%d] Not enough buffer space for pending writeback", io.id)
      // we still need to drive the data somewhere
    }
  }


  /* pending wb buffer ctrl */
  locritPendingWriteback.io.enq.valid := false.B
  locritPendingWriteback.io.enq.bits.address := m.cacheParams.alignToCacheline(pipe_data.address)
  locritPendingWriteback.io.enq.bits.req_type := messageConversion(coh_resp.broadcast)
  locritPendingWriteback.io.enq.bits.requester_id := io.id
  locritPendingWriteback.io.enq.bits.req_wb := true.B
  locritPendingWriteback.io.enq.bits.hasMatched := 0.U
  locritPendingWriteback.io.enq.bits.criticality := io.criticality
  locritPendingWriteback.io.enq.bits.dirty := true.B
  if(m.withLoCritPWB) {
    when(pipe_data.isReplacement) {
      // we should not insert to this buffer
    }
    locritPendingWriteback.io.enq.bits.dirty := true.B
    // we don't need to mark dirty because clean writeback only happens in PMESI
    when(pipe_data.tr.result === TagCheckResult.hitPendingMem) {
      locritPendingWriteback.io.enq.bits.dirty := pipe_data.tr.pendingMemEntry.dirty
    }.elsewhen(pipe_data.tr.result === TagCheckResult.hit || pipe_data.tr.result === TagCheckResult.missFull) {
      // replacement or snoop eviction
      locritPendingWriteback.io.enq.bits.dirty := pipe_data.tr.tagEntry.dirty
    }
    when(coh_resp.markDirty) {
      locritPendingWriteback.io.enq.bits.dirty := true.B
    }.elsewhen(coh_resp.markClean) {
      locritPendingWriteback.io.enq.bits.dirty := false.B
    }
    when(clearingPipeIn && coh_resp.broadcastLoCritWB) {
      locritPendingWriteback.io.enq.valid := true.B
      assert(locritPendingWriteback.io.enq.ready, "Not enough buffer space for pending writeback")
      // we still need to drive the data somewhere
    }
  }

  val rrCounter = RegInit(0.U(1.W))
  // a hack...alternating every slot, but not whether they are firing...
  when(io.bus_request_channel.ready && (if(m.useAtomicBusModified) { busPhase === 0.U } else { true.B }) ) {
    rrCounter := rrCounter + 1.U
  }
  dontTouch(rrCounter)

  pendingRequest.io.deq.ready := false.B
  pendingReplacementWriteback.io.deq.ready := false.B
  pendingWriteback.io.deq.ready := false.B
  if(m.isConventionalProtocol) {
    io.dedicated_bus_request_channel.valid := false.B
    io.dedicated_bus_request_channel.bits := 0.U.asTypeOf(chiselTypeOf(io.dedicated_bus_request_channel.bits))
    locritPendingWriteback.io.deq.ready := false.B
    m.getDataBusConf match {
      case SharedEverything => {
        // simply connet PWB as it is merged
        if(!m.conventionalSplit) {
          io.bus_request_channel <> pendingWriteback.io.deq
        } else {
          when(pendingRequest.io.deq.valid) {
            io.bus_request_channel <> pendingRequest.io.deq
          }.otherwise {
            io.bus_request_channel <> pendingWriteback.io.deq
          }
        }
      }
      case DedicatedDataBusOneWay | DedicatedDataBusTwoWay => {
        assert(false, "Conventional protocols currently do not separate between requests and write-backs")
      }
    }
  } else {
    // The original setup
    m.getDataBusConf match {
      case SharedEverything => {
        locritPendingWriteback.io.deq.ready := false.B
        io.dedicated_bus_request_channel.valid := false.B
        io.dedicated_bus_request_channel.bits := 0.U.asTypeOf(chiselTypeOf(io.dedicated_bus_request_channel.bits))
        assert(!io.dedicated_bus_request_channel.ready, "Shared everything should not assert dedicated")
        when(rrCounter === 0.U) {
          // request queue
          when(pendingRequest.io.count > 0.U) {
            if (!m.useAtomicBusModified) {
              // if we do not use modified atomic bus
              io.bus_request_channel <> pendingRequest.io.deq
            } else if (m.useAtomicBusModified) {
              // otherwise, we pick whichever is valid
              assert(m.pendingMemSize == 1) // currently the following logic only works for 1 pending request
              when(pendingReplacementWriteback.io.deq.valid) {
                io.bus_request_channel <> pendingReplacementWriteback.io.deq
              }.otherwise {
                io.bus_request_channel <> pendingRequest.io.deq
              }
            }
          }.otherwise {
            if (!m.withLoCritPWB) {
              io.bus_request_channel <> pendingWriteback.io.deq
            } else {
              when(pendingWriteback.io.deq.valid) {
                io.bus_request_channel <> pendingWriteback.io.deq
              }.otherwise {
                io.bus_request_channel <> locritPendingWriteback.io.deq
              }
            }
          }
        }.otherwise {
          assert(rrCounter === 1.U)
          when(pendingWriteback.io.count > 0.U) {
            if (!m.withLoCritPWB) {
              io.bus_request_channel <> pendingWriteback.io.deq
            } else {
              when(pendingWriteback.io.deq.valid) {
                io.bus_request_channel <> pendingWriteback.io.deq
              }.otherwise {
                io.bus_request_channel <> locritPendingWriteback.io.deq
              }
            }
          }.otherwise {
            if (!m.useAtomicBusModified) {
              // if we do not use modified atomic bus
              io.bus_request_channel <> pendingRequest.io.deq
            } else if (m.useAtomicBusModified) {
              // otherwise, we pick whichever is valid
              assert(m.pendingMemSize == 1) // currently the following logic only works for 1 pending request
              when(pendingReplacementWriteback.io.deq.valid) {
                io.bus_request_channel <> pendingReplacementWriteback.io.deq
              }.otherwise {
                io.bus_request_channel <> pendingRequest.io.deq
              }
            }
          }
        }
        // Override the port when the bus phase is 1
        if (m.useAtomicBusModified) {
          when(busPhase === 1.U) {
            io.bus_request_channel <> pendingWriteback.io.deq
            pendingRequest.io.deq.ready := false.B
            pendingReplacementWriteback.io.deq.ready := false.B
          }
        }
      }
      case DedicatedDataBusOneWay | DedicatedDataBusTwoWay => {
        require(!m.useAtomicBusModified)
        io.bus_request_channel <> pendingRequest.io.deq
        locritPendingWriteback.io.deq.ready := false.B
        when(pendingWriteback.io.deq.valid) {
          io.dedicated_bus_request_channel <> pendingWriteback.io.deq
        }.otherwise {
          io.dedicated_bus_request_channel <> locritPendingWriteback.io.deq
        }
      }
    }
  }
  dontTouch(locritPendingWriteback.io.deq)

  // Migrate from replacement writeback to pending writeback, only activated for atomicModified
  if(m.useAtomicBusModified) {
    when(coh_resp.migrateFromRepl && clearingPipeIn) {
      // remove request from replacement
      assert(!pendingReplacementWriteback.io.enq.fire(), "should not issue to both queue")
      pendingReplacementWriteback.io.cancelEntry.enable := true.B
      pendingReplacementWriteback.io.cancelEntry.address := pendingWriteback.io.enq.bits.address
      pendingWriteback.io.enq.valid := pendingReplacementWriteback.io.count > 0.U
      pendingWriteback.io.enq.bits := pendingReplacementWriteback.io.deq.bits
      assert(pendingWriteback.io.enq.ready)
    }
  }


  // Currently, this should only work for the Upg request
  if(m.isConventionalProtocol && !m.conventionalSplit) {
    pendingRequestConventional.io.cancelEntry.enable := false.B
    when(clearingPipeIn) {
      when(pipe_data.coh_resp.defined && pipe_data.coh_resp.cancelPRHead) {
        // In the case of conventional protocol, simply remove it from the PWB
        pendingRequestConventional.io.cancelEntry.enable := true.B
        pendingRequestConventional.io.cancelEntry.address := pipe_data.address
        assert(pipe_data.tr.tagEntry.tag === m.cacheParams.getTagAddress(pipe_data.address),
          "Tag should match entry of interest %x %x", pipe_data.tr.tagEntry.tag,
          m.cacheParams.getTagAddress(pipe_data.address))
      }
    }
  } else {
    when(clearingPipeIn) {
      when(pipe_data.coh_resp.defined && pipe_data.coh_resp.cancelPRHead) {
        io.bus_request_channel.valid := false.B
        pendingRequest.io.deq.ready := true.B
        assert(pendingRequest.io.deq.valid, "CC%d should only dequeue when there is an entry", io.id)
        assert(pendingRequest.io.deq.bits.req_type === RequestType.UPG.U)
        assert(pendingRequest.io.deq.fire())
      }
    }
  }

  fillDataControl(coh_resp.updateData && io.pipe_in.valid, set, way, pipe_data.data)
  fillTagControl(coh_resp.updateTag && io.pipe_in.valid,
    coh_resp.removeTag && io.pipe_in.valid,
    coh_resp.insertTag && io.pipe_in.valid, set, way, newTag)

  private def fillDataControl[T <: Data](wen: Bool, waddr: UInt, wway: UInt, wdata: T): Unit = {
    io.data_array.wen := wen
    io.data_array.waddr := waddr
    io.data_array.wway := wway
    io.data_array.wdata := wdata.asTypeOf(m.cacheParams.genCacheLine)
  }
  private def fillTagControl(wen: Bool, remove: Bool, insert: Bool, waddr: UInt, wway: UInt, wtag: TagEntry): Unit = {
    io.tag_array_ctrl.wen := wen
    io.tag_array_ctrl.remove := remove
    io.tag_array_ctrl.insert := insert
    io.tag_array_ctrl.waddr := waddr
    io.tag_array_ctrl.wway := wway
    io.tag_array_ctrl.wtag := wtag
  }
  private def messageConversion(newMessage: B): UInt = {
    // This function converts the new message format into the old message format
    // (so that it is compliant with the original code...)
    val output = WireInit(0.U(RequestType.getWidth.W))
    val msg = WireInit(newMessage)
    when(newMessage.asUInt === cohSpec.GetM.asUInt) {
      output := RequestType.GETM.U
    }.elsewhen(newMessage.asUInt === cohSpec.GetS.asUInt) {
      output := RequestType.GETS.U
    }.elsewhen(newMessage.asUInt === cohSpec.Upg.asUInt) {
      output := RequestType.UPG.U
    }.elsewhen(newMessage.asUInt === cohSpec.PutM.asUInt) {
      output := RequestType.PUTM.U
    }.elsewhen(newMessage.asUInt === cohSpec.PutS.asUInt) {
      output := RequestType.PUTS.U
    }.otherwise {
      when(io.pipe_in.valid && pipe_data.coh_resp.defined && (pipe_data.coh_resp.broadcastReq ||
        pipe_data.coh_resp.broadcastWB)) {
        printf("New Msg: ")
        utils.printe(newMessage.asInstanceOf[EnumType])
        printf("\n")
        assert(false.B,
          "The translation is not supported for newMessage (asUInt: %d), maybe it should not exist, in CC %d",
          msg.asUInt, io.id)
      }
    }
    output
  }
  val dataQueue = Module(new Queue(m.cacheParams.genCacheLine, entries=m.masterCount + 1, pipe=true))
  dataQueue.io.enq.valid := io.pipe_in.fire() && coh_resp.pushDataBus
  dataQueue.io.enq.bits := pipe_data.data.asTypeOf(m.cacheParams.genCacheLine)
  dontTouch(dataQueue.io.count)
  when(io.pipe_in.valid) {
    assert(dataQueue.io.enq.ready, "not enough data buffer, in CC%d", io.id)
  }

  // simple counter for piping the data out thru data channel
  val beats = m.cacheParams.lineWidth / m.busDataWidth
  val busDataQueue = Module(new Queue(UInt(m.busDataWidth.W), entries=beats, pipe=true))
  val (dataCounter, dataCounterWrap) = Counter(busDataQueue.io.enq.fire(), beats)
  val busDataVec = Wire(Vec(beats, UInt(m.busDataWidth.W)))
  busDataVec := dataQueue.io.deq.bits.asTypeOf(busDataVec)
  busDataQueue.io.enq.bits := busDataVec(dataCounter)
  busDataQueue.io.enq.valid := dataQueue.io.deq.valid
  dataQueue.io.deq.ready := false.B
  when(dataCounterWrap) {
    dataQueue.io.deq.ready := true.B
    assert(busDataQueue.io.enq.ready, "must not stall")
    assert(dataQueue.io.deq.valid, "must not stall")
  }
  io.data_out <> busDataQueue.io.deq

  when(io.pipe_in.valid) {
    printf("=== [CC%d.Writeback] @%d ===\n", io.id, io.time)
    // utils.printbundle(pipe_data)
    printf("\n")
    // printf(p">>>> ${m.cacheParams.alignToCacheline(pipe_data.address)}, ${pipe_data.address}\n")
    printf("[PendingMemReq] entries: %d (enquing? %b)\n", pendingRequest.io.count, pendingRequest.io.enq.fire())
    when(pendingRequest.io.enq.fire()) {
      printf(p"     ${pendingRequest.io.enq.bits}\n")
    }
    printf("[PendingMemWBk] entries: %d (enquing? %b)\n", pendingWriteback.io.count, pendingWriteback.io.enq.fire())
    when(pendingWriteback.io.enq.fire()) {
      printf(p"     ${pendingWriteback.io.enq.bits}\n")
    }
    assert(io.pipe_in.ready, "CC%d: Writeback stage should never stall", io.id)
  }
}
