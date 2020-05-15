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
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class LoadAccountService implements RequestStreamHandler {

	@Override
	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
		LambdaLogger logger = context.getLogger();

		try {
			String AWSDBIP = "userdb.ca14fw262vr6.us-east-1.rds.amazonaws.com";
			Connection conn = null;

			// Prepare database connection
			boolean bd_ok = false;
			Class.forName("com.mysql.cj.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/userdb", "admin", "projetoie");
			bd_ok = true;

			JSONParser parser = new JSONParser();
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			JSONObject responseJson = new JSONObject();

			JSONObject event = (JSONObject) parser.parse(reader);
			
			String token = new String();
			String amount = new String();
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("id") != null)
					token = (String) bodyjson.get("id");
				if (bodyjson.get("amount") != null)
					amount = (String) bodyjson.get("amount");
			}

			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			ResultSet resultSet;
			int balance;

			if (bd_ok == true && event != null) {
				PreparedStatement s;
				int totalAmount = 0;
				s = conn.prepareStatement("select * from userBalance where token = ?");
				s.setString(1, token);
				resultSet = s.executeQuery();
				while (resultSet.next()) {
					logger.log("Found entry of this user!\n");
					balance = resultSet.getInt("balance");
					totalAmount = Integer.parseInt(amount) + balance;
					s = conn.prepareStatement("update userBalance set balance = ? where token = ?");
					s.setInt(1, totalAmount);
					s.setString(2, token);
					resultSet = s.executeQuery();
					
					s.close();
					resultSet.close();
					
					break;
				}
				conn.close();

				responseBody.put("message","Success! Account loaded: " + totalAmount+"€");
			} else {
				logger.log("Failed: bd_ok=" + bd_ok + " event=" + event + "\n");
				responseBody.put("message","Failure");
			}

			//responseBody.put("message", validData);
			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "Load Account Service");
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

}
