# Enterprise-Integration

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