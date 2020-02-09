package com.lucidmotors.data.kafka.csv

case class Redshift(msgId: Int, timestamp: Long, epoch: Int, vin:String, msgName: String, signalName: String, value: Float)
