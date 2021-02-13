
package components

import chisel3._
import chisel3.experimental._
import chisel3.experimental.BundleLiterals._
import params.CoherenceSpec

class CoherenceResponse[S <: Data, B <: Data](
                                             private val genState: () => S,
                                             private val genBroadcast: () => B
                       ) extends Bundle  {
  val nextState = genState()
  val broadcastReq = Bool()
  val broadcastWB = Bool()
  val broadcastLoCritWB = Bool()
  val cancelLoCritPWB = Bool()
  val broadcast = genBroadcast()
  val updateData = Bool() // whether update the data cache
  val insertTag = Bool() // whether insert the tag
  val pushCacheResp = Bool()
  val markDirty = Bool()
  val markClean = Bool()
  val removeTag = Bool()
  val removePendingMem = Bool()
  val updatePendingMem = Bool()
  val cleanMSHR = Bool()
  val insertMSHR = Bool()
  val updateTag = Bool()
  val insertReplay = Bool()
  val releaseReplay = Bool()
  val pushDataBus = Bool()
  val dramRead = Bool()
  val dramWrite = Bool()
  val prlutRemove = Bool()
  val cancelPRHead = Bool()
  val isEData = Bool()
  val prResend = Bool()
  val migrateFromRepl = Bool()
  val isErr = Bool()
  val defined = Bool()

  override def cloneType: CoherenceResponse.this.type = new CoherenceResponse(genState, genBroadcast).asInstanceOf[this.type]
}

object CoherenceResponse {
  case class CoherenceResponseShadow[T <: Data, B <: Data](
                                    nextState: T,
                                    broadcast: B,
                                    broadcastReq: Boolean = false,
                                    broadcastWB: Boolean = false,
                                    broadcastLoCritWB: Boolean  = false,
                                    cancelLoCritPWB: Boolean = false,
                                    updateData: Boolean = false,
                                    insertTag: Boolean = false,
                                    pushCacheResp: Boolean = false,
                                    markDirty: Boolean = false,
                                    markClean: Boolean = false,
                                    removeTag: Boolean = false,
                                    removePendingMem: Boolean = false,
                                    updatePendingMem: Boolean = false,
                                    cleanMSHR: Boolean = false,
                                    insertMSHR: Boolean = false,
                                    updateTag: Boolean = false,
                                    insertReplay: Boolean = false,
                                    releaseReplay: Boolean = false,
                                    defined: Boolean = false,
                                    pushDataBus: Boolean = false,
                                    isEData: Boolean = false,
                                    prResend: Boolean = false,
                                    isErr: Boolean = false,
                                    dramRead: Boolean = false,
                                    dramWrite: Boolean = false,
                                    prlutRemove: Boolean = false,
                                    cancelPRHead: Boolean = false,
                                    migrateFromRepl: Boolean = false,
                                    broadcastObject: Int => B,
                                    stateObject: Int => T
                                    ) {
    def done: CoherenceResponse[T, B] = {
      val nxtState =  nextState.isLit() match {
        case true => nextState
        case false => stateObject(0) // defaults to be 0
      }
      val broadcastValue = broadcast.isLit() match {
        case true => broadcast
        case false => broadcastObject(0)
      }

      new CoherenceResponse(() => nextState.cloneType, () => broadcast.cloneType).Lit(
        _.nextState -> nxtState,
        _.broadcastReq -> broadcastReq.B,
        _.broadcastLoCritWB -> broadcastLoCritWB.B,
        _.cancelLoCritPWB -> cancelLoCritPWB.B,
        _.broadcastWB -> broadcastWB.B,
        _.broadcast -> broadcastValue,
        _.updateData -> updateData.B,
        _.insertTag -> insertTag.B,
        _.pushCacheResp -> pushCacheResp.B,
        _.markDirty -> markDirty.B,
        _.markClean-> markClean.B,
        _.removeTag -> removeTag.B,
        _.removePendingMem -> removePendingMem.B,
        _.updatePendingMem -> updatePendingMem.B,
        _.cleanMSHR -> cleanMSHR.B,
        _.insertMSHR -> insertMSHR.B,
        _.updateTag -> updateTag.B,
        _.insertReplay -> insertReplay.B,
        _.releaseReplay -> releaseReplay.B,
        _.pushDataBus -> pushDataBus.B,
        _.dramRead -> dramRead.B,
        _.dramWrite -> dramWrite.B,
        _.prlutRemove -> prlutRemove.B,
        _.cancelPRHead -> cancelPRHead.B,
        _.isEData -> isEData.B,
        _.prResend -> prResend.B,
        _.migrateFromRepl -> migrateFromRepl.B,
        _.isErr -> isErr.B,
        _.defined -> true.B
      )
    }

    type CR = this.type

    def Broadcast(broadcast: B) = this.copy(broadcast = broadcast)
    def BroadcastWB(broadcastWB: Boolean = true) = this.copy(broadcastWB = broadcastWB)
    def BroadcastLoCritWB(broadcastLoCritWB: Boolean = true) = this.copy(broadcastLoCritWB = broadcastLoCritWB)
    def CancelLoCritPWB(cancelLoCritPWB: Boolean = true) = this.copy(cancelLoCritPWB = cancelLoCritPWB)
    def PRResend(prResend: Boolean = true) = this.copy(prResend = prResend)
    def BroadcastReq(broadcastReq: Boolean = true) = this.copy(broadcastReq = broadcastReq)
    def InsertTag(insertTag: Boolean = true) = this.copy(insertTag = insertTag)
    def PushCacheResp(pushCacheResp: Boolean = true) = this.copy(pushCacheResp = pushCacheResp)
    def MarkDirty(markDirty: Boolean = true) = this.copy(markDirty = markDirty)
    def MarkClean(markClean: Boolean = true) = this.copy(markClean = markClean)
    def RemoveTag(removeTag: Boolean = true) = this.copy(removeTag = removeTag)
    def CleanMSHR(cleanMSHR: Boolean = true) = this.copy(cleanMSHR = cleanMSHR)
    def RemovePendingMem(removePendingMem: Boolean = true) = this.copy(removePendingMem = removePendingMem)
    def InsertMSHR(insertMSHR: Boolean = true) = this.copy(insertMSHR = insertMSHR)
    def UpdateData(updateData: Boolean = true) = this.copy(updateData = updateData)
    def UpdateTag(updateTag: Boolean = true) = this.copy(updateTag = updateTag)
    def InsertReplay(insertReplay: Boolean = true) = this.copy(insertReplay = insertReplay)
    def ReleaseReplay(releaseReplay: Boolean = true) = this.copy(releaseReplay = releaseReplay)
    def UpdatePendingMem(updatePendingMem: Boolean = true) = this.copy(updatePendingMem = updatePendingMem)
    def Goto(state: T) = this.copy(nextState = state)
    def PushDataBus(pushDataBus: Boolean = true) = this.copy(pushDataBus = pushDataBus)
    def DRAMRead(dramRead: Boolean = true) = this.copy(dramRead = dramRead)
    def DRAMWrite(dramWrite: Boolean = true) = this.copy(dramWrite = dramWrite)
    def PRLUTRemove(prlutRemove: Boolean = true) = this.copy(prlutRemove = prlutRemove)
    def CancelPRHead(cancelPRHead: Boolean = true) = this.copy(cancelPRHead = cancelPRHead)
    def MarkEData(isEData: Boolean = true) = this.copy(isEData = isEData)
    def MigrateFromRepl(migrateFromRepl: Boolean = true) = this.copy(migrateFromRepl = migrateFromRepl)
    // maybe we can replace it with more descriptive information
    def Err(err: Boolean = true) = this.copy(isErr = err)
  }

  def apply[S <: Data, B <: Data](dataS: S, dataB: B, bdF: Int => B, stF: Int => S): CoherenceResponseShadow[S, B] = {
    CoherenceResponseShadow(dataS, dataB, broadcastObject = bdF, stateObject = stF)
  }
}
