package com.lucidmotors.data.kafka.csv

import java.io.File
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant, LocalDate, LocalDateTime, ZoneId}

import scala.collection.JavaConverters._
import java.util
import java.util.Collections
import java.util.concurrent.Executors

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicSessionCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Builder, AmazonS3ClientBuilder}
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.sksamuel.avro4s.{AvroOutputStream, AvroSchema}
import net.liftweb.json._
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.log4j.LogManager

import scala.collection.mutable.ArrayBuffer

object SignalFileSinker extends App {
  lazy val logger = LogManager.getLogger(getClass)

  val topic = "signals"
  val props = new util.Properties()

  val brokers = sys.env.getOrElse("BROKERS", "localhost:9092")
  val protocol = sys.env.getOrElse("PROTOCOL", "PLAINTEXT")
  val s3arn = sys.env.getOrElse("S3ARN", "arn:aws:iam::411026478373:role/redshiftS3Access")
  val accessKey = sys.env.getOrElse("ACCESS_KEY", "AKIAV7MYWKUSZYSC77TR")
  val secretKey = sys.env.getOrElse("SECRET_KEY", "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")
  val kafkaOffset = sys.env.getOrElse("KAFKA_OFFSET", "earliest")
  val groupId = sys.env.getOrElse("KAFKA_GROUP_ID", "csv-sinker")
  val outputDir = sys.env.getOrElse("OUTPUT_FOLDER", "s3a://redshift-live-vehicles-signals/scala-client/csv")
  props.put("bootstrap.servers", brokers)
  props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
  props.put("auto.offset.reset", kafkaOffset)
  props.put("group.id", groupId)
  props.put("security.protocol", protocol)

  def now(): String = System.currentTimeMillis().toString
  def today():String = LocalDate.now(ZoneId.of("UTC")).toString

  def getS3Client():AmazonS3 = {
    AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build()
  }

  case class BucketName(name:String, path:String)

  val outputBucket = {
    val uri = new URI(outputDir)
    BucketName(uri.getHost, uri.getPath)
  }

  logger.info(outputDir)
  val service = Executors.newCachedThreadPool()

  class Saver(data:Array[Signal]) extends Runnable {
    override def run(): Unit = {
      val filename:String = s"/tmp/${now()}.avro"
      logger.info(s"writing to ${filename}")
      val outputFile = Paths.get(filename).toFile
      val schema = AvroSchema[Redshift]
      val os = AvroOutputStream.data[Redshift].to(outputFile).build(schema)
      val records = data.map(_.toRedshift())
      os.write(records)
      os.close()
      val client = getS3Client()

      val dst = Array(outputBucket.path.substring(1), today(), outputFile.getName).mkString("/")
      logger.info(s"upload to ${dst}")
      client.putObject(outputBucket.name, dst, outputFile)
      logger.info(s"s3://${Array(outputBucket.name, dst).mkString("/")} upload done")
      outputFile.delete()
    }
  }

  implicit val formats = DefaultFormats

  val bucketSize = 1000000
  val buffer = new ArrayBuffer[Signal](bucketSize)

  val consumer: KafkaConsumer[String, String] = new KafkaConsumer[String, String](props)
  consumer.subscribe(Collections.singletonList(topic))
  while (true) {
    val results = consumer.poll(Duration.ofMillis(200)).asScala
    for (record <- results) {
      val payload = record.value()
      val json = parse(payload)
      val signal = json.extract[Signal]
      buffer += signal
      if (buffer.size == bucketSize) {
        service.submit(new Saver(buffer.toArray.clone()))
        buffer.clear()
      }
    }
  }
}
