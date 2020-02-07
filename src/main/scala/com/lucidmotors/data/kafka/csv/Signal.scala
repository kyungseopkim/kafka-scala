package com.lucidmotors.data.kafka.csv

case class Signal(msgId: Int, timestamp: Long, epoch: Int, usec: Int, vlan:String, vin:String,
                  msgName: String, signalName: String, value: Float) {
  def toCVS():String = {
    s"${msgId},${timestamp},${epoch},${vin},${msgName},${signalName},${value}"
  }
}
