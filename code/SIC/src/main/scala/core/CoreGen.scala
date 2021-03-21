
package core

import java.io.{File, PrintWriter}

import param.{CoreParam, RISCVParam}

object HeaderGen {
  def generateHeaders(args: Array[String], coreParam: CoreParam): Unit = {
    def getVerilogFolder: Option[String] = {
      val res = args.zip(args.drop(1)).filter(_._1 == "-td")
      res.length match {
        case 1 => Some(res.head._2)
        case _ => None
      }
    }

    val outputFolder = getVerilogFolder match {
      case Some(folder) => folder + "/"
      case None => throw new RuntimeException("Output folder must be present for system verilog package output")
    }

    // write system verilog package
    val svWriter = new PrintWriter(new File(outputFolder + "coreparam.sv"))
    svWriter.write(coreParam.toSystemVerilog())
    svWriter.close()

    // write c++ header package
    val cppWriter = new PrintWriter(new File(outputFolder + "coreparam.h"))
    cppWriter.write(coreParam.toCPP())
    cppWriter.close()

  }
}

object CoreGen extends App {
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
    coreID = 0,
    withAXIMemoryInterface = true,
    nCore = 3)

  chisel3.Driver.execute(args, () => new CoreGroupAXI(coreParam))

  HeaderGen.generateHeaders(args, coreParam)

}
