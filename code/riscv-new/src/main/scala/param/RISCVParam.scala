
package param

class RISCVParam(val XLEN: Int,
                 val Embedded: Boolean,
                 val Atomic: Boolean,
                 val Multiplication: Boolean,
                 val Compressed: Boolean,
                 val SingleFloatingPoint: Boolean,
                 val DoubleFloatingPoint: Boolean) {
  require(XLEN == 32 || XLEN == 64, "XLEN can only be 32 or 64")
  private val rveRegisterCount = 16
  private val rviRegisterCount = 32
  private val insnW = 32
  private val csrCnt = 4096
  private val exceptionW = XLEN
  private val tvalW = XLEN

  def registerCount: Int = if(Embedded) rveRegisterCount else rviRegisterCount
  def instructionWidth: Int = insnW
  def csrCount: Int = csrCnt
  def exceptionWidth: Int = XLEN
  def tvalWidth: Int = XLEN

  // instruction fields

  def opcodeRange: (Int, Int) = Tuple2(6, 0)
  def rdRange: (Int, Int) = Tuple2(11, 7)
  def funct3Range: (Int, Int) = Tuple2(14, 12)
  def rs1Range: (Int, Int) = Tuple2(19, 15)
  def rs2Range: (Int, Int) = Tuple2(24, 20)
  def funct5Range: (Int, Int) = Tuple2(31, 27)
  def funct7Range: (Int, Int) = Tuple2(31, 25)

  // I-type
  def imm12Ranges: List[(Int, Int)] = List((31, 20))
  // S-type
  def simm12Ranges: List[(Int, Int)] = List((31, 25), (11, 8), (7, 7))
  // B-type
  def bimm12Ranges: List[(Int, Int)] = List((31, 31), (7, 7), (30, 25), (11, 8))
  // U-type
  def imm20Ranges: List[(Int, Int)] = List((31, 31), (30, 20), (19, 12))
  // J-type
  def jimm20Ranges: List[(Int, Int)] = List((31, 31), (19, 12), (20, 20), (30, 25), (24, 21))
}
