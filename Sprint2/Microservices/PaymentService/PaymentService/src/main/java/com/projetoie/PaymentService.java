package com.projetoie;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class PaymentService implements RequestStreamHandler {
	String AWSDBIP = "userdb.ca14fw262vr6.us-east-1.rds.amazonaws.com";
	String AWSIP = "ec2-3-90-151-30.compute-1.amazonaws.com";
	String CamundaIP = "ec2-54-175-77-51.compute-1.amazonaws.com";
	String groupId = "PaymentService";
	Connection conn = null;
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
			
			//Prepare consumer
			Properties props = new Properties();
			props.put("bootstrap.servers", AWSIP + ":9092," + AWSIP + ":9093," + AWSIP + ":9094");
			props.put("group.id", groupId);
			props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
			props.put("auto.commit.offset", "false"); // to commit manually
			consumer = new KafkaConsumer<String, String>(props);
			List<String> topicsList = new ArrayList<String> ();
			topicsList.add("Debit");
			consumer.subscribe(topicsList);

			//Start consumer events
			try {
				logger.log("handleRequest: Begin!!\n");
				while (true) {
					ConsumerRecords<String, String> records = consumer.poll(100);
					//logger.log("handleRequest: records = " + records.count() + "\n");
					for (ConsumerRecord<String, String> record : records) {
						logger.log("handleRequest: topic = " + record.topic() +", partition = " + record.partition() + ", offset = " + record.offset() + ",customer = " + record.key() + ",message = " + record.value() +"\n");
						
						String message = record.value();
						
						JSONParser parser = new JSONParser();
						JSONObject extractedEvent = (JSONObject) ((JSONObject) parser.parse(message)).get("event");
						
						if(extractedEvent != null && bd_ok) {
							processEvent(extractedEvent, logger);	
						}
						else {
							logger.log("Error : extractedEvent is " + extractedEvent + " and BDConnnection=" + bd_ok + "\n");
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
	
	public void processEvent(JSONObject extractedEvent, LambdaLogger logger) {
		String eventType = (String) extractedEvent.get("eventType");
		logger.log("parseEvent(eventType):" + eventType + "\n");
		JSONObject infojson = (JSONObject) extractedEvent.get("info");
		logger.log("parseEvent(info):" + extractedEvent.get("info") + "\n");
		
		switch(eventType) {
			case "debit":
				processDebitEvent(infojson, logger);
				break;
			default:
				logger.log("parseEvent(eventType): Error : There is not such type of event:" + eventType + "\n");
		}

	}

	private void processDebitEvent(JSONObject infojson, LambdaLogger logger) {
		String id = (String) infojson.get("id");
		String planType = (String) infojson.get("planType");
		String amount = (String) infojson.get("amount");
		
		if(planType.equals("pre-paid")) {
			PreparedStatement s;
			ResultSet resultSet;
			try {
				logger.log("Processing debit event\n");
				s = conn.prepareStatement("select balance from userBalance where id = ?");
				s.setString(1, id);
				resultSet = s.executeQuery();
				while (resultSet.next()) {
		            String previousAccountBalance = resultSet.getString("balance");
		            logger.log("User Balance -> " + previousAccountBalance + "\n");
		            logger.log("Trip Cost -> " + Float.parseFloat(amount) + "\n");
		            float total = Float.parseFloat(previousAccountBalance) - Float.parseFloat(amount);

		            debitAmount(logger, id, total);
		            
		            if(total <= 0 && Float.parseFloat(previousAccountBalance) > 0) {
	            		//User had positive balance but now it is negative or equal to 0
	            		//Dunning Process Starts
	            		String email = getUserEmail(logger, id);
	            		if(email != null) {
	            			startDunningProcess(email, id,logger);
	            		}
	            		else {
	            			logger.log("Email is null!\n");
	            		}
		            }     
		        }
				s.close();
				//conn.close();
			} catch (SQLException e) {
				logger.log("processDebitEvent: Error:" + e.toString() + "\n");
			}
		}
	}

	private String getUserEmail(LambdaLogger logger, String id) throws SQLException {
		PreparedStatement statementInfo;
		String email = null;
		ResultSet resultSetInfo;
		logger.log("Getting Email!\n");
		statementInfo = conn.prepareStatement("select email from userInfo where id = ?");
		statementInfo.setString(1, id);
		resultSetInfo = statementInfo.executeQuery();
		while (resultSetInfo.next()) {
		    email = resultSetInfo.getString("email");
		    logger.log("Result(email) -> " + email + "\n");
		}
		statementInfo.close();
		return email;
	}

	private void debitAmount(LambdaLogger logger, String id, float total) throws SQLException {
		PreparedStatement statementBalance;
		statementBalance = conn.prepareStatement("update userBalance set balance = ? where id = ?");
		statementBalance.setFloat(1, total);
		statementBalance.setString(2, id);
		statementBalance.executeUpdate();
		statementBalance.close();
		
		logger.log("Debited Account of user: " + id + "   New Account Balance: " + total + "\n");
	}
	
	private void startDunningProcess(String email, String id, LambdaLogger logger) {
		logger.log("Start Dunning Process!\n");
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost postRequest = new HttpPost("http://"+CamundaIP+":8080/engine-rest/process-definition/key/dunning-process/start");
		postRequest.addHeader("content-type", "application/json");
		String query = "{\"variables\": {\"fromEmail\": {\"value\":\"Maas Operator <mailgun@sandbox45a4ec4aa29243c78be39889e9338d42.mailgun.org>\"},\"email\":{\"value\":\"" + email + "\"},\"id\": {\"value\":\""+ id + "\"} },\"businessKey\": \""+ id+ "\"}";
		logger.log("Request: " + query + "\n");
		StringEntity Entity;
		try {
			Entity = new StringEntity(query);
			postRequest.setEntity(Entity);
			HttpEntity base = postRequest.getEntity();
			HttpResponse response = httpClient.execute(postRequest);
			int statusCode = response.getStatusLine().getStatusCode();
			logger.log("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
		} catch (UnsupportedEncodingException e) {
			logger.log("Error: " + e.toString() + "\n");
		} catch (ClientProtocolException e) {
			logger.log("Error: " + e.toString() + "\n");
		} catch (IOException e) {
			logger.log("Error: " + e.toString() + "\n");
		} finally {
			httpClient.getConnectionManager().shutdown();
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
