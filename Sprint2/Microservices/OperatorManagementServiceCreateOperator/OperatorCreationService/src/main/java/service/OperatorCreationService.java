package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

public class OperatorCreationService implements RequestStreamHandler {

	private static String AWSIP = "ec2-35-171-83-59.compute-1.amazonaws.com";
	private Connection conn = null;
	static String AWSDBIP = "operatordb.ca14fw262vr6.us-east-1.rds.amazonaws.com";
	
	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {

		LambdaLogger logger = context.getLogger();

		try {
			// Prepare database connection
			boolean bd_ok = false;
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/operatordb", "admin", "projetoie");
			bd_ok = true;

			JSONParser parser = new JSONParser();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			JSONObject responseJson = new JSONObject();

			JSONObject event = (JSONObject) parser.parse(reader);

			String operator = new String();
			String operatorType = new String();
			String price = new String();
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("operator") != null)
					operator = (String) bodyjson.get("operator");
				if (bodyjson.get("operatorType") != null)
					operatorType = (String) bodyjson.get("operatorType");
				if (bodyjson.get("price") != null)
					price = (String) bodyjson.get("price");
			}

			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			String response = "Failure";

			if (bd_ok == true && event != null) {
				if(insertOperatorInDB(operator, operatorType, price, logger) == 0) {
					if(addOperatorTopic(operator, operatorType) == 0) {
						response = "Success";
					}
				}

			} else {
				logger.log("Failed: bd_ok=" + bd_ok + " event=" + event + "\n");
			}
			responseBody.put("message", response);
			// responseBody.put("message", validData);
			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "Operator Creation Service");
			responseJson.put("statusCode", 200);
			responseJson.put("headers", headerJson);
			responseJson.put("body", responseBody.toString());
			OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
			writer.write(responseJson.toString());
			writer.close();

		} catch (SQLException sqle) {
			logger.log("Error : " + sqle.toString());
		} catch (ClassNotFoundException e) {
			logger.log("Error : " + e.toString());
		} catch (IOException | ParseException ioe) {
			logger.log("Error:" + ioe.toString() + "\n");
		}

	}

	private int addOperatorTopic(String operatorName, String operatorType) {

		String zookeeperConnect = AWSIP + ":2181";
		int sessionTimeoutMs = 10 * 1000;
		int connectionTimeoutMs = 8 * 1000;

		String topic = operatorType.toUpperCase() + "_" + operatorName;
		int partitions = 3;
		int replication = 3;
		Properties topicConfig = new Properties();

		ZkClient zkClient = new ZkClient(zookeeperConnect, sessionTimeoutMs, connectionTimeoutMs,
				ZKStringSerializer$.MODULE$);

		boolean isSecureKafkaCluster = false;

		ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zookeeperConnect), isSecureKafkaCluster);

		AdminUtils.createTopic(zkUtils, topic, partitions, replication, topicConfig, RackAwareMode.Enforced$.MODULE$);

		zkClient.close();

		return (0);
	}
	
	private int insertOperatorInDB(String operator, String operatorType, String price, LambdaLogger logger) {
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
			logger.log("operator:" + (String) e.toString() + "\n");
			return 1;
		}
		return 0;
	}

}
