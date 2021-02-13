
package components

sealed trait BroadcastPutM
sealed trait NoBroadcastPutM
sealed trait DataBusConf
case object SharedEverything extends DataBusConf with BroadcastPutM
case object DedicatedDataBusOneWay extends DataBusConf with BroadcastPutM
case object DedicatedDataBusTwoWay extends DataBusConf with NoBroadcastPutM
