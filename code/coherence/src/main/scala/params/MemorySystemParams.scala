
package params

import chisel3._
import coherence.internal.AutoEnum
import components._

case class MemorySystemParams
(// the address used in the memory sub-system, now it should be the same as the one in core
 addrWidth: Int,
 interfaceAddrWidth: Int,
 // dataWidth is the word size used in the interface between core and cache
 dataWidth: Int,
 // specifically for predictable arbiters like TDM
 slotWidth: Int,
 busDataWidth: Int,
 busRequestType: AutoEnum,
 masterCount: Int,
 CohS: AutoEnum,
 cacheParams: SimpleCacheParams,
 // workaround for the coherence table component, they cannot be initialized outside
 // of a module
 coherenceTable: () => CoherenceTableGenerator[AutoEnum, AutoEnum, AutoEnum],
 llcCoherenceTable: () => LLCCoherenceTable[AutoEnum, AutoEnum],
 withCriticality: Boolean,
 outOfSlotResponse: Boolean,
 dataBusConf: () => DataBusConf = () => SharedEverything,
 translateGetS: Boolean = false, // useful only for pmesi
 withLoCritPWB: Boolean = false,
 getCritFromID: Int => UInt = _ => 0.U ,
 useAtomicBus: Boolean = false,
 useCARPArbiter: Boolean = false,
 useAtomicBusModified: Boolean = false,
 isConventionalProtocol: Boolean = false,
 conventionalSplit: Boolean = true// whether split PWB and PR
 // the following options are only useful when isConventionalProtocol is enabled
 /*,
 realTimeCores: Int,
 nonRealTimeCores: Int
 */
) {
  require(!useAtomicBusModified || useAtomicBusModified && useAtomicBus, "atomic modified must be enabled with atomic")
  if(isConventionalProtocol) {
    // assertions for using conventional protocol
    require(!useCARPArbiter)
    require(!useAtomicBusModified)
    require(!useAtomicBus)
  }
  // assert((realTimeCores + nonRealTimeCores) * 2 == masterCount, "Invalid configurations of RT and NRT cores")
  def getGenCacheReq: CacheReq = new CacheReq(addrWidth, dataWidth)
  def getGenCacheResp: CacheResp = new CacheResp(dataWidth)
  @deprecated
  def getGenMemReq: MemReq = new MemReq(addrWidth, cacheParams.lineWidth, busRequestType.getWidth, masterCount)
  def getGenMemReqCommand: MemReqCommand = new MemReqCommand(addrWidth, busRequestType.getWidth, masterCount)
  def getGenMemResp: MemResp = new MemResp(cacheParams.lineWidth)
  def getGenMemRespCommand: MemRespCommand = new MemRespCommand()
  def getGenSnoopReq: SnoopReq = new SnoopReq(addrWidth, busRequestType.getWidth, masterCount)
  def getGenSnoopResp: SnoopResp = new SnoopResp(cacheParams.lineWidth)
  def getGenDebugCacheline: DebugCacheline = new DebugCacheline(CohS, cacheParams)

  def getGenRequestorId: UInt = UInt(16.W)

  private def dramReqTypeWidth = 1
  def getGenDramReq: DRAMReq = new DRAMReq(addrWidth, cacheParams.lineWidth, dramReqTypeWidth)
  def getGenDramResp: DRAMResp = new DRAMResp(cacheParams.lineWidth, dramReqTypeWidth)
  def genCrit: () => UInt = () => UInt(4.W)
  def coreIdToCriticality(cacheId: UInt): UInt = {
    val crit = WireInit(0.U(3.W))
    when(cacheId === (masterCount - 1).U) {
      crit := 5.U
    }.otherwise {
      crit := 0.U
    }
    if(withCriticality) {
      crit
    } else {
      0.U
    }
  }
  def coreIdToCriticality(cacheId: Int): Int = {
    if(withCriticality) {
      if (cacheId == masterCount - 1) {
        5
      } else {
        0
      }
    } else {
      0
    }
  }
  def criticalityComparison(thisCrit: UInt, thatCrit: UInt): UInt = {
    val comparisonResults = Wire(UInt(CriticalityDiff.getWidth.W))
    when(thisCrit === thatCrit) {
      comparisonResults := CriticalityDiff.SAMECRIT.U
    }.elsewhen(thisCrit < thatCrit) {
      // e.g. 0 < 5, so thatCrit is low crit
      comparisonResults := CriticalityDiff.LOCRIT.U
    }.otherwise {
      assert(thisCrit > thatCrit)
      comparisonResults := CriticalityDiff.HICRIT.U
    }
    comparisonResults
  }
  def isLowCrit(crit: UInt): Bool = crit === 5.U
  def isLowCrit(crit: Int): Boolean = crit == 5
  def lowCritCoherenceTable = if(!withCriticality) {
    coherenceTable
  } else {
    () => new CARPNonCritCoherenceTable()
  }

  // we only support 1 pending request for now, further extension requires careful investigation
  def MSHRSize = 1
  def pendingMemSize = 1
  // these are for different bus arbitration
  def getDataBusConf: DataBusConf = dataBusConf() // DedicatedDataBusTwoWay
  def enableForwarding = false
}

