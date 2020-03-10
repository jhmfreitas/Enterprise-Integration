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
/usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server ec2-54-225-58-37.compute-1.amazonaws.com:9093,ec2-54-225-58-37.compute-1.amazonaws.com:9094,ec2-54-225-58-37.compute-1.amazonaws.com:9095 --topic < Topic_Name > --group < Group_name >