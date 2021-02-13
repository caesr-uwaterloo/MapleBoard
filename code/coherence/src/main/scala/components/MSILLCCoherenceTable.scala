
package components

import coherences.PMESILLC

class MSILLCCoherenceTable(masterCount: Int) extends LLCCoherenceTable(
  new RequestType { },
  new PMESILLC { },
  masterCount
) {
  // For MSI, it's only valid (xclusive) invalid
  override def table: Map[Event, Do]  = Map(
    // make them pending
    Event(CE.GETM, CohS.Xclusive) -> Do(CohS.Xclusive, LLCStates.LLC_PR_TABLE_WRITE_REQ),
    Event(CE.GETS, CohS.Xclusive) -> Do(CohS.Xclusive, LLCStates.LLC_PR_TABLE_WRITE_REQ),

    // Note: the sharers are now not accurate !!
    // PUTM that satisfy all reads: they are now just shared and can be removed simply
    Event(CE.PUTM, CohS.Xclusive, AllRead) -> Do(CohS.Invalid, LLCStates.LLC_HASH_REMOVE),
    Event(CE.PUTM, CohS.Xclusive, NoPendingReq) -> Do(CohS.Invalid, LLCStates.LLC_HASH_REMOVE),
    // In this case, the sharers are implicitly set to the last GETM...
    Event(CE.PUTM, CohS.Xclusive, OneWrite) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, SetSharers),

    // This is not possible
    // Event(CE.PUTS, CohS.Xclusive) -> Do(CohS.Xclusive, LLCStates.LLC_IDLE), // this is handled by the GetM transition
    // Event(CE.GETM, CohS.Shared) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    // Event(CE.GETS, CohS.Shared) -> Do(CohS.Shared, LLCStates.LLC_HASH_WRITE, AddSharer),
    // Event(CE.UPG, CohS.Shared) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    // Event(CE.PUTS, CohS.Shared, OneSharer) -> Do(CohS.Invalid, LLCStates.LLC_HASH_REMOVE),
    // Event(CE.PUTS, CohS.Shared, MoreThanOneSharer) -> Do(CohS.Shared, LLCStates.LLC_HASH_WRITE, RemoveSharer),

    // Simply do nothing about it...
    Event(CE.GETS, CohS.Invalid) -> Do(CohS.Invalid, LLCStates.LLC_IDLE, MarkOwner),
    Event(CE.GETM, CohS.Invalid) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner),
    Event(CE.UPG,  CohS.Invalid) -> Do(CohS.Xclusive, LLCStates.LLC_HASH_WRITE, MarkOwner)
  )
}
