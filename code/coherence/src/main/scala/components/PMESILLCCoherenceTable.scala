
package components

import coherences.PMESILLC
import params.MemorySystemParams

class PMESILLCCoherenceTable(masterCount: Int) extends LLCCoherenceTable(
  new RequestType { },
  new PMESILLC { },
  masterCount
) {
  override def table: Map[Event, Do]  = Map(
    Event(CE.GETM, CohS.Xclusive) -> Do(CohS.Xclusive, LLCStates.LLC_PR_TABLE_WRITE_REQ),
    Event(CE.GETS, CohS.Xclusive) -> Do(CohS.Xclusive, LLCStates.LLC_PR_TABLE_WRITE_REQ),
    Event(CE.PUTM, CohS.Xclusive, AllRead) -> Do(CohS.Shared, LLCStates.LLC_HASH_WRITE, AppendSharers),
    Event(CE.PUTM, CohS.Xclusive, NoPendingReq) -> Do(CohS.Invalid, LLCStates.LLC_HASH_REMOVE),
    // In this case, the sharers are implicitly set to the last GETM...
    Event(CE.PUTM, CohS.Xclusive, OneWrite) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, SetSharers),
    Event(CE.PUTS, CohS.Xclusive) -> Do(CohS.Xclusive, LLCStates.LLC_IDLE), // this is handled by the GetM transition
    Event(CE.GETM, CohS.Shared) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    Event(CE.GETS, CohS.Shared) -> Do(CohS.Shared, LLCStates.LLC_HASH_WRITE, AddSharer),
    Event(CE.UPG, CohS.Shared) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    Event(CE.PUTS, CohS.Shared, OneSharer) -> Do(CohS.Invalid, LLCStates.LLC_HASH_REMOVE),
    Event(CE.PUTS, CohS.Shared, MoreThanOneSharer) -> Do(CohS.Shared, LLCStates.LLC_HASH_WRITE, RemoveSharer),
    Event(CE.GETS, CohS.Invalid) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    Event(CE.GETM, CohS.Invalid) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    // Simply Ignore as we did in the previous case
    Event(CE.UPG,  CohS.Invalid) -> Do(CohS.Xclusive, LLCStates.LLC_IDLE)
  )
}
