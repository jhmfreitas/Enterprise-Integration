import java.sql.Timestamp;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class UserProducer {
	public static void main(String[] args) {
		String AWSIP = "3.81.102.111";

		Properties propsConsumer = new Properties();
		propsConsumer.put("bootstrap.servers", AWSIP + ":9093," + AWSIP + ":9094," + AWSIP + ":9095");
		propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
		propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(propsConsumer);

		String event = "{\"event\":{\"eventType\":\"new-user\",\"info\":{\"id\":\"69c54ceeaedd220\",\"email\":\"user@gmail.com\",\"planType\":\"pre-paid\",\"firstName\":\"Paulo\",\"lastName\":\"Neves\",\"balance\":\"500\",\"hasPass\":\"true\"}}}";
		// Fire-and-forget
		System.out.println("Fire-and-forget starting...");
		Timestamp mili = new Timestamp(System.currentTimeMillis());
		String seqkey = new Long(mili.getTime()).toString();
		ProducerRecord<String, String> record = new ProducerRecord<>("T2_GIRA", seqkey, event);
		try {
			producer.send(record);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		producer.close();
		System.out.println("Fire-and-forget stopped.");
	}
}
