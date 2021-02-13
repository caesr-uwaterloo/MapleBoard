
package components

import chisel3.experimental._
import chisel3.util._
import chisel3._
import params.{CoherenceSpec, MemorySystemParams}
import chiseltest._
import chiseltest.experimental.TestOptionBuilder._
import chisel3.experimental.BundleLiterals._
import components.LinkedList.LinkedListOperation

object PendingRequestLookupTable {

  object PendingReqeustLookupTableRequestType extends ChiselEnum {
    // For the first 3, there should be a bit identifying whether remove that bit
    val NextRequestInGlobalOrder, NextRequestGivenAddress, NextRequestGivenSlot, Insert = Value
  }
  object PendingReqeustLookupTableResponseType extends ChiselEnum {
    val NotFound, Found, FoundAndRemoved, Success, NotEnoughSpace, Undefined = Value
  }
  // Entry should be <RequestType, Core, [Crit]>
  class PRLUTRequestChannel[T <: Data, S <: Data, M <: Data, B <: Data](
                                                                         private val m: MemorySystemParams,
                                                                         private val genEntry: () => T,
                                                                         private val coherenceSpec: CoherenceSpec[S, M, B]
                                                                       ) extends Bundle {
    val requestTypePRLUT = PendingReqeustLookupTableRequestType()
    val queryAndRemove = Bool()
    val tag = m.cacheParams.genTag
    val requestor = UIntHolding(m.masterCount + 1)
    val requestType = coherenceSpec.getGenBusReqTypeF()
    val data = genEntry() // could be criticality or something else
    override def cloneType: this.type = new PRLUTRequestChannel(m, genEntry, coherenceSpec).asInstanceOf[this.type]
  }
  class PRLUTResponseChannel[T <: Data, S <: Data, M <: Data, B <: Data](
                                                                          private val m: MemorySystemParams,
                                                                          private val genEntry: () => T,
                                                                          private val coherenceSpec: CoherenceSpec[S, M, B]
                                                                        ) extends Bundle {
    val responseType = PendingReqeustLookupTableResponseType()
    val tag = m.cacheParams.genTag
    val requestor = UIntHolding(m.masterCount + 1)
    val requestType = coherenceSpec.getGenBusReqTypeF()
    val data = genEntry() // could be criticality or something else
    override def cloneType: this.type = new PRLUTResponseChannel(m, genEntry, coherenceSpec).asInstanceOf[this.type]
  }
  class PendingRequestLookupTableIO[T <: Data, S <: Data, M <: Data, B <: Data](
                                                                                 private val m: MemorySystemParams,
                                                                                 private val genEntry: () => T,
                                                                                 private val coherenceSpec: CoherenceSpec[S, M, B]
                                                                               ) extends Bundle {
    val requestChannel = Flipped(Decoupled(new PRLUTRequestChannel(m, genEntry, coherenceSpec)))
    val responseChannel = Decoupled(new PRLUTResponseChannel(m, genEntry, coherenceSpec))
    val version = Output(UInt(32.W))
  }
  class PRLUTEntry[T <: Data, S <: Data, M <: Data, B <: Data](
                                                                private val m: MemorySystemParams,
                                                                private val genEntry: () => T,
                                                                private val coherenceSpec: CoherenceSpec[S, M, B]
                                                              ) extends Bundle {
    val tag = m.cacheParams.genTag
    val requestor = UIntHolding(m.masterCount)
    val requestType = coherenceSpec.getGenBusReqTypeF()
    // could be critical level or something else
    val data = genEntry()
    val globalOrderEntry = UIntHolding(m.masterCount * m.masterCount * 4)
  }

}
@chiselName
class PendingRequestLookupTable[S <: Data, M <: Data, B <: Data](
                                                                  private val memorySystemParams: MemorySystemParams,
                                                                  private val coherenceSpec: CoherenceSpec[S, M, B],
                                                                  private val removeLowCritOnQuery: Boolean = false
                                                                ) extends Module {
  import PendingRequestLookupTable._
  val getParam = memorySystemParams
  val getCohSpec = coherenceSpec
  val genCrit = memorySystemParams.genCrit
  val io = IO(new PendingRequestLookupTableIO(memorySystemParams, genCrit, coherenceSpec))

  val pipe_in_0 = Module(new Queue(new PRLUTRequestChannel(memorySystemParams, genCrit, coherenceSpec), 1))
  val pipe_in_1 = Module(new Queue(new PRLUTRequestChannel(memorySystemParams, genCrit, coherenceSpec), 1))

  val m = memorySystemParams
  val perAddressOrder = for { i <- 0 until m.masterCount} yield {
    val valid = RegInit(false.B)
    val tag = Reg(UInt(m.cacheParams.tagWidth.W))
    val q = Module(new Queue(new PRLUTEntry(memorySystemParams, genCrit, coherenceSpec),
      if(m.withCriticality) {
        2 * m.masterCount * m.masterCount
      } else {
        m.masterCount
      }
    ))
    (valid, tag, q)
  }
  // only tracks the queue id
  // Just for testing purpose, m.masterCount should be enough
  val globalOrder = Module(new LinkedList(
    if(memorySystemParams.withCriticality) {
      m.masterCount * 2
    } else {
      m.masterCount
    },
    log2Ceil(m.masterCount)
  ))
  val inputQueue = Module(new Queue(new PRLUTRequestChannel(memorySystemParams, genCrit, coherenceSpec), 2 * m.masterCount))
  val outputQueue = Module(new Queue(new PRLUTResponseChannel(memorySystemParams, genCrit, coherenceSpec), 2 * m.masterCount))
  val version = RegInit(0.U(32.W))
  io.version := version

  globalOrder.io.enable := false.B
  globalOrder.io.reqType := LinkedList.LinkedListOperation.PushBack
  globalOrder.io.index := 0.U
  globalOrder.io.din := 0.U

  // matching for dequeing per address
  val addressMatch = Wire(Vec(m.masterCount, Bool()))
  val hasAddressMatch = Wire(Bool())
  val addressMatchCount = Wire(UIntHolding(m.masterCount))
  val addressMatchQueue = OHToUInt(addressMatch)

  // matching for dequeing per slot
  val slotMatch = Wire(Vec(m.masterCount, Bool()))
  val hasSlotMatch = Wire(Bool())
  val slotMatchCount = Wire(UIntHolding(m.masterCount))
  val slotMatchQueue = OHToUInt(slotMatch)

  val isVacant = Wire(Vec(m.masterCount, Bool()))
  val hasVacant = Wire(Bool())
  val vacantQueue = Wire(UIntHolding(m.masterCount))
  for { i <- 0 until m.masterCount } {
    addressMatch(i) := perAddressOrder(i)._1 && perAddressOrder(i)._2 === inputQueue.io.deq.bits.tag
    slotMatch(i) := perAddressOrder(i)._1 && perAddressOrder(i)._3.io.deq.valid &&
      perAddressOrder(i)._3.io.deq.bits.requestor === inputQueue.io.deq.bits.requestor
  }
  vacantQueue := 0.U
  for { i <- (0 until m.masterCount).reverse } {
    isVacant(i) := !perAddressOrder(i)._1
    when(isVacant(i)) {
      vacantQueue := i.U
    }
  }
  hasVacant := isVacant.reduce(_ || _)
  hasAddressMatch := addressMatch.reduce(_ || _)
  hasSlotMatch := slotMatch.reduce(_ || _)
  addressMatchCount := PopCount(addressMatch)
  slotMatchCount := PopCount(slotMatch)
  val entry = Wire(new PRLUTEntry(memorySystemParams, genCrit, coherenceSpec))
  for { i <- 0 until m.masterCount} {}
  val pickQueue = Wire(UIntHolding(m.masterCount))
  pickQueue := 0.U
  entry := perAddressOrder(0)._3.io.deq.bits
  for {i <- 0 until m.masterCount} {
    when(i.U === pickQueue) {
      entry := perAddressOrder(i)._3.io.deq.bits
    }
  }
  when(inputQueue.io.deq.fire()) {
    /*
    printf(" -----     ----- \n")
    printf("Req: ")
    utils.printe(inputQueue.io.deq.bits.requestTypePRLUT)
    printf(p" remove?: ${inputQueue.io.deq.bits.queryAndRemove}")
    printf("\n")
    printf(p"pickQueue: ${pickQueue} isVacant ${isVacant} count: ${globalOrder.io.count} entry:\n")
    utils.printbundle(entry)
    printf(p" hasAddrMatch: ${hasAddressMatch} hasSlotMatch ${hasSlotMatch}\n")
    for { i <- 0 until m.masterCount } {
      printf(p"${i.U} V: ${perAddressOrder(i)._1} Tag: ${Hexadecimal(perAddressOrder(i)._2)} Count: ${perAddressOrder(i)._3.io.count}\n")
    }
    printf(" =====     ===== \n")
    */
  }
  // able to sustain the thru put
  // io.requestChannel <> inputQueue.io.enq
  io.requestChannel <> pipe_in_0.io.enq
  pipe_in_0.io.deq <> pipe_in_1.io.enq
  pipe_in_1.io.deq <> inputQueue.io.enq

  io.responseChannel <> outputQueue.io.deq
  inputQueue.io.deq.ready := outputQueue.io.enq.ready && globalOrder.io.ready
  outputQueue.io.enq.valid := inputQueue.io.deq.valid && globalOrder.io.ready
  val reqData = inputQueue.io.deq.bits
  assert(addressMatchCount <= 1.U, "At most 1 queue should match")

  outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.Undefined
  outputQueue.io.enq.bits.requestor := entry.requestor
  outputQueue.io.enq.bits.requestType := entry.requestType
  outputQueue.io.enq.bits.tag := entry.tag
  outputQueue.io.enq.bits.data := entry.data

  for { i <- 0 until m.masterCount } {
    perAddressOrder(i)._3.io.enq.valid := false.B
    perAddressOrder(i)._3.io.enq.bits.requestType := inputQueue.io.deq.bits.requestType
    perAddressOrder(i)._3.io.enq.bits.requestor := inputQueue.io.deq.bits.requestor
    perAddressOrder(i)._3.io.enq.bits.tag := inputQueue.io.deq.bits.tag
    perAddressOrder(i)._3.io.enq.bits.data := inputQueue.io.deq.bits.data
    perAddressOrder(i)._3.io.enq.bits.globalOrderEntry := globalOrder.io.nextFree

    perAddressOrder(i)._3.io.deq.ready := false.B
  }
  when(inputQueue.io.deq.valid) {
    when(reqData.requestTypePRLUT === PendingReqeustLookupTableRequestType.Insert) {
      // We might also need to remove lo crit request when a new request arrives
      when(hasAddressMatch) {
        pickQueue := addressMatchQueue
        outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.Success
      }.otherwise {
        // insert
        outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.Success
        assert(hasVacant, "must have vacant queue for PRLUT")
        pickQueue := vacantQueue
        when(inputQueue.io.deq.fire())  {
          version := version + 1.U
          for { i <- 0 until m.masterCount } {
            when(pickQueue === i.U) {
              assert(!perAddressOrder(i)._1)
              perAddressOrder(i)._1 := true.B
              perAddressOrder(i)._2 := inputQueue.io.deq.bits.tag
            }
          }
        }
      }
      when(inputQueue.io.deq.fire()) {
        globalOrder.io.din := pickQueue
        globalOrder.io.reqType := LinkedListOperation.PushBack
        globalOrder.io.enable := true.B
        assert(globalOrder.io.vacant, "Must be ready to enqueue global order")
        for {i <- 0 until m.masterCount} {
          when(pickQueue === i.U) {
            perAddressOrder(i)._3.io.enq.valid := true.B
            assert(perAddressOrder(i)._3.io.enq.ready)
            // slotMatch only relevant to dequeue
            when(hasAddressMatch) {
              assert(perAddressOrder(i)._1)
              assert(perAddressOrder(i)._2 === inputQueue.io.deq.bits.tag)
            }
          }
        }
        outputQueue.io.enq.valid := true.B
        assert(outputQueue.io.enq.ready)
      }
    }.elsewhen(reqData.requestTypePRLUT === PendingReqeustLookupTableRequestType.NextRequestGivenAddress) {
      pickQueue := addressMatchQueue
      driveRemoveOn(hasAddressMatch && inputQueue.io.deq.fire())
    }.elsewhen(reqData.requestTypePRLUT === PendingReqeustLookupTableRequestType.NextRequestGivenSlot) {
      pickQueue := slotMatchQueue
      driveRemoveOn(hasSlotMatch && inputQueue.io.deq.fire())
    }.elsewhen(reqData.requestTypePRLUT === PendingReqeustLookupTableRequestType.NextRequestInGlobalOrder) {
      pickQueue := globalOrder.io.headData
      when(globalOrder.io.count > 0.U) {
        when(inputQueue.io.deq.bits.queryAndRemove && inputQueue.io.deq.fire()) {
          version := version + 1.U
          outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.FoundAndRemoved
          globalOrder.io.enable := true.B
          globalOrder.io.reqType := LinkedListOperation.PopFront
          for { i <- 0 until m.masterCount } {
            when(i.U === pickQueue) {
              perAddressOrder(i)._3.io.deq.ready := true.B
              when(perAddressOrder(i)._3.io.count === 1.U) {
                assert(!perAddressOrder(i)._3.io.enq.fire())
                perAddressOrder(i)._1 := false.B
              }
              assert(perAddressOrder(i)._3.io.deq.valid)
            }
          }
        }.otherwise {
          outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.Found
          // Though this will not be used
          if(removeLowCritOnQuery) {
            version := version + 1.U
            for { i <- 0 until m.masterCount } {
              when(i.U === pickQueue) {
                when(perAddressOrder(i)._3.io.deq.bits.data === 5.U && perAddressOrder(i)._3.io.count > 1.U) {
                  globalOrder.io.enable := true.B
                  globalOrder.io.reqType := LinkedListOperation.PopFront
                  outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.NotFound
                  perAddressOrder(i)._3.io.deq.ready := true.B
                  assert(perAddressOrder(i)._3.io.deq.valid)
                }
              }
            }
          }
        }
      }.otherwise {
        outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.NotFound
      }
    }
  }
  def driveRemoveOn(cond: Bool): Unit =
    when(cond) {
      when(inputQueue.io.deq.bits.queryAndRemove) {
        version := version + 1.U
        outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.FoundAndRemoved
        globalOrder.io.enable := true.B
        globalOrder.io.reqType := LinkedListOperation.RemoveIndex
        for {i <- 0 until m.masterCount} {
          when(i.U === pickQueue) {
            // printf(p"Trying to Remove From: ${pickQueue}\n")
            globalOrder.io.index := perAddressOrder(i)._3.io.deq.bits.globalOrderEntry
            perAddressOrder(i)._3.io.deq.ready := true.B
            when(perAddressOrder(i)._3.io.count === 1.U) {
              assert(!perAddressOrder(i)._3.io.enq.fire())
              perAddressOrder(i)._1 := false.B
            }
            assert(perAddressOrder(i)._3.io.deq.valid)
          }
        }
        // printf("Hit Branch A\n")
      }.otherwise {
        // printf("Hit Branch B\n"
        outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.Found
        // generate logic so that low crit requests get removed
        if(removeLowCritOnQuery) {
          version := version + 1.U
          for { i <- 0 until m.masterCount } {
            when(i.U === pickQueue) {
              when(perAddressOrder(i)._3.io.deq.bits.data === 5.U && perAddressOrder(i)._3.io.count > 1.U) {
                globalOrder.io.enable := true.B
                globalOrder.io.reqType := LinkedListOperation.RemoveIndex
                outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.NotFound
                globalOrder.io.index := perAddressOrder(i)._3.io.deq.bits.globalOrderEntry
                perAddressOrder(i)._3.io.deq.ready := true.B
                assert(perAddressOrder(i)._3.io.deq.valid)
              }
            }
          }
        }
      }
    }.otherwise {
      // printf("Hit Branch C\n")
      outputQueue.io.enq.bits.responseType := PendingReqeustLookupTableResponseType.NotFound
    }
}
