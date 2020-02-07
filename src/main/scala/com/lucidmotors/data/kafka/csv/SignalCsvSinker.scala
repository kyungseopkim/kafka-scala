package com.lucidmotors.data.kafka.csv

import java.nio.charset.StandardCharsets
import java.time.{Duration, LocalDateTime, ZoneId}

import scala.collection.JavaConverters._
import java.util
import java.util.Collections

import net.liftweb.json._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, LocalFileSystem, Path}
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.log4j.LogManager

import scala.collection.mutable.ArrayBuffer

object SignalCsvSinker extends App {
  lazy val logger = LogManager.getLogger(getClass)

  val topic = "signals"
  val props = new util.Properties()

  val brokers = "b-2.logging.2wby21.c6.kafka.us-east-1.amazonaws.com:9094,b-3.logging.2wby21.c6.kafka.us-east-1.amazonaws.com:9094,b-1.logging.2wby21.c6.kafka.us-east-1.amazonaws.com:9094"
  sys.env.getOrElse("BROKERS", "localhost:9092")
  props.put("bootstrap.servers", brokers)
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("auto.offset.reset", "earliest")
  props.put("group.id", "csv-sinker")
  props.put("security.protocol", "SSL")

  val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](props)

  def now(): String = System.currentTimeMillis().toString

  consumer.subscribe(Collections.singletonList(topic))


  val conf = new Configuration()
  conf.set("hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem")
  conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
  conf.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem")

  val fs = FileSystem.get(conf)
  val outputDir = sys.env.getOrElse("OUTPUt", "s3a://redshift-live-vehicles-signals/scala-client/csv")
  logger.info(outputDir)
  def filename(): String = s"${outputDir}/${now()}.csv"

  implicit val formats = DefaultFormats

  val bucketSize = 100000
  val buffer = new ArrayBuffer[Signal](bucketSize)

  while (true) {
    val results = consumer.poll(Duration.ofMillis(200)).asScala
    for (record <- results) {
      val payload = record.value()
      val json = parse(payload)
      val signal = json.extract[Signal]
      buffer += signal
      if (buffer.size == bucketSize) {
        import org.apache.hadoop.fs.FSDataOutputStream
        val path = new Path(filename())
        logger.info(s"writing to ${path}")
        val out = fs.create(path)
        out.write(buffer.toArray.map(_.toCVS()).mkString("\n").getBytes(StandardCharsets.UTF_8))
        out.close()
        buffer.clear()
      }
    }
  }
}
