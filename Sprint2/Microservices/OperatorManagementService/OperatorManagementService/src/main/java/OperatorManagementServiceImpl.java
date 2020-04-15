package Webservice;

import javax.jws.WebService;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//Service Implementation
@WebService(endpointInterface = "Webservice.OperatorManagementService")
public class OperatorManagementServiceImpl implements OperatorManagementService {
	public OperatorManagementServiceImpl() {
	
	}
	public int addTopic(String topic1, String topic2, String topic3){
		
		String zookeeperConnect = "ec2-35-173-138-16.compute-1.amazonaws.com:2181";
		int sessionTimeoutMs = 10 * 1000;
		int connectionTimeoutMs = 8 * 1000;
		
		String topic1 = "Debit";
		int partitions1 = 3;
		int replication1 = 3;
		Properties topicConfig1 = new Properties();
		
		String topic2 = "OperatorProvision";
		int partitions2 = 3;
		int replication2 = 3;
		Properties topicConfig2 = new Properties();
		
		String topic3 = "new-Operator";
		int partitions3 = 3;
		int replication3 = 3;
		Properties topicConfig3 = new Properties();
	
		ZkClient zkClient = new ZkClient(
			zookeeperConnect,
			sessionTimeoutMs,
			connectionTimeoutMs,
			ZKStringSerializer$.MODULE$);
	
		boolean isSecureKafkaCluster = false;
	
		ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect),
	isSecureKafkaCluster);
	
		AdminUtils.createTopic(zkUtils, topic1, partitions1, replication1, topicConfig1,
	RackAwareMode.Enforced$.MODULE$);
		
		AdminUtils.createTopic(zkUtils, topic2, partitions2, replication2, topicConfig2,
	RackAwareMode.Enforced$.MODULE$);
		
		AdminUtils.createTopic(zkUtils, topic3, partitions3, replication3, topicConfig3,
	RackAwareMode.Enforced$.MODULE$);
		
		zkClient.close();
	
		return(0);
	}
	
	public void startAction(InputStream inputStream, OutputStream outputStream) {
		String AWSDBIP = "userdb.cfergfluhibr.us-east-1.rds.amazonaws.com";
		String AWSIP = "ec2-35-173-138-16.compute-1.amazonaws.com";
		String groupId = "testeAWS2";
		
		try {
			//Get start action
			startService(inputStream);
			
			//Prepare database connection
			Connection conn = null;
			boolean bd_ok = false;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/customerManagementDB", "admin", "projetoie");
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
			topicsList.add("T0_Metro");
			topicsList.add("T1_Uber");
			topicsList.add("T2_GIRA");
			consumer.subscribe(topicsList);
			
			//Prepare producer
			Properties propsConsumer = new Properties();
			propsConsumer.put("bootstrap.servers", AWSIP + ":9093," + AWSIP + ":9094," + AWSIP + ":9095");
			propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
			propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
			KafkaProducer<String, String> producer = new KafkaProducer<String, String>(propsConsumer);
			
			//Start consumer events
			try {
				System.out.println("handleRequest: Begin!!\n");
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					System.out.println("handleRequest: records = " + records.count() + "\n");
					for (ConsumerRecord<String, String> record : records) {
						System.out.println("handleRequest: topic = " + record.topic() +", partition = " + record.partition() + ", offset = " + record.offset() + ",customer = " + record.key() + ",message = " + record.value() +"\n");
						
						String message = record.value();
						String topic = record.topic();
						//long offset = record.offset();
						
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
				System.out.println("parseEvent(operator):" + operator + "\n");
			}
			
			JSONObject infojson = (JSONObject) extractedEvent.get("info");
			System.out.println("parseEvent(eventType):" + eventType + "\n");
			System.out.println("parseEvent(info):" + extractedEvent.get("info") + "\n");
			
			switch(eventType) {
				case "trip-cost":
					insertTripCost(conn, infojson, producer);
					break;
				case "new-operator":
					insertOperator(conn, operator, infojson, producer);
					break;
				case "new-service":
					insertService(conn, operator, infojson, producer);
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
	
	private void insertTripCost(Connection conn, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException{
		String baseCost = (String) infojson.get("baseCost");
		String token = (String) infojson.get("id");
		boolean hasPass = Boolean.parseBoolean((String) infojson.get("hasPass"));
		String planType = (String) infojson.get("planType");
		String tripId = (String) infojson.get("tripId");
		String operatorType = (String) infojson.get("operatorType");
		String operatorName = (String) infojson.get("operatorName");
		String timeStamp = (String) infojson.get("timeStamp");
		
		PreparedStatement s = conn.prepareStatement("insert into tripCost values(?,?,?,?,?,?,?,?)");
		s.setString(1, baseCost);
		s.setString(2, token);
		s.setBoolean(3, hasPass);
		s.setString(4, planType);
		s.setString(5, tripId);
		s.setString(6, operatorType);
		s.setString(7, operatorName);
		s.setString(8, timeStamp);
		s.executeUpdate();
		s.close();
	}

	private void insertOperator(Connection conn, String operator, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String operatorType = (String) infojson.get("operatorType");
		
		PreparedStatement s = conn.prepareStatement("insert into operator values(?)");
		s.setString(1, operatorType);
		s.executeUpdate();
		s.close();
		
	}
	
	private void insertService(Connection conn, String operator, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String name = (String) infojson.get("name");
		String serviceId = (String) infojson.get("serviceId");
		String price = (String) infojson.get("price");
		
		PreparedStatement s = conn.prepareStatement("insert into service values(?,?,?)");
		s.setString(1, name);
		s.setString(2, serviceId);
		s.setString(3, price);
		s.executeUpdate();
		s.close();
	}
	
	private void insertDiscount(Connection conn, String operator, JSONObject infojson, KafkaProducer<String, String> producer) throws SQLException {
		String name = (String) infojson.get("name");
		String serviceId = (String) infojson.get("serviceId");
		String discountId = (String) infojson.get("discountId");
		String value = (String) infojson.get("value");
		String beginAt = (String) infojson.get("beginAt");
		String endAt = (String) infojson.get("endAt");
		boolean appliesOnlyToPass = Boolean.parseBoolean((String) infojson.get("appliesOnlyToPass"));
				
		PreparedStatement s = conn.prepareStatement("insert into discount values(?,?,?,?,?,?,?)");
		s.setString(1, name);
		s.setString(2, serviceId);
		s.setString(3, discountId);
		s.setString(4, value);
		s.setString(5, beginAt);
		s.setString(6, endAt);
		s.setBoolean(7, appliesOnlyToPass);
		s.executeUpdate();
		s.close();
	}
       

	
	private void produceDebitEvent(Connection conn, String operator, KafkaProducer<String, String> producer, String token, String planType,
			String amount) {
		
		PreparedStatement s;
		ResultSet resultSet;
		try {
			System.out.println("produceDebitEvent : send event !\n");
			s = conn.prepareStatement("select * from userInfo where token = ?");
			s.setString(1, token);
			resultSet = s.executeQuery();
			while (resultSet.next()) {
	            String planType = resultSet.getString("planType");
		
		
	            String event = "{\"event\":{\"eventType\":\"trip-cost\", \"info\":{ " +
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

			s.close();
			resultSet.close();
			
		} catch (SQLException e) {
			System.out.println("Error : Produce Debit Event failed ->" + e.toString() + "\n");
		}      	
	}
	
	public String startService(InputStream inputStream) {
		JSONParser parser = new JSONParser();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String action = new String();

		try {
			JSONObject event = (JSONObject) parser.parse(reader);
			System.out.println("start:" + (String) event.toString() + "\n");
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("action") != null)
					action = (String) bodyjson.get("action");
			}
		}catch(IOException | ParseException ioe) {
			System.out.println("start:" + (String) ioe.toString() + "\n");
		}
		
		return action;
	}
}