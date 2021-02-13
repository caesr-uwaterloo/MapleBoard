import coherences.{CARP, PMESI, PMSI}
import components.{CARPCritCoherenceTable, DataBusConf, DedicatedDataBusOneWay, DedicatedDataBusTwoWay, MESICoherenceTable, MESILLCCoherenceTable, MSICoherenceTable, MSILLCCoherenceTable, PMESICoherenceTable, PMESILLCCoherenceTable, PMSICoherenceTable, PMSILLCCoherenceTable, RequestType}
import param.{CoreParam, RISCVParam}
import params.{CARPSpec, MemorySystemParams, PMESISpec, PMSISpec, SimpleCacheParams}
import chisel3._

package object fixtures {

  object TargetPlatformPMESI {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMESI {},
      cacheParams = cacheParams,
      () => { new PMESICoherenceTable() },
      () => { new PMESILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      translateGetS = true
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

    val coherenceSpec = PMESISpec()
  }
  object TargetPlatformPMESIAtomic {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMESI {},
      cacheParams = cacheParams,
      () => { new PMESICoherenceTable() },
      () => { new PMESILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      translateGetS = true,
      useAtomicBus = true
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

    val coherenceSpec = PMESISpec()
  }
  object TargetPlatformPMESIAtomicModified {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMESI {},
      cacheParams = cacheParams,
      () => { new PMESICoherenceTable() },
      () => { new PMESILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      translateGetS = true,
      useAtomicBus = true,
      useAtomicBusModified = true
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

    val coherenceSpec = PMESISpec()
  }
  object TargetPlatformPMESIDedicated {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 256
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMESI {},
      cacheParams = cacheParams,
      () => { new PMESICoherenceTable() },
      () => { new PMESILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      dataBusConf = () => DedicatedDataBusTwoWay,
      translateGetS = true
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

    val coherenceSpec = PMESISpec()
  }
  object TargetPlatformPMESIDedicatedOneWay {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 256
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMESI {},
      cacheParams = cacheParams,
      () => { new PMESICoherenceTable() },
      () => { new PMESILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      dataBusConf = () => DedicatedDataBusOneWay,
      translateGetS = true
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

    val coherenceSpec = PMESISpec()
  }

  object TargetPlatformPMSI {
    val nCore = 4
    val depth = 16
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMSI {},
      cacheParams = cacheParams,
      () => { new PMSICoherenceTable() },
      () => { new PMSILLCCoherenceTable(masterCount) },
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

    val coherenceSpec = PMSISpec()
  }
  object TargetPlatformPMSIAtomicModified {
    val nCore = 4
    val depth = 16
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMSI {},
      cacheParams = cacheParams,
      () => { new PMSICoherenceTable() },
      () => { new PMSILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      useAtomicBus = true,
      useAtomicBusModified = true
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

    val coherenceSpec = PMSISpec()
  }
  object TargetPlatformPMSIDedicated {
    val nCore = 4
    val depth = 16
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = new MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMSI {},
      cacheParams = cacheParams,
      () => { new PMSICoherenceTable() },
      () => { new PMSILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      dataBusConf = () => DedicatedDataBusTwoWay
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

    val coherenceSpec = PMSISpec()
  }
  object TargetPlatformPMSIDedicatedOneWay {
    val nCore = 4
    val depth = 16
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = new MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMSI {},
      cacheParams = cacheParams,
      () => { new PMSICoherenceTable() },
      () => { new PMSILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      dataBusConf = () => DedicatedDataBusOneWay
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

    val coherenceSpec = PMSISpec()
  }

  object TargetPlatformCARP {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new CARP {},
      cacheParams = cacheParams,
      () => { new CARPCritCoherenceTable() },
      () => { new PMSILLCCoherenceTable(masterCount) },
      withCriticality = true,
      outOfSlotResponse = false,
      getCritFromID = i => if (i == masterCount - 1) { 5.U } else { 0.U },
      withLoCritPWB = true
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
    val coherenceSpec = CARPSpec()
  }
  object TargetPlatformCARPArbiter {
    val nCore = 4
    val depth = 8
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new CARP {},
      cacheParams = cacheParams,
      () => { new CARPCritCoherenceTable() },
      () => { new PMSILLCCoherenceTable(masterCount) },
      withCriticality = true,
      outOfSlotResponse = false,
      getCritFromID = i => if (i == masterCount - 1) { 5.U } else { 0.U },
      withLoCritPWB = true,
      useCARPArbiter = true
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
    val coherenceSpec = CARPSpec()
  }

  object TargetPlatformMSI {
    val nCore = 4
    val depth = 16
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMSI {},
      cacheParams = cacheParams,
      () => { new MSICoherenceTable() },
      () => { new MSILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      isConventionalProtocol = true
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

    val coherenceSpec = PMSISpec()
  }
  object TargetPlatformMESI {
    val nCore = 4
    val depth = 16
    val lineSize = 64
    val addrWidth = 64
    val interfaceAddrWidth = 64
    val dataWidth = 64
    val slotWidth = 128
    val busDataWidth = 64
    val masterCount = nCore * 2
    val cacheParams = SimpleCacheParams(depth, 4, lineSize * 8, addrWidth)
    val memorySystemParams = MemorySystemParams(
      addrWidth = addrWidth,
      interfaceAddrWidth = interfaceAddrWidth,
      dataWidth = dataWidth,
      slotWidth = slotWidth,
      busDataWidth = busDataWidth,
      busRequestType = new RequestType {},
      masterCount = masterCount,
      CohS = new PMESI {},
      cacheParams = cacheParams,
      () => { new MESICoherenceTable() },
      () => { new MESILLCCoherenceTable(masterCount) },
      withCriticality = false,
      outOfSlotResponse = false,
      isConventionalProtocol = true
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

    val coherenceSpec = PMSISpec()
  }
}
