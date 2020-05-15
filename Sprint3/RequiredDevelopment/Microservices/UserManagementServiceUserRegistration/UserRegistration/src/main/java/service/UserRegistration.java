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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class UserRegistration implements RequestStreamHandler {

	public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) {
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
			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			
			if (bd_ok == true && event != null) {
				String token = (String) event.get("id");
				String nif = (String) event.get("nif");
				String email = (String) event.get("email");
				String planType = (String) event.get("planType");
				String firstName = (String) event.get("firstName");
				String lastName = (String) event.get("lastName");
				String balance = (String) event.get("balance");
				String address = (String) event.get("address");

				PreparedStatement s = conn.prepareStatement("insert into userInfo values(?,?,?,?,?,?,?)");
				s.setString(1, token);
				s.setString(2, nif);
				s.setString(3, email);
				s.setString(4, firstName);
				s.setString(5, lastName);
				s.setString(6, planType);
				s.setString(7, address);
				s.executeUpdate();
				s.close();

				s = conn.prepareStatement("insert into userBalance values(?,?,?)");
				s.setString(1, token);
				s.setInt(2, Integer.parseInt(balance));
				s.setBoolean(3, false);
				s.executeUpdate();
				s.close();

				logger.log("Success: User inserted! \n");
				
				responseBody.put("message","Success");
			} else {
				logger.log("Failed:\n");
				responseBody.put("message","Failure");
			}
			
			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "User Registration");
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
