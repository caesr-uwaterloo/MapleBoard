
package coherence.internal

import chisel3._


trait AutoEnum extends {
  def getWidth: Int = 0
  def getGeneratedItemCount: Int = 0
  def getStateList: List[String] = List()
  def printState(signal: UInt): Unit = {
    for{i <- getStateList.indices} {
      when(signal === i.U) {
        printf(p"${getStateList(i)}")
      }
    }
  }
}
