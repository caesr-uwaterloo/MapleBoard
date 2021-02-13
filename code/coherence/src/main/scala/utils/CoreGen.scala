
package utils


import coherences.{CARP, PMESI, PMSI}
import components.{AXI4ToMem, CARPCritCoherenceTable, FullSystem, PMESICoherenceTable, PMESILLCCoherenceTable, PMSICoherenceTable, PMSILLCCoherenceTable, RequestType}
import params.{MemorySystemParams, SimpleCacheParams}
import _root_.param.{CoreParam, RISCVParam}
import _root_.core.HeaderGen
import coherence.internal.AutoEnum
import utils.CoreGen.{args, getCacheDepth, getCohS, getCoreCount, getL1CoherenceTable, getLLCCoherenceTable, getMasterCount, getOptions, getRemainingOptions}

trait withCompilerOptionParser {
  implicit val margs: List[String] = List[String]()
  private def concatOptions[T <: Any](m: Map[Symbol, T], nxt: (Map[Symbol, Any], List[String])) = (m ++ nxt._1, nxt._2)
  private def concatOptions(m: List[String], nxt: (Map[Symbol, Any], List[String])) = (nxt._1, m ++ nxt._2)
  def getOptionsAndRemained(implicit margs: List[String]): (Map[Symbol, Any], List[String]) = margs match {
    case "-n" :: value :: tail => concatOptions(Map('nCores -> value.toInt), getOptionsAndRemained(tail))
    case "-cache-depth" :: value :: tail => concatOptions(Map('depth -> value.toInt), getOptionsAndRemained(tail))
    case "-protocol" :: value :: tail => concatOptions(Map('coh -> value), getOptionsAndRemained(tail))
    case "-slot-width" :: value :: tail => concatOptions(Map('sw -> value.toInt), getOptionsAndRemained(tail))
    case option :: value :: tail => concatOptions(List(option, value), getOptionsAndRemained(tail))
    case other :: tail => concatOptions(List(other), getOptionsAndRemained(tail))
    case Nil => (Map(), List())
    case _ => throw new RuntimeException("Unsupported Arguments.")
  }
  def getOptions(implicit margs: List[String]): Map[Symbol, Any] = getOptionsAndRemained._1
  def getRemainingOptions(implicit margs: List[String]): Array[String] = getOptionsAndRemained._2.toArray
  def getCoreCount(implicit margs: List[String]): Int = {
    val opt = getOptions
    opt.get('nCores) match {
      case Some(v) => v.asInstanceOf[Int]
      case None => 4
    }
  }

  def getProtocol(implicit margs: List[String]): String = {
    val opt = getOptions
    opt.get('coh) match {
      case Some(v) => v.asInstanceOf[String]
      case None => "PMESI"
    }
  }

  def getSlotWidth(implicit margs: List[String]): Int = {
    val opt = getOptions
    opt.get('sw) match {
      case Some(v) => v.asInstanceOf[Int]
      case None => 128
    }
  }

  def getCacheDepth(implicit margs: List[String]): Int = {
    val opt = getOptions
    opt.get('depth) match {
      case Some(v) => v.asInstanceOf[Int]
      case None => 8
    }
  }

  def getCohS(implicit margs: List[String]): AutoEnum = getProtocol match {
    case "PMESI" => new PMESI { }
    case "PMSI" => new PMSI { }
    case "CARP" => new CARP { }
    case _ => throw new IllegalArgumentException("Only support PMESI, PMSI and CARP now")
  }

  def getMasterCount(implicit margs: List[String]): Int = getCoreCount * 2

  def getL1CoherenceTable(implicit margs: List[String]) = getProtocol match {
    case "PMESI" => () => { new PMESICoherenceTable() }
    case "PMSI" => () => { new PMSICoherenceTable() }
    case "CARP" => () => { new CARPCritCoherenceTable() }
    case _ => throw new IllegalArgumentException("Only support PMESI, PMSI and CARP now")
  }

  def getLLCCoherenceTable(implicit margs: List[String]) = getProtocol match {
    case "PMESI" => () => { new PMESILLCCoherenceTable(getMasterCount) }
    case "PMSI" => () => { new PMSILLCCoherenceTable(getMasterCount) }
    case "CARP" => () => { new PMSILLCCoherenceTable(getMasterCount) }
    case _ => throw new IllegalArgumentException("Only support PMESI, PMSI and CARP now")
  }
}


// generate the core with memory and caches
object CoreGen extends App with withCompilerOptionParser {
  implicit override val margs: List[String] = args.toList
  println(getOptions)
  println(getRemainingOptions.mkString(" "))
  val nCore = getCoreCount
  val depth = getCacheDepth
  val lineSize = 64
  val addrWidth = 64
  val interfaceAddrWidth = 64
  val dataWidth = 64
  val slotWidth = 128
  val busDataWidth = 64
  val masterCount = getMasterCount
  val cacheParams = SimpleCacheParams(depth, 1, lineSize * 8, addrWidth)
  val memorySystemParams = MemorySystemParams(
    addrWidth = addrWidth,
    interfaceAddrWidth = interfaceAddrWidth,
    dataWidth = dataWidth,
    slotWidth = slotWidth,
    busDataWidth = busDataWidth,
    busRequestType = new RequestType {},
    masterCount = masterCount,
    CohS = getCohS /* new PMESI {} */,
    cacheParams = cacheParams,
    getL1CoherenceTable /* { new PMESICoherenceTable() } */,
    getLLCCoherenceTable /* { new PMESILLCCoherenceTable(masterCount) } */,
    withCriticality = false,
    outOfSlotResponse = false
  )
  val XLEN = 64
  val fetchWidth = 32
  val isaParam = new RISCVParam(XLEN = XLEN,
    Embedded = false,
    Atomic = true,
    Multiplication = false,
    Compressed = false,
    SingleFloatingPoint = false,
    DoubleFloatingPoint = false)

  val coreParam = new CoreParam(fetchWidth = fetchWidth,
    isaParam = isaParam,
    iCacheReqDepth = 1,
    iCacheRespDepth = 1,
    resetRegisterAddress =  0x80000000L,
    initPCRegisterAddress = 0x80003000L,
    baseAddrAddress = 0x80001000L,
    coreID = 0, // Note this is just a placeholder, internally, coreID will be adjusted
    withAXIMemoryInterface = true,
    nCore = nCore)
  chisel3.Driver.execute(getRemainingOptions, () => new CoreGroupAXIWithMemory(coreParam, memorySystemParams))

  HeaderGen.generateHeaders(getRemainingOptions, coreParam)
}


// generate the core with memory and caches
object CoreGenNoMem extends App with withCompilerOptionParser {

  implicit override val margs: List[String] = args.toList
  println(getOptions)
  println(getRemainingOptions.mkString(" "))
  val nCore = getCoreCount
  val depth = getCacheDepth
  val lineSize = 64
  val addrWidth = 64
  val interfaceAddrWidth = 64
  val dataWidth = 64
  val slotWidth = 128
  val busDataWidth = 64
  val masterCount = getMasterCount
  val cacheParams = SimpleCacheParams(depth, 1, lineSize * 8, addrWidth)
  val memorySystemParams = MemorySystemParams(
    addrWidth = addrWidth,
    interfaceAddrWidth = interfaceAddrWidth,
    dataWidth = dataWidth,
    slotWidth = slotWidth,
    busDataWidth = busDataWidth,
    busRequestType = new RequestType {},
    masterCount = masterCount,
    CohS = getCohS /* new PMESI {} */,
    cacheParams = cacheParams,
    getL1CoherenceTable /* { new PMESICoherenceTable() } */,
    getLLCCoherenceTable /* { new PMESILLCCoherenceTable(masterCount) } */,
    withCriticality = false,
    outOfSlotResponse = false
  )
  val XLEN = 64
  val fetchWidth = 32
  val isaParam = new RISCVParam(XLEN = XLEN,
    Embedded = false,
    Atomic = true,
    Multiplication = false,
    Compressed = false,
    SingleFloatingPoint = false,
    DoubleFloatingPoint = false)

  val coreParam = new CoreParam(fetchWidth = fetchWidth,
    isaParam = isaParam,
    iCacheReqDepth = 1,
    iCacheRespDepth = 1,
    resetRegisterAddress =  0x80000000L,
    initPCRegisterAddress = 0x80003000L,
    baseAddrAddress = 0x80001000L,
    coreID = 0, // Note this is just a placeholder, internally, coreID will be adjusted
    withAXIMemoryInterface = true,
    nCore = nCore)
  chisel3.Driver.execute(getRemainingOptions, () => new CoreGroupAXI(coreParam, memorySystemParams))

  HeaderGen.generateHeaders(getRemainingOptions, coreParam)
}
