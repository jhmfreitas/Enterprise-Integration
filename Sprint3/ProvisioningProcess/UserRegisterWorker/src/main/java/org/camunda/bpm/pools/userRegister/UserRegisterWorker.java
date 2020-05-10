package org.camunda.bpm.pools.userRegister;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.logging.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.camunda.bpm.client.ExternalTaskClient;

public class UserRegisterWorker {
	private final static Logger LOGGER = Logger.getLogger(UserRegisterWorker.class.getName());
	
	public static void main(String[] args) {		
		ExternalTaskClient validationClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		validationClient.subscribe("validate-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			LOGGER.info("Validation Started");
			String AWSDBIP = "userdb.ca14fw262vr6.us-east-1.rds.amazonaws.com";
			String token = (String) externalTask.getVariable("token");
			String email = (String) externalTask.getVariable("email");
			String firstName = (String) externalTask.getVariable("firstName");
			String lastName = (String) externalTask.getVariable("lastName");
			String planType = (String) externalTask.getVariable("planType");
			String balance = (String) externalTask.getVariable("balance");
			Boolean validData = true;
			Connection conn = null;
			ResultSet resultSet;
			
			if(isValidString(token) && isValidString(email) && isValidString(firstName) && isValidString(lastName) && isValidString(planType) && Integer.parseInt(balance) > 0) {
				try {
					Class.forName("com.mysql.cj.jdbc.Driver");
					conn = DriverManager.getConnection("jdbc:mysql://" + AWSDBIP + ":3306/userdb", "admin", "projetoie");
					PreparedStatement s;
					s = conn.prepareStatement ("select * from userInfo where token = ?");
					s.setString(1, token);
					resultSet = s.executeQuery();
					while (resultSet.next()) {
						LOGGER.info("Repeated id! Try again");
						validData = false;
				    }
					
					s.close();
					resultSet.close();
					conn.close();
				} catch (ClassNotFoundException | SQLException e) { LOGGER.info("Error:"+ e.toString()); } 
			}
			else {
				LOGGER.info("Data not valid");
				validData = false;
			}
			externalTaskService.complete(externalTask , Collections.singletonMap("validData", validData));
		}).open();
		
		
		ExternalTaskClient registrationClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		registrationClient.subscribe("create-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String token = (String) externalTask.getVariable("token");
			String email = (String) externalTask.getVariable("email");
			String firstName = (String) externalTask.getVariable("firstName");
			String lastName = (String) externalTask.getVariable("lastName");
			String planType = (String) externalTask.getVariable("planType");
			String balance = (String) externalTask.getVariable("balance");
			Boolean validData = (Boolean) externalTask.getVariable("validData");
			try {
				LOGGER.info("Creation Started");
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "new-user.com");
				String query = "{\"id\":\""+token+"\",\"email\":\""+ email +"\",\"planType\":\""+planType+"\",\"firstName\":\""+firstName+"\",\"lastName\":\""+lastName+"\",\"balance\":\""+balance+"\"}";
				StringEntity Entity = new StringEntity(query);
				postRequest.setEntity(Entity);
				HttpEntity base = postRequest.getEntity();
				HttpResponse response = httpClient.execute(postRequest);
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null)
					LOGGER.info("response body = " + EntityUtils.toString(responseEntity));
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
			externalTaskService.complete(externalTask);
		}).open();
	}

	private static boolean isValidString(String str) {
		if(str != null && !str.trim().isEmpty()) {
			return true;
		}
		return false;
	}
}
