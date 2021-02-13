
package params

import chisel3._
import chisel3.experimental.{EnumFactory, EnumType}
import components.{CoherenceQuery, CoherenceResponse, RequestType}
import chisel3.util.experimental.loadMemoryFromFile
import components.CoherenceResponse.CoherenceResponseShadow
import chisel3.util._
import java.io._

import _root_.utils.BundleLitAsBigIntHelper

trait CoherenceSpec[S <: Data, M <: Data, B <: Data] {

  // we define the corresponding component object here, direct translation to control signal is not supported...
  // These are wrapping functions for the CoherenceResponse whatever
  trait CoherenceDSLComponents {
    type Action = CoherenceResponseShadow[S, B] => CoherenceResponseShadow[S, B]
  }
  /* ---------- For Private Caches ------------ */
  object MSHR extends CoherenceDSLComponents {
    def insertCoreRequest(): Action = resp => resp.InsertMSHR()
    def cleanAndRespond(): Action = resp => resp.CleanMSHR()
  }
  object State extends CoherenceDSLComponents {
    def apply(s: S): Action = resp => resp.Goto(s)
  }
  object PR extends CoherenceDSLComponents {
    def insert(bdc: B): Action = resp => resp.Broadcast(bdc).BroadcastReq()
    def remove(): Action = resp => resp.RemovePendingMem()
    def update(dirty: Boolean = false): Action = { resp =>
      val res = resp.UpdatePendingMem()
      if(dirty) {
        res.MarkDirty()
      } else {
        res
      }
    }
    def cancelPRHead(): Action = resp => resp.CancelPRHead()
    def resend(): Action = resp => resp.PRResend()
  }
  object TAG extends CoherenceDSLComponents {
    def insert(dirty: Boolean = false): Action = { resp =>
      val res = resp.InsertTag()
      if(dirty) {
        res.MarkDirty()
      } else {
        res
      }
    }
    def update(dirty: Boolean = false, clean: Boolean = false): Action = { resp =>
      val res = resp.UpdateTag()
      assert(!(dirty && clean), "Dirty and clean cannot be asserted at the same time")
      if(dirty) {
        res.MarkDirty()
      } else if(clean) {
        res.MarkClean()
      } else  {
        res
      }
    }
    def remove(): Action = resp => resp.RemoveTag()
  }
  object DATA extends CoherenceDSLComponents {
    def update(): Action = resp => resp.UpdateData()
    def pushDataBus(): Action = resp => resp.PushDataBus()
  }
  object PWB extends CoherenceDSLComponents {
    def insert(bdc: B): Action = resp => resp.Broadcast(bdc).BroadcastWB()
  }
  object LoCritPWB extends CoherenceDSLComponents {
    def insert(bdc: B): Action = resp => resp.Broadcast(bdc).BroadcastLoCritWB()
    def cancelWB(): Action = resp => resp.CancelLoCritPWB()
  }
  object RB extends CoherenceDSLComponents { // Replay Buffer
    // note: replay buffer is used in the replacement case, because we want to generate the pending request
    // after it is broadcast (we don't have the space to hold the incoming line)
    def release(): Action = resp => resp.ReleaseReplay()
    def insert(): Action = resp => resp.InsertReplay()
  }
  object CR extends CoherenceDSLComponents {
    def respond(): Action = resp => resp.PushCacheResp()
    def markEData(): Action = resp => resp.MarkEData()
  }

  object DRAM extends CoherenceDSLComponents {
    def read(): Action = resp => resp.DRAMRead()
    def write(): Action = resp => resp.DRAMWrite()
  }
  object PRLUT extends CoherenceDSLComponents {
    def removeEntry(): Action = resp => resp.PRLUTRemove()
  }

  object ReplWB extends CoherenceDSLComponents {
    def migrate(): Action = resp => resp.MigrateFromRepl()
  }

  object Err {
    def apply(m: M, s: List[S]) = {
      for { st <- s } yield {
        CoherenceQuery(m, st) -> Act(Err.fire())
      }
    }
    def fire(): Action = resp => resp.Err()
  }

  /* ---------- For Shared Memory ------------ */

  def getGenState: S
  def getGenStateF: () => S
  def getGenMessage: M
  def getGenMessageF: () => M
  def getGenBusReqType: B
  def getGenBusReqTypeF: () => B
  def getPrivateCacheTable: Map[CoherenceQuery[M, S], CoherenceResponseShadow[S, B]]
  // This is like nothing
  def getLoCritPrivateCacheTable: Map[CoherenceQuery[M, S], CoherenceResponseShadow[S, B]] = Map()
  def getSharedCacheTable: Map[CoherenceQuery[B, S], CoherenceResponseShadow[S, B]]
  def getGenCohQuery: CoherenceQuery[M, S]
  def R(nextState: S): CoherenceResponseShadow[S, B] = CoherenceResponse(nextState, getGenBusReqType, getBFromInt, getSFromInt)
  type Action = CoherenceResponseShadow[S, B] => CoherenceResponseShadow[S, B]
  // scalastyle:off
  def Act(actions: Action*) = {
    val init = CoherenceResponse(getGenState, getGenBusReqType, getBFromInt, getSFromInt)
    actions.foldLeft(init) { (prev, act) =>
      act(prev)
    }
  }
  def GetM: B
  def GetS: B
  def Upg: B
  def PutM: B
  def PutS: B
  def GetSE: B
  def getBFromInt(x: Int): B
  def getSFromInt(x: Int): S
  def requestTypeToB(x: UInt): B = {
    val w = WireInit(GetS)
    when(x === RequestType.GETM.U) {
      w := GetM
    }.elsewhen (x === RequestType.GETS.U) {
      w := GetS
    }.elsewhen (x === (RequestType.PUTS + 1).U) {
      w := GetSE
    }.elsewhen (x === RequestType.UPG.U) {
      w := Upg
    }.elsewhen (x === RequestType.PUTM.U) {
      w := PutM
    }.elsewhen (x === RequestType.PUTS.U) {
      w := PutS
    }
    w
  }
  // todo: get table...
  def generatePrivateCacheTableSeq = {
    for { key <- getPrivateCacheTable } yield {
      (key._1.toBigInt, key._2.done.toBigInt)
    }
  }
  def generateLoCritPrivateCacheTableSeq = {
    for { key <- getLoCritPrivateCacheTable} yield {
      (key._1.toBigInt, key._2.done.toBigInt)
    }
  }
  def generatePrivateCacheTableFile(preferredName: Option[String]): String = {
    val name = preferredName match {
      case Some(v) => v
      case None => "coherence_table.mem"
    }
    // we generate files into the memory content
    val f = new File(name)
    val bw = new BufferedWriter(new FileWriter(f))
    for { key <- getPrivateCacheTable } {
      val query = key._1.toBigInt
      val resp = key._2.done.toBigInt
      bw.write(s"@${query.toString(16)}\n")
      bw.write(s"${resp.toString(16)}\n")
    }
    bw.close()
    name
  }
  def generateSharedCacheTableSeq = {
    for { key <- getSharedCacheTable} yield {
      (key._1.toBigInt, key._2.done.toBigInt)
    }
  }
  def generateSharedCacheTableFile(preferredName: Option[String]): String = {
    val name = preferredName match {
      case Some(v) => v
      case None => "coherence_table_shared.mem"
    }
    // we generate files into the memory content
    val f = new File(name)
    val bw = new BufferedWriter(new FileWriter(f))
    for { key <- getSharedCacheTable } {
      val query = key._1.toBigInt
      val resp = key._2.done.toBigInt
      bw.write(s"@${query.toString(16)}\n")
      bw.write(s"${resp.toString(16)}\n")
    }
    bw.close()
    name
  }
}

object CoherenceSpec {
  class CoherenceIO[S <: Data, M <: Data, B <: Data]
  (
    private val genS: () => S,
    private val genM: () => M,
    private val genB: () => B
  ) extends Bundle {
    val enable = Input(Bool())
    val query = Input(new CoherenceQuery(genM, genS))
    val resp = Output(new CoherenceResponse(genS, genB))
    override def cloneType: CoherenceIO.this.type = new CoherenceIO(genS, genM, genB).asInstanceOf[this.type]
  }
  class MemoryControllerCoherenceIO[S <: Data, M <: Data, B <: Data]
  (
    private val genS: () => S,
    private val genM: () => M,
    private val genB: () => B
  ) extends Bundle {
    val enable = Input(Bool())
    val query = Input(new CoherenceQuery(genB, genS))
    val resp = Output(new CoherenceResponse(genS, genB))
    override def cloneType: MemoryControllerCoherenceIO.this.type = new CoherenceIO(genS, genM, genB).asInstanceOf[this.type]
  }
  class PrivateCoherenceTable[S <: Data, M <: Data, B <: Data](val coherenceSpec: CoherenceSpec[S, M, B],
                                                               val locrit: Boolean = false)
    extends Module {
    val genS = coherenceSpec.getGenStateF
    val genM = coherenceSpec.getGenMessageF
    val genB = coherenceSpec.getGenBusReqTypeF
    val io = IO(new CoherenceIO(genS, genM, genB))
    // expand the table into a read only memory or BRAM
    val genCohResp = new CoherenceResponse(genS, genB)
    // val rdData = Wire(UInt(genCohResp.getWidth.W))
    val rdData = RegInit(0.U(genCohResp.getWidth.W))
    val memDepth = 4096
    // val coherenceMemory = SyncReadMem(memDepth, UInt(genCohResp.getWidth.W))

    // rdData := coherenceMemory.read(io.query.asUInt, io.enable)
    io.resp := rdData.asTypeOf(genCohResp)
    // load memory content
    // loadMemoryFromFile(coherenceMemory, "coherence_table.mem")
    // printf("===== [Private Coherence Table] =====\n")
    // printf(" en: %b, query: %d, resp: %x\n", io.enable, io.query.asUInt, io.resp.asUInt)
    when(io.enable) {
      val cohtable = if(!locrit) {
        // locrit false
        coherenceSpec.generatePrivateCacheTableSeq
      } else {
        // locrit true
        coherenceSpec.generateLoCritPrivateCacheTableSeq
      }
      cohtable.foldLeft(when(false.B) {}) { (prev, next) =>
        prev.elsewhen(io.query.asUInt === next._1.U) {
          rdData := next._2.U
        }
      }.otherwise {
        rdData := 0.U
        println("Nothing Match")
        println(p"${io.query}\n")
      }
    }
    dontTouch(io)
    dontTouch(io.query)

  }
  class MemoryControllerCoherenceTable[S <: Data, M <: Data, B <: Data](val coherenceSpec: CoherenceSpec[S, M, B])
    extends Module {
    val genS = coherenceSpec.getGenStateF
    val genM = coherenceSpec.getGenMessageF
    val genB = coherenceSpec.getGenBusReqTypeF
    val io = IO(new CoherenceIO(genS, genB, genB))
    // expand the table into a read only memory or BRAM
    val genCohResp = new CoherenceResponse(genS, genB)
    // val rdData = Wire(UInt(genCohResp.getWidth.W))
    val rdData = RegInit(0.U(genCohResp.getWidth.W))
    val memDepth = 4096
    // val coherenceMemory = SyncReadMem(memDepth, UInt(genCohResp.getWidth.W))

    // rdData := coherenceMemory.read(io.query.asUInt, io.enable)
    io.resp := rdData.asTypeOf(genCohResp)
    dontTouch(io)
    dontTouch(io.query)
    // load memory content
    // loadMemoryFromFile(coherenceMemory, "coherence_table_shared.mem")
    // printf("===== [Private Coherence Table] =====\n")
    // printf(" en: %b, query: %d, resp: %x\n", io.enable, io.query.asUInt, io.resp.asUInt)
    when(io.enable) {
      coherenceSpec.generateSharedCacheTableSeq.foldLeft(when(false.B) {}) { (prev, next) =>
        prev.elsewhen(io.query.asUInt === next._1.U) {
          rdData := next._2.U
        }
      }.otherwise {
        rdData := 0.U
      }
    }
  }
  def translateToPrivateModule[S <: Data, M <: Data, B <: Data](coherenceSpec: CoherenceSpec[S, M, B]):
  () => PrivateCoherenceTable[S, M, B] = {
    () => new PrivateCoherenceTable(coherenceSpec)
  }
  def translateToLoCritPrivateModule[S <: Data, M <: Data, B <: Data](coherenceSpec: CoherenceSpec[S, M, B]):
  () => PrivateCoherenceTable[S, M, B] = {
    () => new PrivateCoherenceTable(coherenceSpec, locrit = true)
  }
  def translateToSharedModule[S <: Data, M <: Data, B <: Data](coherenceSpec: CoherenceSpec[S, M, B]):
  () => MemoryControllerCoherenceTable[S, M, B] = {
    () => new MemoryControllerCoherenceTable(coherenceSpec)
  }
}
