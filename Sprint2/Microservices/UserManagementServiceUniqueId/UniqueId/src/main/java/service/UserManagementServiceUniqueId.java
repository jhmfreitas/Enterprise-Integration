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

public class UserManagementServiceUniqueId implements RequestStreamHandler {

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
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("id") != null)
					token = (String) bodyjson.get("id");
			}

			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			ResultSet resultSet;
			String validData = "true";

			if (bd_ok == true && event != null) {
				PreparedStatement s;
				s = conn.prepareStatement("select * from userInfo where token = ?");
				s.setString(1, token);
				resultSet = s.executeQuery();
				while (resultSet.next()) {
					logger.log("Repeated ID!\n");
					validData = "false";
				}

				s.close();
				resultSet.close();
				conn.close();

				// responseBody.put("message",validData);
			} else {
				logger.log("Failed: bd_ok=" + bd_ok + " event=" + event + "\n");
				// responseBody.put("message","False");
			}

			responseBody.put("message", validData);
			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "Unique ID Validation");
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
