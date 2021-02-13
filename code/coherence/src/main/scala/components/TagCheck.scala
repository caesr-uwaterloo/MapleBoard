
package components

import chisel3._
import chisel3.util._
import coherences.{CoherenceMessage, RelativeCriticality}
import params.{CoherenceSpec, MemorySystemParams}

class TagCheck[S <: Data, M <: Data, B <: Data](m: MemorySystemParams,
                                     cohSpec: CoherenceSpec[S, M, B]) extends Module {
  val io = IO(new Bundle {
    val pipe_in = Flipped(Decoupled(new PipeData(m, cohSpec)))
    val pipe_out = Decoupled(new PipeData(m, cohSpec))

    val tags = Input(Vec(m.cacheParams.nWays, new TagEntry(m, cohSpec)))
    val tag_valid = Input(Vec(m.cacheParams.nWays, Bool()))
    val data = Input(Vec(m.cacheParams.nWays, m.cacheParams.genCacheLineBytes))
    val pendingMem = Input(Vec(m.pendingMemSize, new PendingMemoryRequestEntry(m, cohSpec)))
    val pendingMemValid = Input(Vec(m.pendingMemSize, Bool()))
    val mshrValid = Input(Vec(m.MSHRSize, Bool()))
    val coh_resp = Output(new CoherenceResponse(cohSpec.getGenStateF, cohSpec.getGenBusReqTypeF))
    // when it is a replacement, the requet is sipmly put in the replay buffer
    val id = Input(UIntHolding(m.masterCount + 1))
    val time = Input(UIntHolding(128))

    val criticality = Input(m.genCrit())
    val query_coverage = Output(ValidIO(cohSpec.getGenCohQuery))
  })
  dontTouch(io.pipe_in.bits.address)
  val nWays = m.cacheParams.nWays
  io.pipe_out <> io.pipe_in
  val pipe_data = io.pipe_in.bits
  // checking the tag
  val tagMatch = WireInit(VecInit(Seq.fill(nWays) { false.B }))
  val pendingMemMatch = WireInit(VecInit(Seq.fill(m.pendingMemSize) { false.B }))
  val hitTag = WireInit(false.B)
  val hitWay = Wire(m.cacheParams.genWay)
  val hitPendingMem = WireInit(false.B)
  val hitPendingMemId = Wire(UIntHolding(m.pendingMemSize))
  val vacantTag = WireInit(false.B)
  val vacantWay = WireInit(0.U.asTypeOf(m.cacheParams.genWay))
  val victimRR = RegInit(0.U.asTypeOf(m.cacheParams.genWay))
  val result = Wire(TagCheckResult())
  val tag = m.cacheParams.getTagAddress(io.pipe_in.bits.address)
  for { i <- 0 until nWays } {
    tagMatch(i) := tag === io.tags(i).tag && io.tag_valid(i)
    when(!io.tag_valid(i)) {
      vacantWay := i.U
    }
  }
  hitTag := tagMatch.reduceLeft(_ || _)
  hitWay := OHToUInt(tagMatch)
  vacantTag := !io.tag_valid.reduceLeft(_ && _)

  for { i <- 0 until m.pendingMemSize } {
    pendingMemMatch(i) := io.pendingMemValid(i) && io.pendingMem(i).tag === tag
  }
  hitPendingMem := pendingMemMatch.reduceLeft(_ || _)
  hitPendingMemId := OHToUInt(pendingMemMatch)

  val vacantMSHR = WireInit(0.U(log2Ceil(m.MSHRSize + 1).W))
  val vacantPendingMem = WireInit(0.U(log2Ceil(m.pendingMemSize + 1).W))
  for { i <- 0 until m.MSHRSize } {
    when(!io.mshrValid(i)) {
      vacantMSHR := i.U
    }
  }
  for { i <- 0 until m.pendingMemSize } {
    when(!io.pendingMemValid(i)) {
      vacantPendingMem := i.U
    }
  }

  when(hitTag) {
    result := TagCheckResult.hit
    io.pipe_out.bits.tr.way := hitWay
    io.pipe_out.bits.tr.tagEntry := io.tags(hitWay)
    io.pipe_out.bits.data := io.data(hitWay)
  }.elsewhen(hitPendingMem) {
    result := TagCheckResult.hitPendingMem
    io.pipe_out.bits.tr.way := io.pendingMem(hitPendingMemId).way
    io.pipe_out.bits.tr.pendingMemEntry := io.pendingMem(hitPendingMemId)
    io.pipe_out.bits.tr.hitPendingMemId := hitPendingMemId
  }.elsewhen(vacantTag) {
    result := TagCheckResult.missVacant
    io.pipe_out.bits.tr.way := vacantWay
  }.otherwise {
    result := TagCheckResult.missFull
    if(m.cacheParams.nWays == 1) {
      // if it is only 1-way, we can only use way 0
      io.pipe_out.bits.tr.way := 0.U
    } else {
      io.pipe_out.bits.tr.way := victimRR
    }
    io.pipe_out.bits.tr.tagEntry := io.tags(victimRR)
    // to be evicted...
    victimRR := victimRR + 1.U
  }
  /** now we can have event encoders (and query the coherence table) */

  val coherenceTable = Module(
    CoherenceSpec.translateToPrivateModule(cohSpec)()
  )
  val loCritCoherenceTable = Module(
    CoherenceSpec.translateToLoCritPrivateModule(cohSpec)()
  )
  coherenceTable.io.enable := io.pipe_in.valid
  coherenceTable.io.query := 0.U.asTypeOf(cohSpec.getGenCohQuery)
  loCritCoherenceTable.io.enable := io.pipe_in.valid
  loCritCoherenceTable.io.query := 0.U.asTypeOf(cohSpec.getGenCohQuery)
  if(!m.withCriticality) {
    io.coh_resp := coherenceTable.io.resp
  } else {
    when(m.isLowCrit(io.criticality)) {
      io.coh_resp := loCritCoherenceTable.io.resp
    }.otherwise {
      io.coh_resp := coherenceTable.io.resp
    }
  }

  val eventEncoder = Module(new EEncoder(m))
  eventEncoder.io.core := pipe_data.core
  eventEncoder.io.snoop := pipe_data.snoop
  eventEncoder.io.id := io.id
  eventEncoder.io.isDedicatedWB := pipe_data.isDedicatedWB
  eventEncoder.io.tag := result
  eventEncoder.io.mem := pipe_data.mem
  eventEncoder.io.src := pipe_data.src

  coherenceTable.io.query.message := eventEncoder.io.event
  loCritCoherenceTable.io.query.message := eventEncoder.io.event
  // the state is stored in two places or otherwise it is the Invalid state
  when(hitTag) {
    coherenceTable.io.query.state := io.tags(hitWay).state
    loCritCoherenceTable.io.query.state := io.tags(hitWay).state
  }.elsewhen(hitPendingMem) {
    coherenceTable.io.query.state := io.pendingMem(hitPendingMemId).state
    loCritCoherenceTable.io.query.state := io.pendingMem(hitPendingMemId).state
  }.elsewhen(result === TagCheckResult.missFull) {
    when(eventEncoder.io.event === CoherenceMessage.Replacement) {
      coherenceTable.io.query.state := io.tags(victimRR).state
      loCritCoherenceTable.io.query.state := io.tags(victimRR).state
    }.otherwise {
      coherenceTable.io.query.state := 0.U.asTypeOf(cohSpec.getGenState)
      loCritCoherenceTable.io.query.state := 0.U.asTypeOf(cohSpec.getGenState)
    }
  }.otherwise {
    coherenceTable.io.query.state := 0.U.asTypeOf(cohSpec.getGenState)
    loCritCoherenceTable.io.query.state := 0.U.asTypeOf(cohSpec.getGenState)
  }
  /** connection for relative criticality */
  when(eventEncoder.io.src === ESource.snoop) {
    // only valid when it's from snoop
    when(!eventEncoder.io.isDedicatedWB) {
      when(pipe_data.snoop.criticality < io.criticality) {
        coherenceTable.io.query.relCrit := RelativeCriticality.HiCrit
        loCritCoherenceTable.io.query.relCrit := RelativeCriticality.HiCrit
      }.elsewhen(pipe_data.snoop.criticality > io.criticality) {
        coherenceTable.io.query.relCrit := RelativeCriticality.LoCrit
        loCritCoherenceTable.io.query.relCrit := RelativeCriticality.LoCrit
      }
    }.otherwise {
      loCritCoherenceTable.io.query.relCrit := RelativeCriticality.SameCrit
    }
    // otherwise, it's same crit
  }

  /** connection for output */
  io.pipe_out.bits.tr.result := result
  io.pipe_out.bits.freeMSHR := vacantMSHR
  io.pipe_out.bits.freePendingMem := vacantPendingMem
  io.pipe_out.bits.isReplacement := eventEncoder.io.event === CoherenceMessage.Replacement
  /** for debugging purpose */
  val totHitTag = PopCount(tagMatch)
  assert(totHitTag <= 1.U, "hit in multiple tags")

  io.query_coverage.valid := io.pipe_in.valid
  io.query_coverage.bits := coherenceTable.io.query

  when(coherenceTable.io.resp.defined && io.pipe_in.valid) {
    assert(!coherenceTable.io.resp.isErr, "should not cause error")
  }
  when(loCritCoherenceTable.io.resp.defined && io.pipe_in.valid) {
    assert(!loCritCoherenceTable.io.resp.isErr, "should not cause error")
  }

  when(io.pipe_in.valid) {
    printf("=== [CC%d.TagCheck] @%d ===\n", io.id, io.time)
    // printf("Pipe Data: ")
    // utils.printbundle(pipe_data)
    // printf("\n")
    printf("\n")
    utils.printbundle(io.pipe_out.bits.tr)
    printf("\n")
    utils.printbundle(coherenceTable.io.query)
    printf("\n")
    // utils.printbundle(io.pipe_out.bits.coh_resp)
    printf("\n")
    for { i <- 0 until nWays } {
      printf("Valid: %b ", io.tag_valid(i))
      utils.printbundle(io.tags(i))
      printf("\n")
    }
  }

}
