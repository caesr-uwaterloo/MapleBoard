
package coherences

import chisel3.experimental._

object CoherenceMessage extends ChiselEnum {
  val NONE_CACHE_EVENT, Load, Store, AMO, OwnGetS, OwnGetM, OtherGetS, OtherGetM, OwnPutM,
  OtherPutM, OwnUpg, OtherUpg, Data, Ack, FLUSH, LLCC_FLUSH, EData, Replacement, OwnPutS, OtherPutS, TIMEOUT,
  CacheGetS, CacheGetM, CachePutM, CacheUpg = Value
}
