package Webservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.jws.WebService;

import org.I0Itec.zkclient.ZkClient;
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
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

//Service Implementation
@WebService(portName = "OperatorManagementServicePort",serviceName = "OperatorManagementService",targetNamespace="http://ec2-54-196-98-231.compute-1.amazonaws.com:9997/operatorManagementService",endpointInterface = "Webservice.OperatorManagementService")
public class OperatorManagementServiceImpl implements OperatorManagementService {
	static String AWSIP = "ec2-54-196-98-231.compute-1.amazonaws.com";
	static String AWSDBIP = "operatordb.cfergfluhibr.us-east-1.rds.amazonaws.com";
	static KafkaProducer<String, String> producer;
	static KafkaConsumer<String, String> consumer;
	static Connection conn = null;
	
	public OperatorManagementServiceImpl() {
	
	}
	private int addOperatorTopic(String operatorName, String operatorType){
		
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
			consumer = new KafkaConsumer<String, String>(props);
			List<String> topicsList = new ArrayList<String> ();
			topicsList.add("TripCosts");
			topicsList.add("OperatorProvision");
			consumer.subscribe(topicsList);
			
			//Prepare producer
			Properties propsConsumer = new Properties();
			propsConsumer.put("bootstrap.servers", AWSIP + ":9093," + AWSIP + ":9094," + AWSIP + ":9095");
			propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
			propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
			producer = new KafkaProducer<String, String>(propsConsumer);
			
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
		
		JSONObject infojson = (JSONObject) extractedEvent.get("info");
		System.out.println("processEvent(eventType):" + eventType + "\n");
		System.out.println("processEvent(info):" + extractedEvent.get("info") + "\n");
		
		switch(eventType) {
			case "trip-cost":
				processTripCostEventFromJson(infojson);
				break;
			case "new-operator":
				String operator = (String) extractedEvent.get("operator");
				System.out.println("processEvent(operator):" + operator + "\n");
				createOperatorFromJson(operator, infojson);
				break;
			case "new-discount":
				JSONArray operators = (JSONArray) extractedEvent.get("operator");
				System.out.println("processEvent(operator):" + operators + "\n");
				createDiscountFromJson(operators, infojson);
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
		String token = (String) infojson.get("token");
		
		processTripCostEvent(baseCostString, operatorName, timeStamp, planType, token);
	}
	
	
	public int processTripCostEvent(String baseCostString, String operatorName, String timeStamp, String planType,
			String token) {
		float discount = getDiscount(planType, timeStamp, operatorName);
		
		if(discount == -1) {
			return 1;
		}
		float baseCost;
		if(baseCostString.equals("null")) {
			System.out.println("processTripCostEvent: basecost is null -> " + baseCostString + "\n");
			baseCost = getOperatorBaseCost(operatorName);
		}else {
			System.out.println("processTripCostEvent: basecost is -> " + baseCostString + "\n");
			baseCost = Float.valueOf(baseCostString);
		}

		float debitAmount = baseCost * discount;
			
		produceDebitEvent(token, planType, debitAmount);
		
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
				System.out.println("Got operator cost ->" + resultSet.getFloat("price") + "\n");
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
		PreparedStatement s;
		ResultSet resultSet;
		float discountValue = 1;
		try {
			s = conn.prepareStatement("select value from discount inner join operator_discount on discount.discountId = operator_discount.discountId inner join discount_planType on discount.discountId = discount_planType.discountId where beginAt >= ? and endAt <= ? and operatorName = ? and plan=?");
			s.setTimestamp(1, java.sql.Timestamp.valueOf(timestamp));
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setString(3, operatorName);
			s.setString(4, planType);
			resultSet = s.executeQuery();
			float discountValueAux = 0;
			while (resultSet.next()) {
				float value = resultSet.getInt("value")/100;
				if(value > discountValueAux) {
					System.out.println("getDiscount : Discount found = " + value + "\n");
					discountValueAux = value;
				}
		    }
		    
		    s.close();
		    resultSet.close();
		    if(discountValueAux != 0){
		    	System.out.println("getDiscount : Best Discount found = " + discountValueAux + "\n");
		    	discountValue = discountValueAux;
		    }
		} catch (SQLException e) {
			System.out.println("Error : Looking for discount ->" + e.toString() + "\n");
			return -1;
		}
		
		return discountValue;
	}
	
	private void createOperatorFromJson(String operator, JSONObject infojson){
		String operatorType = (String) infojson.get("operatorType");
		String price = (String) infojson.get("baseCost");
		
		createOperator(operator, operatorType, price);
	}
	
	@Override
	public String createOperator(String operator, String operatorType, String price) {
		if(insertOperatorInDB(operator, operatorType, price) == 0) {
			if(addOperatorTopic(operator, operatorType) == 0) {
				return "Success";
			}
		}
		
		return "Failure";
	}
	/*
	public void createDiscount(ArrayList<String> operators, String discountId,String discountName, String value, String beginAt, String endAt, ArrayList<String> planTypes)
	{
		insertDiscountInDB(operators, discountId, discountName, value, beginAt, endAt, planTypes);
	}*/
	
	private int insertOperatorInDB(String operator, String operatorType, String price) {
		PreparedStatement s;
		try {
			s = conn.prepareStatement("insert into operator values(?,?,?)");
			s.setString(1, operator);
			s.setString(2, operatorType);
			if(price.equals("null")) {
				s.setNull(3,Types.FLOAT);
			}
			else {
				s.setFloat(3, Float.parseFloat(price));
			}
			s.executeUpdate();
			s.close();
		} catch (SQLException e) {
			System.out.println("operator:" + (String) e.toString() + "\n");
			return 1;
		}
		return 0;
	}
	
	private void createDiscountFromJson(JSONArray operators, JSONObject infojson){
		String discountId = (String) infojson.get("discountId");
		String discountName = (String) infojson.get("discountName");
		String value = (String) infojson.get("value");
		String beginAt = (String) infojson.get("beginAt");
		String endAt = (String) infojson.get("endAt");
		JSONArray planTypes = (JSONArray) infojson.get("appliesToPlanType");
		
		insertDiscountInDB(operators, discountId, discountName, value, beginAt, endAt, planTypes);
	}
	
	private void insertDiscountInDB(JSONArray operators, String discountId, String discountName, String value,
			String beginAt, String endAt, JSONArray planTypes) {
		PreparedStatement s;
		try {
			s = conn.prepareStatement("insert into discount values(?,?,?,?,?,?,?)");
			s.setString(1, discountId);
			s.setString(2, discountName);
			s.setInt(3, Integer.parseInt(value));
			s.setTimestamp(4, java.sql.Timestamp.valueOf(beginAt));
			s.setTimestamp(5, java.sql.Timestamp.valueOf(endAt));
			s.executeUpdate();
			s.close();
		} catch (SQLException e) {
			System.out.println("insertDiscount:" + (String) e.toString() + "\n");
		}
		
		
        Iterator<String> operatorsIterator = operators.iterator();
        while (operatorsIterator.hasNext()) {
        	String operator = operatorsIterator.next();
        	System.out.println("Operator:" + operator);
            
            try {
				s = conn.prepareStatement("insert into operator_discount values(?,?)");
	    		s.setString(1, operator);
	    		s.setString(2, discountId);
	    		s.executeUpdate();
	    		s.close();
			} catch (SQLException e) {
				System.out.println("operator_discount:" + (String) e.toString() + "\n");
			}

    		
    		for (int i = 0; i < planTypes.size(); i++) {
    			String plan = (String) planTypes.get(i);
	          	System.out.println("PlanType:" + plan);
	              
	          	try {
					s = conn.prepareStatement("insert into discount_planType values(?,?)");
		      		s.setString(1, discountId);
		      		s.setString(2, plan);
		      		s.executeUpdate();
		      		s.close();
				} catch (SQLException e) {
					System.out.println("discount_planType:" + (String) e.toString() + "\n");
				}
			}
        }
	}
	
	private void produceDebitEvent(String token, String planType, float amount) {
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
	
	@Override
	public String createTripCost(String baseCostString, String operatorName, String timeStamp, String planType,
			String token) {
		if(processTripCostEvent(baseCostString, operatorName, timeStamp, planType, token) == 0) {
			return "Success";
		}
		
		return "Failure";
	}

}