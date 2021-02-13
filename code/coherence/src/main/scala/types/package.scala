// See README.md for license details.

// import chisel3.core.EnumFactory
import chisel3._
import chisel3.util.log2Ceil
import scala.language.dynamics

import scala.collection.mutable

package object types {

  trait EnumTrait extends Dynamic {
    val value_map: mutable.HashMap[String, UInt] = new mutable.HashMap[String, UInt]
    def selectDynamic(name: String): UInt = value_map(name)
    def getWidth: Int
  }

  trait AutoEnumTrait extends EnumTrait {
    def itemList: List[String]

    for((x, i) <- itemList.zipWithIndex) {
      value_map(x) = i.U(width = getWidth.W)
    }
    def getWidth: Int = log2Ceil(itemList.length)
  }
}
