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

public class UserManagementServiceRemoveUser implements RequestStreamHandler {

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
			logger.log("event: " + event + "\n");
			String nif = new String();
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				logger.log("body: " + bodyjson + "\n");
				if (bodyjson.get("nif") != null) {
					nif = (String) bodyjson.get("nif");
					logger.log("nif: " + nif + "\n");
				}
			}

			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			
			if (bd_ok == true && event != null) {
				int resultSet;
				
				PreparedStatement s;
				s = conn.prepareStatement("delete from userInfo where nif = ?");
				s.setString(1, nif);
				resultSet = s.executeUpdate();
				s.close();
				
				logger.log("Delete from userInfo:" + resultSet + "\n");
				
				s = conn.prepareStatement("delete from userBalance where nif = ?");
				s.setString(1, nif);
				resultSet = s.executeUpdate();
				s.close();
				
				logger.log("Delete from userBalance:" + resultSet + "\n");
				
				conn.close();

				responseBody.put("message","Success");
			} else {
				logger.log("Failed: bd_ok=" + bd_ok + " event=" + event + "\n");
				responseBody.put("message","Failure");
			}

			//responseBody.put("message", validData);
			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "Remove User");
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
