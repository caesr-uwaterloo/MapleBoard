
package params

import chisel3._
import chisel3.util._
import components.UIntHolding

trait L1CacheParams {
  def nSets:         Int
  def nWays:         Int // This field will be ignored until we have set-assoc cache
  def lineWidth:     Int
  def addrWidth:     Int

  def lineBytes: Int = lineWidth / 8
  def lineOffsetWidth: Int = log2Ceil(lineBytes)
  def tagWidth: Int = addrWidth - lineOffsetWidth
  def lineAddrWidth: Int = log2Ceil(nSets)

  def getLineAddress(addr: UInt): UInt = addr(lineOffsetWidth + lineAddrWidth - 1, lineOffsetWidth)
  def alignToCacheline(addr: UInt): UInt = tagAddrToLineAddr(getTagAddress(addr))
  def tagAddrToLineAddr(tag: UInt): UInt = Cat(tag, 0.U(lineOffsetWidth.W)).asUInt
  def getTagAddress(addr: UInt): UInt = addr(addrWidth - 1, lineOffsetWidth)
  def genTag: UInt = UInt(tagWidth.W)
  def genCacheLine: UInt = UInt(lineWidth.W)
  def genCacheLineBytes: Vec[UInt] = Vec(lineBytes, UInt(8.W))
  def genAddress: UInt = UInt(addrWidth.W)
  def genWay: UInt = UIntHolding(nWays)
  def genSet: UInt = UIntHolding(nSets)
}

case class SimpleCacheParams(nSets: Int = 16,
                            nWays: Int = 1,
                            lineWidth: Int = 512,
                            addrWidth: Int = 32
                            ) extends L1CacheParams {}