package webservice;

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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//Service Implementation
@WebService(endpointInterface = "Webservice.OperatorManagementService")
public class OperatorManagementServiceImpl implements OperatorManagementService {
	static String AWSIP = "ec2-35-173-138-16.compute-1.amazonaws.com";
	static String AWSDBIP = "userdb.cfergfluhibr.us-east-1.rds.amazonaws.com";
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
	
	public void startService(InputStream inputStream, OutputStream outputStream) {
		
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
			String operator = (String) extractedEvent.get("operator");
			if(operator != null) {
				System.out.println("processEvent(operator):" + operator + "\n");
			}
			
			JSONObject infojson = (JSONObject) extractedEvent.get("info");
			System.out.println("processEvent(eventType):" + eventType + "\n");
			System.out.println("processEvent(info):" + extractedEvent.get("info") + "\n");
			
			switch(eventType) {
				case "trip-cost":
					processTripCostEvent(conn, infojson, producer);
					break;
				case "new-operator":
					insertOperator(conn, operator, infojson, producer);
					break;
				case "new-discount":
					insertDiscount(conn, operator, infojson, producer);
					break;
				default:
					System.out.println("parseEvent(eventType): Error : There is not such type of event:" + eventType + "\n");
			}	
		}catch (SQLException e) {
			System.out.println("parseMessage:" + (String) e.toString() + "\n");
		}

	}
	

	private void processTripCostEvent(Connection conn, JSONObject infojson, KafkaProducer<String, String> producer) {
		
		boolean hasPass = Boolean.parseBoolean((String) infojson.get("hasPass"));
		float baseCost = Float.valueOf((String) infojson.get("baseCost"));
		String timeStamp = (String) infojson.get("timeStamp");
		String serviceId = (String) infojson.get("serviceId");
		String planType = (String) infojson.get("planType");
		String token = (String) infojson.get("token");
				
		insertPlanType(conn, planType);
		
		float discount = getDiscount(conn, hasPass, timeStamp, serviceId);
		float debitAmount = baseCost * discount;
			
		produceDebitEvent(producer, token, planType, debitAmount);
	}
	
	private float getDiscount(Connection conn, boolean hasPass, String timestamp, String serviceId) {
		// TODO Auto-generated method stub
		if (hasPass == false) {
			getBestDiscountWithPass(conn, timestamp, serviceId);
		}
		else {
			getBestDiscountWithoutPass(conn, timestamp, serviceId);
		}
		return 0;
	}
	
	private void getBestDiscountWithoutPass(Connection conn, String timestamp, String serviceId) {
		PreparedStatement s;
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select * from discount where nonPassOnly = ? and beginAt >= ? and endAt <= ? and serviceId = ?");
			s.setBoolean(1, false);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setTimestamp(3, java.sql.Timestamp.valueOf(timestamp));
			s.setString(4, serviceId);
			resultSet = s.executeQuery();
			float discountValue = 0;
			while (resultSet.next()) {
				float value = resultSet.getInt("value")/100;
				if(value > discountValue) {
					discountValue = value;
				}
		    }
		    
		    s.close();
		    resultSet.close();
		} catch (SQLException e) {
			System.out.println("Error : Looking for discount ->" + e.toString() + "\n");
		}
	}
	private void getBestDiscountWithPass(Connection conn, String timestamp, String serviceId) {
		PreparedStatement s;
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select * from discount where passOnly = ? and beginAt >= ? and endAt <= ? and serviceId = ?");
			s.setBoolean(1, false);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setTimestamp(3, java.sql.Timestamp.valueOf(timestamp));
			s.setString(4, serviceId);
			resultSet = s.executeQuery();
			float discountValue = 0;
			while (resultSet.next()) {
				float value = resultSet.getInt("value")/100;
				if(value > discountValue) {
					discountValue = value;
				}
		    }
		    
		    s.close();
		    resultSet.close();
		} catch (SQLException e) {
			System.out.println("Error : Looking for discount ->" + e.toString() + "\n");
		}
	}
	
	private void insertOperator(Connection conn, String operator, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String operatorType = (String) infojson.get("operatorType");
		
		addTopic(operator, operatorType);
		
		PreparedStatement s = conn.prepareStatement("insert into operator values(?,?)");
		s.setString(1, operator);
		s.setString(2, operatorType);
		s.executeUpdate();
		s.close();
		
	}
	
	private void insertDiscount(Connection conn, String operator, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String discountId = (String) infojson.get("discountId");
		String discountName = (String) infojson.get("discountName");
		String value = (String) infojson.get("value");
		String beginAt = (String) infojson.get("beginAt");
		String endAt = (String) infojson.get("endAt");
		boolean passOnly = Boolean.parseBoolean((String) infojson.get("passOnly"));
		boolean nonPassOnly = Boolean.parseBoolean((String) infojson.get("nonPassOnly"));
		
		PreparedStatement s = conn.prepareStatement("insert into discount values(?,?,?,?,?,?,?)");
		s.setString(1, discountId);
		s.setString(2, discountName);
		s.setInt(3, Integer.parseInt(value));
		s.setTimestamp(4, java.sql.Timestamp.valueOf(beginAt));
		s.setTimestamp(5, java.sql.Timestamp.valueOf(endAt));
		s.setBoolean(6, passOnly);
		s.setBoolean(7, nonPassOnly);
		s.executeUpdate();
		s.close();
		
		s = conn.prepareStatement("insert into operator_discount values(?,?)");
		s.setString(1, operator);
		s.setString(2, discountId);
		s.executeUpdate();
		s.close();
		
		s = conn.prepareStatement("insert into discount_planType values(?,?)");
		s.setString(1, discountId);
		s.executeUpdate();
		s.close();
	}
	
	private void insertPlanType(Connection conn, String plan) throws SQLException {
		PreparedStatement s = conn.prepareStatement("insert into planType values(?)");
		s.setString(1, plan);
		s.executeUpdate();
		s.close();
		
		s = conn.prepareStatement("insert into discount_planType values(?,?)");
		s.setString(2, plan);
		s.executeUpdate();
		s.close();
	}
	
	private void produceDebitEvent(KafkaProducer<String, String> producer, String token, String planType, String amount) {
		
		
        String event = "{\"event\":{\"eventType\":\"debit\", \"info\":{ " +
				"\"token\": \" " + token + "\", "+
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