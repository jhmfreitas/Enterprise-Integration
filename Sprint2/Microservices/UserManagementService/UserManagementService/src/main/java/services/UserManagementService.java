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
	String AWSDBIP = "userdb.ca14fw262vr6.us-east-1.rds.amazonaws.com";
	String AWSIP = "ec2-52-202-49-153.compute-1.amazonaws.com";
	//String AWSOperatorIP = "ec2-54-84-79-209.compute-1.amazonaws.com";
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
				conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/userdb", "admin", "projetoie");
				bd_ok = true;
			} catch (SQLException sqle) {
				logger.log("Error : " + sqle.toString());
			} catch (ClassNotFoundException e) {
				logger.log("Error : " + e.toString());
			}
			
			boolean bdOperator_ok = false;
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
				connOperatorDB = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/operatordb", "admin", "projetoie");
				bdOperator_ok = true;
			} catch (SQLException sqle) {
				logger.log("Error : " + sqle.toString());
			} catch (ClassNotFoundException e) {
				logger.log("Error : " + e.toString());
			}
			
			//Prepare consumer
			Properties props = new Properties();
			props.put("bootstrap.servers", AWSIP + ":9092," + AWSIP + ":9093," + AWSIP + ":9094");
			props.put("group.id", groupId);
			props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("auto.commit.offset", "false"); // to commit manually
			consumer = new KafkaConsumer<String, String>(props);
			
			
			//Collect operator names and subscribe to topics
			subscribeOperatorTopics(logger);
			
			
			//Prepare producer
			Properties propsConsumer = new Properties();
			propsConsumer.put("bootstrap.servers", AWSIP + ":9092," + AWSIP + ":9093," + AWSIP + ":9094");
			propsConsumer.put("key.serializer","org.apache.kafka.common.serialization.StringSerializer");
			propsConsumer.put("value.serializer","org.apache.kafka.common.serialization.StringSerializer");
			producer = new KafkaProducer<String, String>(propsConsumer);
			
			//Start consumer events
			try {
				logger.log("handleRequest: Begin!!\n");
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					//logger.log("handleRequest: records = " + records.count() + "\n");
					for (ConsumerRecord<String, String> record : records) {
						logger.log("handleRequest: topic = " + record.topic() +", partition = " + record.partition() + ", offset = " + record.offset() + ",customer = " + record.key() + ",message = " + record.value() +"\n");
						
						String message = record.value();
						String topic = record.topic();
						
						JSONParser parser = new JSONParser();
						JSONObject extractedEvent = (JSONObject) ((JSONObject) parser.parse(message)).get("event");
						
						if(extractedEvent != null && bd_ok && bdOperator_ok) {
							processEvent(extractedEvent, topic, logger);	
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

	private void subscribeOperatorTopics(LambdaLogger logger) {
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
				logger.log("Subscribing to topic ->" + topicName + "\n");
				topicsList.add(topicName);
		    }
		    
		    s.close();
		    resultSet.close();
		    
		} catch (SQLException e) {
			logger.log("Error : subscribeOperatorTopics ->" + e.toString() + "\n");
		}

		consumer.subscribe(topicsList);
	}
	
	public void processEvent(JSONObject extractedEvent, String topic, LambdaLogger logger) {

		try {
			String eventType = (String) extractedEvent.get("eventType");
			String operator = (String) extractedEvent.get("operator");
			if(operator != null) {
				logger.log("parseEvent(operator):" + operator + "\n");
			}
			JSONObject infojson = (JSONObject) extractedEvent.get("info");
			logger.log("parseEvent(eventType):" + eventType + "\n");
			logger.log("parseEvent(info):" + extractedEvent.get("info") + "\n");
			
			switch(eventType) {
				case "t0-check-in":
					insertT0Info(operator, eventType, infojson, logger);
					break;
				case "t0-check-out":
					insertT0Info(operator, eventType, infojson, logger);
					break;
				case "t1":
					insertT1Info(operator, infojson, logger);
					break;
				case "t2":
					insertT2Info(operator, infojson, logger);
					break;
				default:
					logger.log("parseEvent(eventType): Error : There is not such type of event:" + eventType + "\n");
			}
			
			
		}catch (SQLException e) {
			logger.log("parseMessage:" + (String) e.toString() + "\n");
		}

	}

	private void insertT2Info(String operator, JSONObject infojson, LambdaLogger logger){
		String id = (String) infojson.get("Id");
		String price = (String) infojson.get("Price");
		String time = (String) infojson.get("Time");
		String timestamp = (String) infojson.get("Timestamp");
		String tripId = getUniqueID(logger, id, timestamp);
		
		insertHistory(operator, id, timestamp, logger, tripId);
		
		PreparedStatement s;
		try {
			s = conn.prepareStatement("insert into T2_History values(?,?,?,?)");
			s.setString(1, tripId);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setLong(3, Long.parseLong(time));
			s.setFloat(4, Float.parseFloat(price));
			s.executeUpdate();
			s.close();
		} catch (SQLException e) {
			logger.log("Error : insertT2Info ->" + e.toString() + "\n");
		}

		
		produceTripCostEvent(operator, price, "t2", id, timestamp, tripId, logger);
	}

	private void insertT1Info(String operator, JSONObject infojson, LambdaLogger logger){
		String id = (String) infojson.get("Id");
		String price = (String) infojson.get("Price");
		String timestamp = (String) infojson.get("Timestamp");
		String tripId = getUniqueID(logger, id, timestamp);
		
		insertHistory(operator, id, timestamp, logger, tripId);
		
		PreparedStatement s;
		try {
			s = conn.prepareStatement("insert into T1_History values(?,?,?)");
			s.setString(1, tripId);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setFloat(3, Float.parseFloat(price));
			s.executeUpdate();
			s.close();
		} catch (SQLException e) {
			logger.log("Error : insertT1Info ->" + e.toString() + "\n");
		}

		
		produceTripCostEvent(operator, price, "t1", id, timestamp, tripId, logger);
	}

	private void insertT0Info(String operator, String eventType, JSONObject infojson, LambdaLogger logger) throws SQLException {
		String id = (String) infojson.get("Id");
		String station = (String) infojson.get("Station");
		String timestamp = (String) infojson.get("Timestamp");
		String tripId = getUniqueID(logger, id, timestamp);
		
		insertHistory(operator, id, timestamp, logger, tripId);
		
		if(eventType.equals("t0-check-in")) {
			PreparedStatement s = conn.prepareStatement("insert into T0_History values(?,?,?,?)");
			s.setString(1, tripId);
			s.setTimestamp(2, java.sql.Timestamp.valueOf(timestamp));
			s.setString(3, station);
			s.setBoolean(4, true);
			s.executeUpdate();
			s.close();
			
			produceTripCostEvent(operator, "null", "t0", id, timestamp, tripId, logger);
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

	private void produceTripCostEvent(String operator, String cost, String operatorType, String id,
			String timestamp, String tripId, LambdaLogger logger) {
		
		PreparedStatement s;
		ResultSet resultSet;
		try {
			logger.log("produceTripCostEvent : send event !\n");
			s = conn.prepareStatement("select planType from userInfo where id = ?");
			s.setString(1, id);
			resultSet = s.executeQuery();
			while (resultSet.next()) {
	            String planType = resultSet.getString("planType");
	            
	            String event = "{\"event\":{\"eventType\":\"trip-cost\", \"info\":{ " +
	    				"\"cost\": \"" + cost + "\", "+
	    				"\"id\": \"" + id + "\", "+
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

	private String getUniqueID(LambdaLogger logger, String id, String timestamp) {
		Date date;
		String datetime = "";
		try {
			date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(timestamp);
			SimpleDateFormat ft = new SimpleDateFormat("yyMMddhhmmssMs");
	        datetime = ft.format(date);
		} catch (java.text.ParseException e) {
			logger.log("Error : " + e.toString());
		}
		return id + "-" + datetime;
	}

	private void insertHistory(String operator, String id, String timestamp, LambdaLogger logger, String tripId){
		PreparedStatement s;
		try {
			s = conn.prepareStatement("insert into history values(?,?,?,?)");
			s.setString(1, tripId);
			s.setString(2, id);
			s.setString(3, operator);
			s.setTimestamp(4, java.sql.Timestamp.valueOf(timestamp));
			s.executeUpdate();
			s.close();
		} catch (SQLException e) {
			logger.log("Error : InsertHistory ->" + e.toString() + "\n");
		}

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
