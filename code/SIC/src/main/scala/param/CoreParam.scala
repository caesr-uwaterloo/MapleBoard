
package param

import core.{MemoryRequestType, _}

class AXIParam(val axiWordWidth: Int) {}

/**
  *  The class transform the parameters into constants usable for C++ host and SystemVerilog
  */
case class CoreParam(fetchWidth: Int,
                isaParam: RISCVParam,
                iCacheReqDepth: Int, // should be 1
                iCacheRespDepth: Int,
                resetRegisterAddress: Long,
                initPCRegisterAddress: Long,
                baseAddrAddress: Long,
                coreID: Int,
                withAXIMemoryInterface: Boolean,
                nCore: Int) {
  // axiParam must be present when used, for example, in the axi memory
  def axiParam: Option[AXIParam] = if(withAXIMemoryInterface) {
    Some(new AXIParam(axiWordWidth = 64))
  } else {
    None
  }

  def toSystemVerilog(): String = svTemplate()
  def toCPP(): String = cppTemplate()

  private def cppStart(): String =
  """// This file is generated automatically using scala
    |#pragma once
  """.stripMargin
  private def cppTemplate(): String = {
    s"""${cppStart}
       |const int XLEN = ${isaParam.XLEN};
       |const unsigned long long resetRegisterAddress = ${resetRegisterAddress}ULL;
       |const unsigned long long initPCRegisterAddress = ${initPCRegisterAddress}ULL;
       |const unsigned long long baseAddrAddress = ${baseAddrAddress}ULL;
       |const bool memoryRead = ${MemoryRequestType.read.litValue()};
       |const bool memoryWrite = ${MemoryRequestType.write.litValue()};
     """.stripMargin
  }

  private def svStart(): String =
    """// This file is generated automatically using scala
      |package coreparam;
    """.stripMargin
  private def svEnd(): String =
    """endpackage: coreparam"""
  private def svTemplate(): String = {
    s"""${svStart}
       | // core parameters
       | parameter fetchWidth = ${fetchWidth};
       | parameter longint resetRegisterAddress = 64'd${resetRegisterAddress};
       | parameter longint initPCRegisterAddress = 64'd${initPCRegisterAddress};
       | parameter longint baseAddrAddress = 64'd${baseAddrAddress};
       | parameter memoryRead = ${MemoryRequestType.read.litValue()};
       | parameter memoryWrite = ${MemoryRequestType.write.litValue()};
       | parameter withAXIMemoryInterface = ${if(withAXIMemoryInterface) 1 else 0};
       | // ISA parameters
       | parameter XLEN       = ${isaParam.XLEN};
       |${svEnd}
     """.stripMargin
  }
}
