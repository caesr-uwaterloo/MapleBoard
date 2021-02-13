
package components.arbiters

import chisel3._

trait HasChoice {
  def getChoice : UInt
}