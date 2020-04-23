package Webservice;

import javax.jws.WebService;

import org.I0Itec.zkclient.ZkClient;
import java.util.Properties;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import kafka.utils.ZkUtils;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;

//Service Implementation
@WebService(endpointInterface = "Webservice.OperatorManagementService")
public class OperatorManagementServiceImpl implements OperatorManagementService {
	static String AWSIP = "ec2-34-228-247-65.compute-1.amazonaws.com";
	static String AWSDBIP = "operatordb.cfergfluhibr.us-east-1.rds.amazonaws.com";
	public OperatorManagementServiceImpl() {
	
	}
	public int addTopic(String operatorName, String operatorType){
		
		String zookeeperConnect = AWSIP + ":2181";
		int sessionTimeoutMs = 10 * 1000;
		int connectionTimeoutMs = 8 * 1000;
		
		String topic = operatorType.toUpperCase() + "_" + operatorName;
		int partitions = 3;
		int replication = 3;
		Properties topicConfig = new Properties();
	
		ZkClient zkClient = new ZkClient(
			zookeeperConnect,
			sessionTimeoutMs,
			connectionTimeoutMs,
			ZKStringSerializer$.MODULE$);
	
		boolean isSecureKafkaCluster = false;
	
		ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect),
	isSecureKafkaCluster);
	
		AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig,
	RackAwareMode.Enforced$.MODULE$);
		
		zkClient.close();
	
		return(0);
	}
	
	@Override
	public void startService() {
		
		String groupId = "OperatorManagementService";
		
		try {
			//Prepare database connection
			Connection conn = null;
			boolean bd_ok = false;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/operatorManagementDB", "admin", "projetoie");
				bd_ok = true;
			} catch (SQLException sqle) {
				System.out.println("Error : " + sqle.toString());
			} catch (ClassNotFoundException e) {
				System.out.println("Error : " + e.toString());
			}
			
			//Prepare consumer
			Properties props = new Properties();
			props.put("bootstrap.servers", AWSIP + ":9093," + AWSIP + ":9094," + AWSIP + ":9095");
			props.put("group.id", groupId);
			props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("auto.commit.offset", "false"); // to commit manually
			KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(props);
			List<String> topicsList = new ArrayList<String> ();
			topicsList.add("TripCosts");
			topicsList.add("OperatorProvision");
			consumer.subscribe(topicsList);
			
			//Prepare producer
			Properties propsConsumer = new Properties();
			propsConsumer.put("bootstrap.servers", AWSIP + ":9093," + AWSIP + ":9094," + AWSIP + ":9095");
			propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
			propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
			KafkaProducer<String, String> producer = new KafkaProducer<String, String>(propsConsumer);
			
			//Start consumer events
			try {
				System.out.println("startService: Begin!!\n");
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					System.out.println("startService: records = " + records.count() + "\n");
					for (ConsumerRecord<String, String> record : records) {
						System.out.println("startService: topic = " + record.topic() +", partition = " + record.partition() + ", offset = " + record.offset() + ",customer = " + record.key() + ",message = " + record.value() +"\n");
						
						String message = record.value();
						String topic = record.topic();
						
						JSONParser parser = new JSONParser();
						JSONObject extractedEvent = (JSONObject) ((JSONObject) parser.parse(message)).get("event");
						
						if(extractedEvent != null && bd_ok) {
							processEvent(extractedEvent, topic, conn, producer);	
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
		
	
	private void processEvent(JSONObject extractedEvent, String topic, Connection conn, KafkaProducer<String, String> producer) {
		try {
			String eventType = (String) extractedEvent.get("eventType");
			
			JSONObject infojson = (JSONObject) extractedEvent.get("info");
			System.out.println("processEvent(eventType):" + eventType + "\n");
			System.out.println("processEvent(info):" + extractedEvent.get("info") + "\n");
			
			switch(eventType) {
				case "trip-cost":
					processTripCostEvent(conn, infojson, producer);
					break;
				case "new-operator":
					String operator = (String) extractedEvent.get("operator");
					System.out.println("processEvent(operator):" + operator + "\n");
					insertOperator(conn, operator, infojson, producer);
					break;
				case "new-discount":
					JSONArray operators = (JSONArray) extractedEvent.get("operator");
					System.out.println("processEvent(operator):" + operators + "\n");
					insertDiscount(conn, operators, infojson, producer);
					break;
				default:
					System.out.println("parseEvent(eventType): Error : There is not such type of event:" + eventType + "\n");
			}	
		}catch (SQLException e) {
			System.out.println("processEvent:" + (String) e.toString() + "\n");
		}

	}
	

	private void processTripCostEvent(Connection conn, JSONObject infojson, KafkaProducer<String, String> producer) {
		String baseCostString = (String) infojson.get("cost");
		String operatorName = (String) infojson.get("operatorName");
		float baseCost;
		if(baseCostString.equals("null")) {
			System.out.println("processTripCostEvent: basecost is null -> " + baseCostString + "\n");
			baseCost = getOperatorBaseCost(conn, operatorName);
		}else {
			System.out.println("processTripCostEvent: basecost is -> " + baseCostString + "\n");
			baseCost = Float.valueOf(baseCostString);
		}
		String timeStamp = (String) infojson.get("timeStamp");
		String planType = (String) infojson.get("planType");
		String token = (String) infojson.get("token");
		
		float discount = getDiscount(conn, planType, timeStamp, operatorName);
		float debitAmount = baseCost * discount;
			
		produceDebitEvent(producer, token, planType, debitAmount);
	}
	
	private float getOperatorBaseCost(Connection conn, String operatorName) {
		PreparedStatement s;
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select price from operator where operatorName = ?");
			s.setString(1, operatorName);
			resultSet = s.executeQuery();
			while (resultSet.next()) {
				System.out.println("Got operator cost ->" + resultSet.getFloat("price") + "\n");
				return resultSet.getFloat("price");
		    }
		    
		    s.close();
		    resultSet.close();

		} catch (SQLException e) {
			System.out.println("Error : getOperatorBaseCost ->" + e.toString() + "\n");
		}
		
		return 1;
	}
	
	private float getDiscount(Connection conn, String planType, String timestamp, String operatorName) {
		
		PreparedStatement s;
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select value from discount inner join operator_discount on discount.discountId = operator_discount.discountId inner join discount_planType on discount.discountId = discount_planType.discountId where beginAt >= ? and endAt <= ? and operatorName = ? and plan=?");
			s.setTimestamp(1, java.sql.Timestamp.valueOf(timestamp));
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setString(3, operatorName);
			s.setString(4, planType);
			resultSet = s.executeQuery();
			float discountValue = 0;
			while (resultSet.next()) {
				float value = resultSet.getInt("value")/100;
				if(value > discountValue) {
					System.out.println("getDiscount : Discount found = " + value + "\n");
					discountValue = value;
				}
		    }
		    
		    s.close();
		    resultSet.close();
		    if(discountValue != 0){
		    	System.out.println("getDiscount : Best Discount found = " + discountValue + "\n");
				return discountValue;
		    }
		    //No applicable discount
		    return 1;
		} catch (SQLException e) {
			System.out.println("Error : Looking for discount ->" + e.toString() + "\n");
		}
		
		//Should not happen
		return 1;
	}
	
	private void insertOperator(Connection conn, String operator, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String operatorType = (String) infojson.get("operatorType");
		String price = (String) infojson.get("baseCost");
		
		addTopic(operator, operatorType);
		
		PreparedStatement s = conn.prepareStatement("insert into operator values(?,?,?)");
		s.setString(1, operator);
		s.setString(2, operatorType);
		s.setFloat(3, Float.parseFloat(price));
		s.executeUpdate();
		s.close();
		
	}
	
	private void insertDiscount(Connection conn, JSONArray operators, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String discountId = (String) infojson.get("discountId");
		String discountName = (String) infojson.get("discountName");
		String value = (String) infojson.get("value");
		String beginAt = (String) infojson.get("beginAt");
		String endAt = (String) infojson.get("endAt");
		JSONArray planTypes = (JSONArray) infojson.get("appliesToPlanType");
		
		PreparedStatement s = conn.prepareStatement("insert into discount values(?,?,?,?,?,?,?)");
		s.setString(1, discountId);
		s.setString(2, discountName);
		s.setInt(3, Integer.parseInt(value));
		s.setTimestamp(4, java.sql.Timestamp.valueOf(beginAt));
		s.setTimestamp(5, java.sql.Timestamp.valueOf(endAt));
		s.executeUpdate();
		s.close();
		
		
        Iterator<String> operatorsIterator = operators.iterator();
        while (operatorsIterator.hasNext()) {
        	String operator = operatorsIterator.next();
        	System.out.println("Operator:" + operator);
            
            s = conn.prepareStatement("insert into operator_discount values(?,?)");
    		s.setString(1, operator);
    		s.setString(2, discountId);
    		s.executeUpdate();
    		s.close();
    		
    		for (int i = 0; i < planTypes.size(); i++) {
    			String plan = (String) planTypes.get(i);
	          	System.out.println("PlanType:" + plan);
	              
	          	s = conn.prepareStatement("insert into discount_planType values(?,?)");
	      		s.setString(1, discountId);
	      		s.setString(2, plan);
	      		s.executeUpdate();
	      		s.close();
			}
        }
		
	}
	
	private void produceDebitEvent(KafkaProducer<String, String> producer, String token, String planType, float amount) {
        String event = "{\"event\":{\"eventType\":\"debit\", \"info\":{ " +
				"\"token\": \"" + token + "\", "+
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