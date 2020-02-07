FROM openjdk:8-alpine

WORKDIR /root
COPY target/scala-2.12/scala-client-assembly-0.1.0-SNAPSHOT.jar /root

ENTRYPOINT ["java", "-cp", "/root/scala-client-assembly-0.1.0-SNAPSHOT.jar", "com.lucidmotors.data.kafka.csv.SignalCsvSinker"]

