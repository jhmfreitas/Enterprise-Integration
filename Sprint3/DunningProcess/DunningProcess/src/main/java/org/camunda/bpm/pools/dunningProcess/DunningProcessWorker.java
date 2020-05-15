package org.camunda.bpm.pools.dunningProcess;

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

public class DunningProcessWorker {
	private final static Logger LOGGER = Logger.getLogger(DunningProcessWorker.class.getName());

	public static void main(String[] args) {
		ExternalTaskClient blacklistClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		blacklistClient.subscribe("blacklist-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String token = (String) externalTask.getVariable("token");
			
			try {
				LOGGER.info("Blacklist User Started!");
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "blacklist-user.com");
				String query = "{\"id\":\""+token+"\",\"operation\":\"blacklist\"}";
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
					LOGGER.info("Blacklisted ID:"+responseValue);
					externalTaskService.complete(externalTask);
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
		}).open();
		
		ExternalTaskClient undoBlacklistClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		undoBlacklistClient.subscribe("undo-blacklist").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String token = (String) externalTask.getVariable("token");
			
			try {
				LOGGER.info("Undo Blacklist Started!");
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "blacklist-user.com");
				String query = "{\"id\":\""+token+"\",\"operation\":\"undo-blacklist\"}";
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
					LOGGER.info("Undo Blacklist:"+responseValue);
					externalTaskService.complete(externalTask);
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
		}).open();
		
		ExternalTaskClient removeUserClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		removeUserClient.subscribe("remove-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String token = (String) externalTask.getVariable("token");
			
			try {
				LOGGER.info("Remove User Started!");
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "remove-user.com");
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
					LOGGER.info("Remove User:"+responseValue);
					externalTaskService.complete(externalTask);
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
		}).open();
	}
}
