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

public class UserBlacklist implements RequestStreamHandler{

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
			
			String id = new String();
			String operation = new String();
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("id") != null)
					id = (String) bodyjson.get("id");
				if (bodyjson.get("operation") != null)
					operation = (String) bodyjson.get("operation");
			}

			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			int resultSet;

			if (bd_ok == true && event != null && operation.equals("blacklist")) {
				PreparedStatement s;
				s = conn.prepareStatement("update userBalance set blackListed = ? where id = ?");
				s.setBoolean(1, true);
				s.setString(2, id);
				resultSet = s.executeUpdate();

				s.close();
				conn.close();
				
				responseBody.put("message", "Success");
			} 
			else if(bd_ok == true && event != null && operation.equals("undo-blacklist")){
				PreparedStatement s;
				s = conn.prepareStatement("update userBalance set blackListed = ? where id = ?");
				s.setBoolean(1, false);
				s.setString(2, id);
				resultSet = s.executeUpdate();

				s.close();
				conn.close();
				
				responseBody.put("message", "Success");
			}else {
				logger.log("Failed: bd_ok=" + bd_ok + " event=" + event + "\n");
				responseBody.put("message", "Failure");
			}

			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "Blacklist User Service");
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
