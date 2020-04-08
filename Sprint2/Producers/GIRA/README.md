# ProducerProviderAbstractWithoutTokenControl
Producer Provider Without Token Control

The project is to be used under the Enterprise Integration course (2020 edition) and the goal is to be customized by students (if needed). To compile execute the following :
````
mvn package shade:shade
````
The usage of this Provider Producer for MaaS Simulator is the following.
`````
MaaSMessageProviderWithoutTokenControl --provider-name <Name to assign to the provider> --broker-list <KafkaBrokerList with Ports> --topic <topic> --token-list <token-list> --throughput <value> --typeMessage <value>
where, 
--provider-name: is the name of the provider and is mandatory
--broker-list: is a broker list with ports (e.g.: kafka02.example.com:9092,kafka03.example.com:9092) and the default value is localhost:9092
--topic: is the kafka topic to be provisioned and is mandatory
--token-list: is a list of client tokens (e.g.: eudij3674fgo,dhjsyuyfdhi3,djkfjd8) and is mandatory
--throughput: is the approximate maximum messages to be produced by minute and the default value is 10
--typeMessage: is the type of message to be produced: JSON or XML, default value is JSON.
