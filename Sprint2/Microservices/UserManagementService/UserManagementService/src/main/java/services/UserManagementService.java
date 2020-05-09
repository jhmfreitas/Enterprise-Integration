package services;

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
import java.util.Properties;

import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class UserManagementService implements RequestStreamHandler {
	String AWSDBIP = "userdb.cfergfluhibr.us-east-1.rds.amazonaws.com";
	String AWSIP = "ec2-54-84-79-209.compute-1.amazonaws.com";
	String AWSOperatorIP = "ec2-54-84-79-209.compute-1.amazonaws.com";
	String groupId = "UserManagementService";
	KafkaProducer<String, String> producer;
	Connection conn = null;
	Connection connOperatorDB = null;
	KafkaConsumer<String, String> consumer;
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
		LambdaLogger logger = context.getLogger();
		
		try {
			//Get start action
			startService(logger, inputStream);
			
			//Prepare database connection
			
			boolean bd_ok = false;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/customerManagementDB", "admin", "projetoie");
				bd_ok = true;
			} catch (SQLException sqle) {
				logger.log("Error : " + sqle.toString());
			} catch (ClassNotFoundException e) {
				logger.log("Error : " + e.toString());
			}
			
			boolean bdOperator_ok = false;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				connOperatorDB = DriverManager.getConnection("jdbc:mysql://" + AWSOperatorIP + ":3306/operatorManagementDB", "admin", "projetoie");
				bdOperator_ok = true;
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
			
			
			//Collect operator names and subscribe to topics
			subscribeOperatorTopics();
			
			
			//Prepare producer
			Properties propsConsumer = new Properties();
			propsConsumer.put("bootstrap.servers", AWSIP + ":9093," + AWSIP + ":9094," + AWSIP + ":9095");
			propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
			propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
			producer = new KafkaProducer<String, String>(propsConsumer);
			
			//Start consumer events
			try {
				logger.log("handleRequest: Begin!!\n");
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					logger.log("handleRequest: records = " + records.count() + "\n");
					for (ConsumerRecord<String, String> record : records) {
						logger.log("handleRequest: topic = " + record.topic() +", partition = " + record.partition() + ", offset = " + record.offset() + ",customer = " + record.key() + ",message = " + record.value() +"\n");
						
						String message = record.value();
						String topic = record.topic();
						
						JSONParser parser = new JSONParser();
						JSONObject extractedEvent = (JSONObject) ((JSONObject) parser.parse(message)).get("event");
						
						if(extractedEvent != null && bd_ok && bdOperator_ok) {
							processEvent(extractedEvent, topic, logger, conn, producer);	
						}
						else {
							logger.log("Error : extractedEvent is " + extractedEvent + " and BDConnnection=" + bd_ok + " and operatorDBConnection=" + bdOperator_ok + "\n");
						}
					}
					// Commit Current Offset
					consumer.commitSync();
				}
			}catch (CommitFailedException e) {
				logger.log("Error : Commit Failed -> " + e.toString()+ "\n");
			}finally {
				consumer.close();
				try {
					conn.close();
				} catch (SQLException e) {
					logger.log("Error : " + e.toString()+ "\n");
				}
			}
		} catch (Exception exc) {
			logger.log("Error : " + exc.toString()+ "\n");
		}
	}

	private void subscribeOperatorTopics() {
		PreparedStatement s;
		ResultSet resultSet;
		List<String> topicsList = new ArrayList<String> ();
		try {
			s = connOperatorDB.prepareStatement("select operatorType,operatorName from operator");
			resultSet = s.executeQuery();
			while (resultSet.next()) {
				String operatorType = resultSet.getString("operatorType");
				String operatorName = resultSet.getString("operatorName");
				
				String topicName = operatorType + "_" + operatorName;
				System.out.println("Subscribing to topic ->" + topicName + "\n");
				topicsList.add(topicName);
		    }
		    
		    s.close();
		    resultSet.close();
		    
		} catch (SQLException e) {
			System.out.println("Error : subscribeOperatorTopics ->" + e.toString() + "\n");
		}

		consumer.subscribe(topicsList);
	}
	
	public void processEvent(JSONObject extractedEvent, String topic, LambdaLogger logger, Connection conn, KafkaProducer<String, String> producer) {

		try {
			String eventType = (String) extractedEvent.get("eventType");
			String operator = (String) extractedEvent.get("operator");
			if(operator != null) {
				//Validation needed because of new-user events
				logger.log("parseEvent(operator):" + operator + "\n");
			}
			JSONObject infojson = (JSONObject) extractedEvent.get("info");
			logger.log("parseEvent(eventType):" + eventType + "\n");
			logger.log("parseEvent(info):" + extractedEvent.get("info") + "\n");
			
			switch(eventType) {
				case "t0-check-in":
					insertT0Info(conn, operator, eventType, infojson, logger, producer);
					break;
				case "t0-check-out":
					insertT0Info(conn, operator, eventType, infojson, logger, producer);
					break;
				case "t1":
					insertT1Info(conn, operator, infojson, logger, producer);
					break;
				case "t2":
					insertT2Info(conn, operator, infojson, logger, producer);
					break;
				case "new-user":
					insertUser(conn, infojson, logger);
					break;
				default:
					logger.log("parseEvent(eventType): Error : There is not such type of event:" + eventType + "\n");
			}
			
			
		}catch (SQLException e) {
			logger.log("parseMessage:" + (String) e.toString() + "\n");
		}

	}

	private void insertUser(Connection conn, JSONObject infojson, LambdaLogger logger) throws SQLException {
		String token = (String) infojson.get("id");
		String email = (String) infojson.get("email");
		String planType = (String) infojson.get("planType");
		String firstName = (String) infojson.get("firstName");
		String lastName = (String) infojson.get("lastName");
		String balance = (String) infojson.get("balance");
		
		PreparedStatement s = conn.prepareStatement("insert into userInfo values(?,?,?,?,?)");
		s.setString(1, token);
		s.setString(2, email);
		s.setString(3, firstName);
		s.setString(4, lastName);
		s.setString(5, planType);
		s.executeUpdate();
		s.close();
		
		s = conn.prepareStatement("insert into userBalance values(?,?)");
		s.setString(1, token);
		s.setInt(2, Integer.parseInt(balance));
		s.executeUpdate();
		s.close();
	}

	private void insertT2Info(Connection conn, String operator, JSONObject infojson, LambdaLogger logger, KafkaProducer<String, String> producer) throws SQLException {
		String token = (String) infojson.get("Token");
		String price = (String) infojson.get("Price");
		String time = (String) infojson.get("Time");
		String timestamp = (String) infojson.get("Timestamp");
		String tripId = getUniqueID(logger, token, timestamp);
		
		insertHistory(conn, operator, token, timestamp, logger, tripId);
		
		PreparedStatement s = conn.prepareStatement("insert into T2_History values(?,?,?,?)");
		s.setString(1, tripId);
		s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
		s.setLong(3, Long.parseLong(time));
		s.setFloat(4, Float.parseFloat(price));
		s.executeUpdate();
		s.close();
		
		produceTripCostEvent(conn, operator, producer, price, "t2", token, timestamp, tripId, logger);
	}

	private void insertT1Info(Connection conn, String operator, JSONObject infojson, LambdaLogger logger, KafkaProducer<String, String> producer) throws SQLException {
		String token = (String) infojson.get("Token");
		String price = (String) infojson.get("Price");
		String timestamp = (String) infojson.get("Timestamp");
		String tripId = getUniqueID(logger, token, timestamp);
		
		insertHistory(conn, operator, token, timestamp, logger, tripId);
		
		PreparedStatement s = conn.prepareStatement("insert into T1_History values(?,?,?)");
		s.setString(1, tripId);
		s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
		s.setFloat(3, Float.parseFloat(price));
		s.executeUpdate();
		s.close();
		
		produceTripCostEvent(conn, operator, producer, price, "t1", token, timestamp, tripId, logger);
	}

	private void insertT0Info(Connection conn, String operator, String eventType, JSONObject infojson, LambdaLogger logger, KafkaProducer<String, String> producer) throws SQLException {
		String token = (String) infojson.get("Token");
		String station = (String) infojson.get("Station");
		String timestamp = (String) infojson.get("Timestamp");
		String tripId = getUniqueID(logger, token, timestamp);
		
		insertHistory(conn, operator, token, timestamp, logger, tripId);
		
		if(eventType.equals("t0-check-in")) {
			PreparedStatement s = conn.prepareStatement("insert into T0_History values(?,?,?,?)");
			s.setString(1, tripId);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setString(3, station);
			s.setBoolean(4, true);
			s.executeUpdate();
			s.close();
			
			produceTripCostEvent(conn, operator, producer, "null", "t0", token, timestamp, tripId, logger);
		}
		else if(eventType.equals("t0-check-out")) {
			PreparedStatement s = conn.prepareStatement("insert into T0_History values(?,?,?,?)");
			s.setString(1, tripId);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setString(3, station);
			s.setBoolean(4, false);
			s.executeUpdate();
			s.close();
		}
		else {
			logger.log("Error : T0 type doesn't exist ->" + eventType + "\n");
		}
	}

	private void produceTripCostEvent(Connection conn, String operator, KafkaProducer<String, String> producer, String cost, String operatorType, String token,
			String timestamp, String tripId, LambdaLogger logger) {
		
		PreparedStatement s;
		ResultSet resultSet;
		try {
			logger.log("produceTripCostEvent : send event !\n");
			s = conn.prepareStatement("select * from userInfo where token = ?");
			s.setString(1, token);
			resultSet = s.executeQuery();
			while (resultSet.next()) {
	            String planType = resultSet.getString("planType");
	            
	            String event = "{\"event\":{\"eventType\":\"trip-cost\", \"info\":{ " +
	    				"\"cost\": \"" + cost + "\", "+
	    				"\"token\": \" " + token + "\", "+
	    				"\"planType\": \"" + planType +"\", "+
	    				"\"operatorName\": \""+operator+"\", "+
	    				"\"timeStamp\": \""+timestamp+"\" "+
	    			"}"+
	    		"}"+
	    		"}";
	    		
	    		ProducerRecord<String, String> record = new ProducerRecord<>("TripCosts", "TripCostsKey", event);
	    		
	    		
	    		producer.send(record);
	    		logger.log("produceTripCostEvent : Sent -> " + event + "\n");
	        }
	        
	        s.close();
	        resultSet.close();
		} catch (SQLException e) {
			logger.log("Error : Produce TripCost Event failed ->" + e.toString() + "\n");
		}
	}

	private String getUniqueID(LambdaLogger logger, String token, String timestamp) {
		Date date;
		String datetime = "";
		try {
			date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timestamp);
			SimpleDateFormat ft = new SimpleDateFormat("yyMMddhhmmssMs");
	        datetime = ft.format(date);
		} catch (java.text.ParseException e) {
			logger.log("Error : " + e.toString());
		}
		return token + "-" + datetime;
	}

	private void insertHistory(Connection conn, String operator, String token, String timestamp, LambdaLogger logger, String tripId) throws SQLException {
		PreparedStatement s = conn.prepareStatement("insert into history values(?,?,?,?)");
		s.setString(1, tripId);
		s.setString(2, token);
		s.setString(3, operator);
		s.setTimestamp(4, java.sql.Timestamp.valueOf(timestamp));
		s.executeUpdate();
		s.close();
	}
	
	public String startService(LambdaLogger logger, InputStream inputStream) {
		JSONParser parser = new JSONParser();
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String action = new String();

		try {
			JSONObject event = (JSONObject) parser.parse(reader);
			logger.log("start:" + (String) event.toString() + "\n");
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("action") != null)
					action = (String) bodyjson.get("action");
			}
		}catch(IOException | ParseException ioe) {
			logger.log("start:" + (String) ioe.toString() + "\n");
		}
		
		return action;
	}
}
