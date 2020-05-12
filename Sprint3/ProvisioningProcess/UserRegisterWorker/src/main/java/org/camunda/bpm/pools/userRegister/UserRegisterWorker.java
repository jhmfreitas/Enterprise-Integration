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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.camunda.bpm.client.ExternalTaskClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class UserRegisterWorker {
	private final static Logger LOGGER = Logger.getLogger(UserRegisterWorker.class.getName());
	
	public static void main(String[] args) {
		ExternalTaskClient uniqueClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		uniqueClient.subscribe("user-exists").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String token = (String) externalTask.getVariable("token");
			//morada e nif
			try {
				LOGGER.info("Unique ID Validation Started");
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "unique-id.com");
				String query = "{\"id\":\""+token+"\"}";
				StringEntity Entity = new StringEntity(query);
				postRequest.setEntity(Entity);
				HttpEntity base = postRequest.getEntity();
				HttpResponse response = httpClient.execute(postRequest);
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseStr = EntityUtils.toString(responseEntity); 
					JSONParser parser = new JSONParser();
					JSONObject responseObj = (JSONObject) ((JSONObject) parser.parse(responseStr));
					String responseValue = (String) responseObj.get("message");
					LOGGER.info("Unique ID:"+responseValue);
					externalTaskService.complete(externalTask , Collections.singletonMap("uniqueID", Boolean.valueOf(responseValue)));
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
		}).open();
		
		ExternalTaskClient validationClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		validationClient.subscribe("validate-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			LOGGER.info("Validation Started");
			String email = (String) externalTask.getVariable("email");
			String firstName = (String) externalTask.getVariable("firstName");
			String lastName = (String) externalTask.getVariable("lastName");
			String planType = (String) externalTask.getVariable("planType");
			String balance = (String) externalTask.getVariable("balance");
			//morada e nif
			Boolean validData;
			
			if(isValidString(email) && isValidString(firstName) && isValidString(lastName) && isValidString(planType) && Integer.parseInt(balance) > 0) {
				LOGGER.info("Data is valid");
				validData = true;
			}
			else {
				LOGGER.info("Data is invalid");
				validData = false;
			}
			externalTaskService.complete(externalTask , Collections.singletonMap("validData", validData));
		}).open();
		
		ExternalTaskClient nifValidationClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		nifValidationClient.subscribe("validate-nif").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String nif = (String) externalTask.getVariable("nif");
			//morada e nif
			
			try {
				LOGGER.info("NIF Validation Started");
				HttpGet getRequest = new HttpGet("https://www.nif.pt/?json=1&q=" + nif + "&key=885d793bae35ea25b1514ee74ab18461");
				getRequest.addHeader("content-type", "application/json");
				HttpResponse response = httpClient.execute(getRequest);
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
				HttpEntity responseEntity = response.getEntity();
				if (responseEntity != null) {
					String responseStr = EntityUtils.toString(responseEntity); 
					JSONParser parser = new JSONParser();
					JSONObject responseObj = (JSONObject) ((JSONObject) parser.parse(responseStr));
					String responseValue = (String) responseObj.get("result");
					LOGGER.info("Valid NIF:"+responseValue);
					if(responseValue.equals("success")){
						externalTaskService.complete(externalTask , Collections.singletonMap("validNif", true));
					}
					else {
						externalTaskService.complete(externalTask , Collections.singletonMap("validNif", false));
					}
				}
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
		}).open();
		
		
		ExternalTaskClient registrationClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		registrationClient.subscribe("create-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String nif = (String) externalTask.getVariable("nif");
			String token = (String) externalTask.getVariable("token");
			String email = (String) externalTask.getVariable("email");
			String firstName = (String) externalTask.getVariable("firstName");
			String lastName = (String) externalTask.getVariable("lastName");
			String planType = (String) externalTask.getVariable("planType");
			String balance = (String) externalTask.getVariable("balance");
			try {
				LOGGER.info("Creation Started");
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "new-user.com");
				String query = "{\"id\":\""+token+"\",\"nif\":\""+nif+"\",\"email\":\""+ email +"\",\"planType\":\""+planType+"\",\"firstName\":\""+firstName+"\",\"lastName\":\""+lastName+"\",\"balance\":\""+balance+"\"}";
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
