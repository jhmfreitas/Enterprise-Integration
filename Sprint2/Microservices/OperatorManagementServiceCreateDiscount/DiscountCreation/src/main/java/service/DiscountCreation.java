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
import java.util.ArrayList;
import java.util.Iterator;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class DiscountCreation implements RequestStreamHandler {
	private Connection conn = null;
	static String AWSDBIP = "operatordb.ca14fw262vr6.us-east-1.rds.amazonaws.com";

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

			String discountId = new String();
			String discountName = new String();
			String value = new String();
			String beginAt = new String();
			String endAt = new String();
			String planType = new String();
			if (event.get("body") != null) {
				JSONObject bodyjson = (JSONObject) parser.parse((String) event.get("body"));
				if (bodyjson.get("discountId") != null)
					discountId = (String) bodyjson.get("discountId");
				if (bodyjson.get("discountName") != null)
					discountName = (String) bodyjson.get("discountName");
				if (bodyjson.get("value") != null)
					value = (String) bodyjson.get("value");
				if (bodyjson.get("beginAt") != null)
					beginAt = (String) bodyjson.get("beginAt");
				if (bodyjson.get("endAt") != null)
					endAt = (String) bodyjson.get("endAt");
				if (bodyjson.get("planType") != null)
					planType = (String) bodyjson.get("planType");
			}

			logger.log("start:" + (String) event.toString() + "\n");
			JSONObject responseBody = new JSONObject();
			String response = "Failure";

			if (bd_ok == true && event != null) {
				if (insertDiscountInDB(discountId, discountName, value, beginAt, endAt, planType) == 0) {
					response = "Success";
				}

			} else {
				logger.log("Failed: bd_ok=" + bd_ok + " event=" + event + "\n");
			}
			responseBody.put("message", response);
			// responseBody.put("message", validData);
			JSONObject headerJson = new JSONObject();
			headerJson.put("x-custom-header", "Discount Creation Service");
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

	private int insertDiscountInDB(String discountId, String discountName, String value, String beginAt, String endAt, String planType) {
		PreparedStatement s;
		try {
			System.out.println("insertDiscountInDB: Insert discount! \n");
			s = conn.prepareStatement("insert into discount values(?,?,?,?,?)");
			s.setString(1, discountId);
			s.setString(2, discountName);
			s.setInt(3, Integer.parseInt(value));
			s.setTimestamp(4, java.sql.Timestamp.valueOf(beginAt));
			s.setTimestamp(5, java.sql.Timestamp.valueOf(endAt));
			s.executeUpdate();
			s.close();
			System.out.println("insertDiscountInDB: Done! \n");
		} catch (SQLException e) {
			System.out.println("insertDiscount:" + (String) e.toString() + "\n");
			return 1;
		}

		//Get All operators to apply discount
		ArrayList<String> operators = new ArrayList<String>();
		ResultSet resultSet;
		try {
			s = conn.prepareStatement("select operatorName from operator");
			resultSet = s.executeQuery();
			while (resultSet.next()) {
				System.out.println("getOperators : Got Operator\n");
				String operator = resultSet.getString("operatorName");
				System.out.println("getOperators : name= " + operator + "\n");
				operators.add(operator);
		    };
		} catch (SQLException e) {
			System.out.println("getOperators :" + (String) e.toString() + "\n");
			return 1;
		}


		try {
			s = conn.prepareStatement("insert into discount_planType values(?,?)");
			s.setString(1, discountId);
			s.setString(2, planType);
			s.executeUpdate();
			s.close();
		} catch (SQLException e) {
			System.out.println("discount_planType:" + (String) e.toString() + "\n");
			return 1;
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
				return 1;
			}
		}

		return 0;
	}
}
