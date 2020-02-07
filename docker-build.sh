#!/bin/bash

sbt clean assembly
img=411026478373.dkr.ecr.us-east-1.amazonaws.com/signal-csv-sinker:v0.1 

docker build -t $img . 
docker push $img

