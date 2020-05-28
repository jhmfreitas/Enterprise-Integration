package Webservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jws.WebService;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

//Service Implementation
@WebService(endpointInterface = "Webservice.OperatorManagementService")
public class OperatorManagementServiceImpl implements OperatorManagementService {
	static String AWSIP = "ec2-35-173-139-11.compute-1.amazonaws.com";
	static String AWSDBIP = "operatordb.ca14fw262vr6.us-east-1.rds.amazonaws.com";
	static KafkaProducer<String, String> producer;
	static KafkaConsumer<String, String> consumer;
	static Connection conn = null;
	
	public OperatorManagementServiceImpl() {
		startService();
	}

	@Override
	public void startService() {
		
		String groupId = "OperatorManagementService";
		
		try {
			//Prepare database connection
			boolean bd_ok = false;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/operatordb", "admin", "projetoie");
				bd_ok = true;
			} catch (SQLException sqle) {
				System.out.println("Error : " + sqle.toString());
			} catch (ClassNotFoundException e) {
				System.out.println("Error : " + e.toString());
			}
			
			//Prepare consumer
			Properties props = new Properties();
			props.put("bootstrap.servers", AWSIP + ":9092," + AWSIP + ":9093," + AWSIP + ":9094");
			props.put("group.id", groupId);
			props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("auto.commit.offset", "false"); // to commit manually
			consumer = new KafkaConsumer<String, String>(props);
			List<String> topicsList = new ArrayList<String> ();
			topicsList.add("TripCosts");
			topicsList.add("OperatorProvision");
			consumer.subscribe(topicsList);
			
			//Prepare producer
			Properties propsConsumer = new Properties();
			propsConsumer.put("bootstrap.servers", AWSIP + ":9092," + AWSIP + ":9093," + AWSIP + ":9094");
			propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
			propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
			producer = new KafkaProducer<String, String>(propsConsumer);
			
			//Start consumer events
			try {
				System.out.println("startService: Begin!!\n");
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					for (ConsumerRecord<String, String> record : records) {
						System.out.println("startService: topic = " + record.topic() +", partition = " + record.partition() + ", offset = " + record.offset() + ",customer = " + record.key() + ",message = " + record.value() +"\n");
						
						String message = record.value();
						String topic = record.topic();
						
						JSONParser parser = new JSONParser();
						JSONObject extractedEvent = (JSONObject) ((JSONObject) parser.parse(message)).get("event");
						
						if(extractedEvent != null && bd_ok) {
							processEvent(extractedEvent, topic);	
						}
						else {
							System.out.println("Error : extractedEvent is " + extractedEvent + " and BDConnnection=" + bd_ok + "\n");
						}
					}
					// Commit Current Offset
					consumer.commitSync();
				}
			}catch (CommitFailedException e) {
				System.out.println("Error : Commit Failed -> " + e.toString()+ "\n");
			}finally {
				consumer.close();
				try {
					conn.close();
				} catch (SQLException e) {
					System.out.println("Error : " + e.toString()+ "\n");
				}
			}
		} catch (Exception exc) {
			System.out.println("Error : " + exc.toString()+ "\n");
		}
	}
		
	private void processEvent(JSONObject extractedEvent, String topic) {
		String eventType = (String) extractedEvent.get("eventType");
		System.out.println("processEvent(eventType):" + eventType + "\n");
		JSONObject infojson = (JSONObject) extractedEvent.get("info");
		System.out.println("processEvent(info):" + extractedEvent.get("info") + "\n");
		
		switch(eventType) {
			case "trip-cost":
				processTripCostEventFromJson(infojson);
				break;
			default:
				System.out.println("parseEvent(eventType): Error : There is not such type of event:" + eventType + "\n");
		}

	}
	
	private void processTripCostEventFromJson(JSONObject infojson) {
		String baseCostString = (String) infojson.get("cost");
		String operatorName = (String) infojson.get("operatorName");
		String timeStamp = (String) infojson.get("timeStamp");
		String planType = (String) infojson.get("planType");
		String id = (String) infojson.get("id");
		
		processTripCostEvent(baseCostString, operatorName, timeStamp, planType, id);
	}
	
	
	public int processTripCostEvent(String baseCostString, String operatorName, String timeStamp, String planType,
			String id) {
		float discount = getDiscount(planType, timeStamp, operatorName);
		
		if(discount == -1) {
			return 1;
		}
		float baseCost;
		if(baseCostString.equals("null")) {
			baseCost = getOperatorBaseCost(operatorName);
			System.out.println("BaseCost:" + baseCost  + "\n");
		}else {
			baseCost = Float.valueOf(baseCostString);
			System.out.println("BaseCost:" + baseCost  + "\n");
		}
		float debitAmount = baseCost * (1.0f - discount);
		produceDebitEvent(id, planType, debitAmount);
		
		return 0;
	}
	
	private float getOperatorBaseCost(String operatorName) {
		PreparedStatement s;
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select price from operator where operatorName = ?");
			s.setString(1, operatorName);
			resultSet = s.executeQuery();
			while (resultSet.next()) {
				System.out.println("Operator Cost ->" + resultSet.getFloat("price") + "\n");
				return resultSet.getFloat("price");
		    }
		    
		    s.close();
		    resultSet.close();

		} catch (SQLException e) {
			System.out.println("Error : getOperatorBaseCost ->" + e.toString() + "\n");
		}
		//Operator doesn't exist
		return 1;
	}
	
	private float getDiscount(String planType, String timestamp, String operatorName) {
		System.out.println("getDiscount : Start searching for discount\n");
		PreparedStatement s;
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select value from discount,operator_discount,discount_planType where discount.discountId = operator_discount.discountId and discount.discountId = discount_planType.discountId and beginAt <= ? and endAt >= ? and operatorName = ? and plan=?;");
			s.setString(1, timestamp);
			s.setString(2, timestamp);
			s.setString(3, operatorName);
			s.setString(4, planType);
			resultSet = s.executeQuery();
			float discountValueAux = 0;
			while (resultSet.next()) {
				int value = resultSet.getInt("value");
				System.out.println("getDiscount : Applicable discount found = " + value + "\n");
				if(value > discountValueAux) {
					discountValueAux = value;
				}
		    }
		    
		    s.close();
		    resultSet.close();
		    if(discountValueAux != 0){
		    	System.out.println("Best discount :" + discountValueAux/100.0f + "\n");
		    	return discountValueAux/100.0f;
		    }
		} catch (SQLException e) {
			System.out.println("Error : Looking for discount ->" + e.toString() + "\n");
			return -1;
		}
		System.out.println("getDiscount : No discount found\n");
		return 0;
	}

	private void produceDebitEvent(String id, String planType, float amount) {
        String event = "{\"event\":{\"eventType\":\"debit\", \"info\":{ " +
				"\"id\": \"" + id + "\", "+
				"\"planType\": \"" + planType +"\", "+
				"\"amount\": \"" + amount + "\" "+
			"}"+
		"}"+
		"}";
	
		ProducerRecord<String, String> record = new ProducerRecord<>("Debit", "DebitKey", event);
		producer.send(record);
		System.out.println("produceDebitEvent : Sent -> " + event + "\n");     	
	}

}