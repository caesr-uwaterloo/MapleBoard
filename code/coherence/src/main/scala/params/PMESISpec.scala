
package params

import chisel3._
import coherences.{CoherenceMessage, PMESIStates}
import components.CoherenceResponse.CoherenceResponseShadow
import components.{BusRequestType, CoherenceQuery}

// scalastyle:off
class PMESISpec extends CoherenceSpec[PMESIStates.Type, CoherenceMessage.Type, BusRequestType.Type] {
  override def getGenState: PMESIStates.Type = PMESIStates()
  override def getGenMessage: CoherenceMessage.Type = CoherenceMessage()
  override def getGenBusReqType: BusRequestType.Type = BusRequestType()

  override def getGenStateF = () => PMESIStates()
  override def getGenMessageF = () => CoherenceMessage()
  override def getGenBusReqTypeF = () => BusRequestType()
  override def getGenCohQuery: CoherenceQuery[CoherenceMessage.Type, PMESIStates.Type] = new CoherenceQuery(getGenMessageF, getGenStateF)
  import CoherenceMessage._
  import PMESIStates._
  private def e = CoherenceQuery
  private def f = CoherenceQuery
  override def getPrivateCacheTable: Map[CoherenceQuery[CoherenceMessage.Type, PMESIStates.Type],
    CoherenceResponseShadow[PMESIStates.Type, BusRequestType.Type]] =
    Map(
      e(Load, I) -> Act(State(IS_AD), MSHR.insertCoreRequest(), PR.insert(BusRequestType.GetS)),
      e(Store, I) -> Act(State(IM_AD), MSHR.insertCoreRequest(), PR.insert(BusRequestType.GetM)),
      e(OwnGetM, IM_AD) -> Act(State(IM_D), PR.update()),
      e(OtherGetM, IM_D) -> Act(State(IM_DI), PR.update()),
      e(OtherGetS, IM_D) -> Act(State(IM_DS), PR.update()),
      e(OtherGetM, IM_DS) -> Act(State(IM_DI), PR.update()),
      e(Data,  IM_D) -> Act(State(M), MSHR.cleanAndRespond(), TAG.insert(dirty=true), PR.remove(), DATA.update()),
      e(EData, IM_D) -> Act(State(M), MSHR.cleanAndRespond(), TAG.insert(dirty=true), PR.remove(), DATA.update()),
      // conceptually, these are still M
      e(Data, IM_DI) -> Act(State(MI_A), MSHR.cleanAndRespond(), DATA.update(),
        PWB.insert(PutM), TAG.insert(dirty=true), PR.remove()), // Note: we need the what ever know what data to push
      e(EData, IM_DI) -> Act(State(MI_A), MSHR.cleanAndRespond(), DATA.update(),
        PWB.insert(PutM), TAG.insert(dirty=true), PR.remove()),
      e(Data, IM_DS) -> Act(State(MS_A), MSHR.cleanAndRespond(), DATA.update(),
        PWB.insert(PutM), TAG.insert(dirty=true), PR.remove()), // Note: we need the what ever know what data to push
      e(EData, IM_DS) -> Act(State(MS_A), MSHR.cleanAndRespond(), DATA.update(),
        PWB.insert(PutM), TAG.insert(dirty=true), PR.remove()),

      // e(Store, E) -> R(M).PushCacheResp().UpdateData().MarkDirty().UpdateTag(),
      e(Store, E) -> Act(State(M), CR.respond(), TAG.update(dirty=true), DATA.update()),
      e(Load, E) -> R(E).PushCacheResp(),

      e(Load, M) -> R(M).PushCacheResp(),
      e(Store, M) -> R(M).PushCacheResp().UpdateData(),
      // e(Load, I) -> R(IS_AD).BroadcastReq().Broadcast(GetS).InsertMSHR(),
      e(OwnGetS, IS_AD) -> Act(State(IS_D), PR.update()),
      e(OtherGetM, IS_D) -> Act(State(IS_DI), PR.update()),
      e(OtherUpg, IS_D) -> Act(State(IS_DI), PR.update()),
      // e(OtherGetS, IS_AD) -> Act(State(IS_AD))  do nothing
      e(Data, IS_D) -> Act(State(S), DATA.update(), TAG.insert(), MSHR.cleanAndRespond(), PR.remove()),
      e(EData, IS_D) -> Act(State(E), DATA.update(), TAG.insert(), MSHR.cleanAndRespond(), PR.remove()),
      e(Data, IS_DI) -> Act(State(I), MSHR.cleanAndRespond(), PR.remove()),
      e(EData, IS_DI) -> Act(State(EI_A), MSHR.cleanAndRespond(), DATA.update(),
        PWB.insert(PutM), TAG.insert(dirty=false), PR.remove()),

      e(Load, S) -> Act(CR.respond()),
      e(Replacement, S) -> Act(State(I), TAG.remove(), RB.insert(), RB.release()), // silent eviction
      e(OtherGetM, S) -> Act(State(I), TAG.remove()), // silent eviction
      e(OtherUpg, S) -> Act(State(I), TAG.remove()), // silent eviction
      // There could be optimization here: (Load, SM_W) can be hit, but that requires extra logic/transition signals...
      // So we don't do that here
      // in tag
      e(Store, S) -> Act(State(SM_W_A), PR.insert(Upg), MSHR.insertCoreRequest(), TAG.update()), // not going through the MSHR path
      // TODO: on changing of this part, the shared memory should be able to track accutately what is happening
      // Can add a new action: PR.cancelLastUpg() and dequeue that message
      // =====
      // e(OtherGetM, SM_W) -> Act(State(IM_W), TAG.update()),
      // e(OtherUpg, SM_W) -> Act(State(IM_W), TAG.update()),
      // e(OwnUpg, IM_W) -> Act(State(IM_AD), TAG.remove(), PR.insert(GetM)), // retry..
      // =====
      // prevent the request from issuing
      e(OtherUpg, SM_W_A) -> Act(State(IM_AD), TAG.remove(), PR.insert(GetM), PR.cancelPRHead()),
      e(OtherGetM, SM_W_A) -> Act(State(IM_AD), TAG.remove(), PR.insert(GetM), PR.cancelPRHead()),
      e(OwnUpg, SM_W_A) -> Act(State(SM_W), TAG.update()),
      e(Ack, SM_W) -> Act(State(M), MSHR.cleanAndRespond(), TAG.update(dirty=true), DATA.update()),
      e(OtherGetS, SM_W) -> Act(State(SM_WS), TAG.update()),
      e(OtherGetM, SM_W) -> Act(State(SM_WI), TAG.update()),
      e(OtherGetM, SM_WS) -> Act(State(SM_WI), TAG.update()),
      e(Ack, SM_WS) -> Act(State(MS_A), MSHR.cleanAndRespond(), PWB.insert(PutM), TAG.update(dirty=true), DATA.update()),
      e(Ack, SM_WI) -> Act(State(MI_A), MSHR.cleanAndRespond(), PWB.insert(PutM), TAG.update(dirty=true), DATA.update()),
      // This is because other transitions might be releasing the buffer
      // Migrating from TAG to PR
      // e(OwnUpg, IM_W) -> Act(TAG.remove(), RB.release()), // retry..
      e(Replacement, M) -> Act(State(MI_A), TAG.update(), PWB.insert(PutM), RB.insert()),
      e(Replacement, MI_A) -> Act(RB.insert()),

      e(Replacement, E) -> Act(State(EI_A), TAG.update(), PWB.insert(PutM), RB.insert()),
      e(Replacement, EI_A) -> Act(RB.insert()),

      e(OtherGetS, M) -> Act(State(MS_A), TAG.update(), PWB.insert(PutM)),
      e(OtherGetS, E) -> Act(State(ES_A), TAG.update(), PWB.insert(PutM)),

      e(OtherGetM, M) -> Act(State(MI_A), TAG.update(), PWB.insert(PutM)),
      e(OtherGetM, E) -> Act(State(EI_A), TAG.update(), PWB.insert(PutM)),

      e(OtherGetM, MS_A) -> Act(State(MI_A), TAG.update(), ReplWB.migrate()),
      e(OtherGetM, ES_A) -> Act(State(EI_A), TAG.update(), ReplWB.migrate()),

      // Both core in Shared state, both get to SM_W and one get upgraded first
      e(OtherUpg, M) -> Act(State(MI_A), TAG.update(), PWB.insert(PutM)),
      e(OtherUpg, E) -> Act(Err.fire()),

      e(Replacement, MS_A) -> Act(State(MI_A), TAG.update(), RB.insert()),
      e(Replacement, ES_A) -> Act(State(EI_A), TAG.update(), RB.insert()),

      e(Load, MS_A) -> Act(State(MS_A), CR.respond()),
      e(Load, ES_A) -> Act(State(ES_A), CR.respond()),

      e(Load, MI_A) -> Act(State(MI_A), CR.respond()),
      e(Load, MS_A) -> Act(State(MS_A), CR.respond()),
      e(Store, MI_A) -> Act(State(MI_A), CR.respond(), DATA.update()),
      e(Store, MS_A) -> Act(State(MS_A), CR.respond(), DATA.update()),

      e(Load, EI_A) -> Act(State(MI_A), CR.respond()),
      e(Load, ES_A) -> Act(State(MS_A), CR.respond()),
      e(Store, EI_A) -> Act(State(MI_A), CR.respond(), DATA.update(), TAG.update(dirty = true)),
      e(Store, ES_A) -> Act(State(MS_A), CR.respond(), DATA.update(), TAG.update(dirty = true)),
      // replay requests if any due to WB, we need a label saying that...whether it is triggered by replacemnet
      e(OwnPutM, MI_A) -> Act(State(I), TAG.remove(), DATA.pushDataBus(), RB.release()),
      e(OwnPutM, MS_A) -> Act(State(S), TAG.update(clean = true), DATA.pushDataBus()),

      e(OwnPutM, EI_A) -> Act(State(I), TAG.remove(), RB.release()),
      e(OwnPutM, ES_A) -> Act(State(S), TAG.update(clean = true)),

      // For replacement buffer migration
      e(OtherGetM, MI_A) -> Act(ReplWB.migrate()),
      e(OtherGetM, EI_A) -> Act(ReplWB.migrate()),
      e(OtherGetS, MI_A) -> Act(ReplWB.migrate()),
      e(OtherGetS, EI_A) -> Act(ReplWB.migrate()),
      e(OtherGetS, MS_A) -> Act(ReplWB.migrate()),
      e(OtherGetS, ES_A) -> Act(ReplWB.migrate()),
    ).++(Map(Err(OwnPutM, PMESIStates.I_): _*)).++(Map(Err(OtherPutM, PMESIStates.M_): _*)).++(
      Map(Err(OwnUpg, PMESIStates.M_): _*)
    ).++(Map(Err(Store, List(PMESIStates.SM_W)): _*))

  override def getSharedCacheTable: Map[CoherenceQuery[BusRequestType.Type, PMESIStates.Type],
    CoherenceResponseShadow[PMESIStates.Type, BusRequestType.Type]] = Map(
    f(GetS, I) -> Act(DRAM.read(), CR.respond(), PRLUT.removeEntry()),
    f(GetSE, I) -> Act(State(M), TAG.insert(dirty = true), DRAM.read(), CR.respond(), PRLUT.removeEntry(), CR.markEData()),

    f(GetM, I) -> Act(State(M), TAG.insert(dirty = true), DRAM.read(), CR.respond(), PRLUT.removeEntry()),
    f(Upg, I) -> Act(State(M), TAG.insert(dirty = true), PRLUT.removeEntry(), CR.respond()),
    f(PutM, M) -> Act(State(I), TAG.remove(), DRAM.write()),
    f(PutM, MI_A) -> Act(State(I), TAG.remove(), DRAM.write()),
    f(GetS, M) -> Act(State(MI_A), TAG.update()),
    f(GetM, M) -> Act(State(MI_A), TAG.update()),
    // f(CacheUpg, M) -> Act(), // simply ignore, maybe a result of two cores upgrading
    f(Upg, M) -> Act(Err.fire()), // Simply ignore, but also need to remove the Upg
    f(PutM, I) -> Act(Err.fire())
  )

  override def GetM = BusRequestType.GetM
  override def GetS = BusRequestType.GetS
  override def GetSE = BusRequestType.GetSE
  override def Upg = BusRequestType.Upg
  override def PutM = BusRequestType.PutM
  override def PutS = BusRequestType.PutS

  override def getBFromInt(x: Int): BusRequestType.Type = BusRequestType(x.U)
  override def getSFromInt(x: Int): PMESIStates.Type = PMESIStates(x.U)
}

object PMESISpec {
  def apply(): PMESISpec = new PMESISpec()
}

