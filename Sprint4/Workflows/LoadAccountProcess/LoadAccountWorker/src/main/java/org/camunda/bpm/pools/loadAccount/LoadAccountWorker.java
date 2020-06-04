package org.camunda.bpm.pools.loadAccount;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import java.util.logging.Logger;

public class LoadAccountWorker {
	private final static Logger LOGGER = Logger.getLogger(LoadAccountWorker.class.getName());
	private final static String KongIP = "http://ec2-3-86-40-136.compute-1.amazonaws.com:8000";
	private final static String CamundaIP = "http://ec2-54-175-77-51.compute-1.amazonaws.com:8080/engine-rest";
	
	public static void main(String[] args) {
		ExternalTaskClient nifValidationClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		nifValidationClient.subscribe("validate-nif").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String nif = (String) externalTask.getVariable("nif");
			
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
					Boolean responseValue = (Boolean) responseObj.get("nif_validation");
					LOGGER.info("Valid NIF:"+responseValue.toString());
					externalTaskService.complete(externalTask , Collections.singletonMap("validNif", responseValue));
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
		
		ExternalTaskClient validationClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		validationClient.subscribe("validate-amount").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			LOGGER.info("Amount Validation Started");
			String amount = (String) externalTask.getVariable("amount");

			Boolean validAmount;
			
			if(Integer.parseInt(amount) > 0) {
				LOGGER.info("Amount is valid");
				validAmount = true;
			}
			else {
				LOGGER.info("Amount is invalid");
				validAmount = false;
			}
			externalTaskService.complete(externalTask , Collections.singletonMap("validAmount", validAmount));
		}).open();
		
		ExternalTaskClient checkNifClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		checkNifClient.subscribe("check-nif").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String nif = (String) externalTask.getVariable("nif");
			Boolean isRegistered = null;
			try {
				LOGGER.info("Check NIF Started!");
				HttpPost postRequest = new HttpPost(KongIP);
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "check-user.com");
				String query = "{\"nif\":\""+nif+"\"}";
				StringEntity Entity = new StringEntity(query);
				postRequest.setEntity(Entity);
				HttpEntity base = postRequest.getEntity();
				HttpResponse response = httpClient.execute(postRequest);
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
				HttpEntity responseEntity = response.getEntity();
				String id = null;
				if (responseEntity != null) {
					String responseStr = EntityUtils.toString(responseEntity); 
					JSONParser parser = new JSONParser();
					JSONObject responseObj = (JSONObject) ((JSONObject) parser.parse(responseStr));
					String responseValue = (String) responseObj.get("message");
					id = (String) responseObj.get("id");
					LOGGER.info("NIF is registered:"+responseValue);
					if(responseValue.equals("Success")) {
						isRegistered = true;
					}
					else {
						isRegistered = false;
					}
				}
				
				Map<String,Object> variables  = new HashMap<String,Object>();
				variables.put("isRegistered", isRegistered);
				variables.put("id", id);
				
				externalTaskService.complete(externalTask , variables);
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
		
		ExternalTaskClient getUserClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		getUserClient.subscribe("get-email").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String nif = (String) externalTask.getVariable("nif");
			Boolean isRegistered = null;
			String email = new String();
			try {
				LOGGER.info("Get email Started!");
				HttpPost postRequest = new HttpPost(KongIP);
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "get-email.com");
				String query = "{\"nif\":\""+nif+"\"}";
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
					LOGGER.info("Got email:"+responseValue);
					if(!responseValue.equals("Failure")) {
						email = responseValue;
						isRegistered = true;
					}
					else {
						isRegistered = false;
					}
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
			externalTaskService.complete(externalTask , Collections.singletonMap("email", email));
		}).open();
		
		ExternalTaskClient loadAccountClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		loadAccountClient.subscribe("load-account").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String nif = (String) externalTask.getVariable("nif");
			String amount = (String) externalTask.getVariable("amount");
			
			try {
				LOGGER.info("Load Account Started!");
				HttpPost postRequest = new HttpPost(KongIP);
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "load-account.com");
				String query = "{\"nif\":\""+nif+"\",\"amount\":\""+amount+"\"}";
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
					String previousBalance = (String) responseObj.get("previous-balance");
					String newBalance = (String) responseObj.get("new-balance");
					LOGGER.info("Previous Balance: "+previousBalance+"€");
					LOGGER.info("Amount to deposit: "+amount+"€");
					LOGGER.info("Loaded Account:"+responseValue);
					Map<String,Object> variables  = new HashMap<String,Object>();
					variables.put("previousBalance", Double.parseDouble(previousBalance));
					variables.put("newBalance", Double.parseDouble(newBalance));
					externalTaskService.complete(externalTask , variables);
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
		
		ExternalTaskClient stopDunningClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		loadAccountClient.subscribe("stop-dunning").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			LOGGER.info("Stop Dunning Started!");
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String id = (String) externalTask.getVariable("id");

			HttpPost postRequest = new HttpPost(CamundaIP+"/message");
			postRequest.addHeader("content-type", "application/json");
			String query = "{\"messageName\":\"UserLoadsAccountMessage\",\"businessKey\":\"" + id + "\"}";
			LOGGER.info("Post request: " + query );
			StringEntity Entity;
			try {
				Entity = new StringEntity(query);
				postRequest.setEntity(Entity);
				HttpEntity base = postRequest.getEntity();
				HttpResponse response = httpClient.execute(postRequest);
				int statusCode = response.getStatusLine().getStatusCode();
				LOGGER.info("Finished with HTTP error code : " + statusCode + "\n" + response.toString());
			} catch (UnsupportedEncodingException e) {
				LOGGER.info("Error: " + e.toString() + "\n");
			} catch (ClientProtocolException e) {
				LOGGER.info("Error: " + e.toString() + "\n");
			} catch (IOException e) {
				LOGGER.info("Error: " + e.toString() + "\n");
			} finally {
				httpClient.getConnectionManager().shutdown();
			}
			externalTaskService.complete(externalTask);
		}).open();
	}
}
