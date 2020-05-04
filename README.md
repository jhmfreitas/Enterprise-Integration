# Enterprise-Integration

Kong Public DNS: ec2-54-236-120-160.compute-1.amazonaws.com
OperatorDB Public DNS: operatordb.cfergfluhibr.us-east-1.rds.amazonaws.com
EC2 Instance Public DNS: ec2-54-196-98-231.compute-1.amazonaws.com

## Kong Important commnads
### Start Kong
kong start

### Stop Kong
kong stop

### Start KONGA
At docker terminal:
docker run -d --name konga -p 1337:1337 pantsel/konga 

Access:
http://192.168.99.100:1337/


### Start Operator Management Service
In EC2-Instance:

cd OperatorWebservice/

mvn exec:java -D"exec.mainClass"="Webservice.OperatorManagementServicePublisher"

## Camunda Important Commands
### Start Camunda Engine
At docker terminal:
docker run -d --name camunda -p 8080:8080 camunda/camunda-bpm-platform:latest

Access:
http://192.168.99.100:8080/camunda-welcome/index.html

## Kafka Important Commands
### Describe topic
/usr/local/kafka/bin/kafka-topics.sh --describe --zookeeper <Public_DNS>:2181 --topic T1_Uber

### Check Ports and Running Brokers
ps -ef |grep java |grep server

### Start Broker
sudo /usr/local/kafka/bin/kafka-server-start.sh -daemon /usr/local/kafka/config/server-3.properties

### Stop Broker
sudo kill < Port number >

### Create a consumer
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server <Public_DNS>:9093,<Public_DNS>:9094,<Public_DNS>:9095 --topic < Topic_Name > --group < Group_name >

## Start GIRA Producer
In target folder:
java -jar ProducerProvider2-0.0.1-SNAPSHOT.jar --provider-name GIRA --broker-list <Public_DNS>:9093,<Public_DNS>:9094,<Public_DNS>:9095 --topic T2_GIRA --token-list jjdgdj32 --throughput 1 --typeMessage JSON

## Start Uber Producer
In target folder:
java -jar ProducerProvider2-0.0.1-SNAPSHOT.jar --provider-name Uber --broker-list <Public_DNS>:9093,<Public_DNS>:9094,<Public_DNS>:9095 --topic T1_Uber --token-list jjdgdjs --throughput 50 --typeMessage JSON

## Start Metro Producer
In target folder:
java -jar ProducerProvider2-0.0.1-SNAPSHOT.jar --provider-name Metro --broker-list <Public_DNS>:9093,<Public_DNS>:9094,<Public_DNS>:9095 --topic T0_Metro --token-list jjdgdj32 --throughput 1 --typeMessage JSON

## Start OperatorService
curl -i -H "Content-Type: text/xml;charset=UTF-8" --data "@operatorTest.xml" -X POST http://ec2-34-228-247-65.compute-1.amazonaws.com:9997/operatorManagementService
