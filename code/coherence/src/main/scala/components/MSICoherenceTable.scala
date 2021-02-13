package components

import coherences.PMSI

class MSICoherenceTable extends CoherenceTableGenerator(
  new CacheEvent { },
  new PMSI {},
  new RequestType { }
) {

  override def table: Map[Event, Do]  = Map(
    // From I
    Event(CE.LOAD,CohS.I) -> Do(CohS.IS_AD,"ADD_TO_REQ_QUEUE").markValid.broadcast(R.GETS),
    Event(CE.STORE,CohS.I) -> Do(CohS.IM_AD,"ADD_TO_REQ_QUEUE").markValid.broadcast(R.GETM),
    Event(CE.OWN_GETM,CohS.IM_AD) -> Do(CohS.IM_D, "UPDATE_CACHE"),
    Event(CE.OTHER_GETM,CohS.IM_D) -> Do(CohS.IM_DUI, "UPDATE_CACHE"),
    Event(CE.OTHER_GETS,CohS.IM_D) -> Do(CohS.IM_DUS, "UPDATE_CACHE"),
    Event(CE.OTHER_GETM,CohS.IM_DUS) -> Do(CohS.IM_DUI, "UPDATE_CACHE"),
    Event(CE.EDATA, CohS.IM_D) -> Do(CohS.M, "REFILL_CACHE").markDirty, // This should be changed
    Event(CE.DATA, CohS.IM_D) -> Do(CohS.M, "REFILL_CACHE").markDirty,
    Event(CE.DATA, CohS.IM_DUI) -> Do(CohS.IM_UI, "REFILL_CACHE"),
    Event(CE.DATA, CohS.IM_DUS) -> Do(CohS.IM_US, "REFILL_CACHE"),
    Event(CE.EDATA, CohS.IM_DUI) -> Do(CohS.IM_UI, "REFILL_CACHE"),
    Event(CE.EDATA, CohS.IM_DUS) -> Do(CohS.IM_US, "REFILL_CACHE"),

    Event(CE.STORE, CohS.IM_UI) -> Do(CohS.MI_A, "WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q").markDirty.broadcast(R.PUTM),
    Event(CE.STORE, CohS.IM_US) -> Do(CohS.MS_A, "WRITE_DATA_ARRAY_ADD_TO_CACHE_RESP_Q").markDirty.broadcast(R.PUTM),
    Event(CE.OWN_GETS, CohS.IS_AD) -> Do(CohS.IS_D, "UPDATE_CACHE"),
    Event(CE.OTHER_GETM, CohS.IS_D) -> Do(CohS.IS_DI, "UPDATE_CACHE"),
    Event(CE.OTHER_GETS, CohS.IS_D) -> Do(CohS.IS_D, "UPDATE_CACHE"),
    Event(CE.DATA, CohS.IS_D) -> Do(CohS.S, "REFILL_CACHE"),
    Event(CE.EDATA, CohS.IS_D) -> Do(CohS.S, "REFILL_CACHE"),
    Event(CE.DATA, CohS.IS_DI) -> Do(CohS.IS_UI, "REFILL_CACHE"),
    Event(CE.EDATA, CohS.IS_DI) -> Do(CohS.IS_UI, "REFILL_CACHE"),
    Event(CE.LOAD, CohS.IS_UI) -> Do(CohS.I, "ADD_TO_CACHE_RESP_QUEUE"),
    // From S
    Event(CE.LOAD, CohS.S) -> Do(CohS.S, "ADD_TO_CACHE_RESP_QUEUE"),
    Event(CE.REPLACEMENT, CohS.S) -> Do(CohS.I, "REMOVE_FROM_CACHE").reset,
    Event(CE.OTHER_GETM, CohS.S) -> Do(CohS.I, "UPDATE_CACHE").reset,
    Event(CE.OTHER_UPG, CohS.S) -> Do(CohS.I, "UPDATE_CACHE").reset,
    Event(CE.STORE, CohS.S) -> Do(CohS.SM_W, "ADD_TO_REQ_QUEUE").broadcast(R.UPG),
    Event(CE.OTHER_GETM, CohS.SM_W) -> Do(CohS.IM_W, "UPDATE_CACHE"),
    Event(CE.OTHER_UPG, CohS.SM_W) -> Do(CohS.IM_W, "UPDATE_CACHE"),
    Event(CE.OWN_UPG,    CohS.SM_W) -> Do(CohS.M,     "ADD_TO_CACHE_RESP_QUEUE_STORE").dirtify,
    Event(CE.OWN_UPG,    CohS.IM_W) -> Do(CohS.IM_AD, "ADD_TO_REQ_QUEUE").broadcast(R.GETM),
    // From M
    Event(CE.LOAD, CohS.M) -> Do(CohS.M, "ADD_TO_CACHE_RESP_QUEUE"),
    Event(CE.STORE, CohS.M) -> Do(CohS.M, "ADD_TO_CACHE_RESP_QUEUE_STORE"),
    Event(CE.REPLACEMENT, CohS.M) -> Do(CohS.MI_WB, "ADD_TO_WB_QUEUE").broadcast(R.PUTM),
    Event(CE.REPLACEMENT, CohS.M) -> Do(CohS.MI_WB, "ADD_TO_WB_QUEUE").broadcast(R.PUTM),
    Event(CE.OTHER_GETS, CohS.M) -> Do(CohS.MS_WB, "ADD_TO_WB_QUEUE").broadcast(R.PUTM),
    Event(CE.OTHER_GETM, CohS.M) -> Do(CohS.MI_WB, "ADD_TO_WB_QUEUE").broadcast(R.PUTM),

    Event(CE.REPLACEMENT, CohS.MS_WB) -> Do(CohS.MI_WB, "UPDATE_CACHE"),
    Event(CE.LOAD, CohS.MS_WB) -> Do(CohS.MS_WB, "ADD_TO_CACHE_RESP_QUEUE"),
    //*Event(CE.STORE, CohS.MS_WB) -> Do(CohS.MS_WB, "ADD_TO_CACHE_RESP_QUEUE_STORE"),
    Event(CE.OWN_PUTM, CohS.MS_WB) -> Do(CohS.S, "UPDATE_CACHE").clean,
    Event(CE.OWN_PUTM, CohS.MS_A) -> Do(CohS.S, "UPDATE_CACHE").clean,
    Event(CE.OTHER_GETM, CohS.MS_WB) -> Do(CohS.MI_WB, "UPDATE_CACHE"),
    Event(CE.OTHER_GETM, CohS.MS_A) -> Do(CohS.MI_A, "UPDATE_CACHE"),
    Event(CE.OWN_PUTM, CohS.MI_WB) -> Do(CohS.I, "REMOVE_FROM_CACHE").reset,
    Event(CE.OWN_PUTM, CohS.MI_A) -> Do(CohS.I, "REMOVE_FROM_CACHE").reset,
    // Impossible transitions
    Event(CE.OTHER_PUTM, CohS.MI_WB) -> Do(CohS.I, "IMPOSSIBLE"),
    Event(CE.OTHER_PUTM, CohS.MS_WB) -> Do(CohS.I, "IMPOSSIBLE"),
    Event(CE.OTHER_PUTM, CohS.M) -> Do(CohS.I, "IMPOSSIBLE"),
    Event(CE.OTHER_PUTM, CohS.MI_A) -> Do(CohS.I, "IMPOSSIBLE"),
  )
  override def getCoherenceWBStates: List[Int] = List(CohS.MS_A, CohS.MI_A, CohS.MI_WB,
    CohS.MS_WB)
}

