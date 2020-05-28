package org.camunda.bpm.pools.operatorCreation;

import java.io.IOException;
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
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class OperatorCreationWorker {
	private final static Logger LOGGER = Logger.getLogger(OperatorCreationWorker.class.getName());
	private final static String KongIP = "http://ec2-3-84-68-234.compute-1.amazonaws.com:8000";
	private final static String CamundaIP = "http://ec2-54-82-161-3.compute-1.amazonaws.com:8080/engine-rest";
	
	public static void main(String[] args) {
		
		ExternalTaskClient validationClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		validationClient.subscribe("validate-info").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			LOGGER.info("Info Validation Started");
			String operatorName = (String) externalTask.getVariable("operatorName");
			String price = (String) externalTask.getVariable("price");
			String operatorType = (String) externalTask.getVariable("operatorType");

			Boolean validData;
			
			if((operatorType.equals("T1") || operatorType.equals("T2")) && price.equals("null")) {
				LOGGER.info("Data is valid");
				validData = true;
			}
			else if(operatorType.equals("T0") && isValidString(price) && Float.parseFloat(price) > 0 && isValidString(operatorName)) {
				LOGGER.info("Data is valid");
				validData = true;
			}
			else {
				LOGGER.info("Data is invalid");
				validData = false;
			}
			externalTaskService.complete(externalTask , Collections.singletonMap("validInfo", validData));
		}).open();
		
		ExternalTaskClient createOperatorClient = ExternalTaskClient.create().baseUrl(CamundaIP)
				.asyncResponseTimeout(10000).build();
		createOperatorClient.subscribe("create-operator").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			String operatorName = (String) externalTask.getVariable("operatorName");
			String operatorType = (String) externalTask.getVariable("operatorType");
			String price = (String) externalTask.getVariable("price");
			Boolean succeeded = null;
			try {
				LOGGER.info("Create Operator Started!");
				HttpPost postRequest = new HttpPost(KongIP);
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "create-operator.com");
				String query = "{\"operator\":\""+ operatorName +"\",\"operatorType\":\""+operatorType+"\",\"price\":\"" + price + "\"}";
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
					LOGGER.info("Creation Succeeded:"+responseValue);
					if(responseValue.equals("Success")) {
						succeeded = true;
					}
					else {
						succeeded = false;
					}
				}
				
				externalTaskService.complete(externalTask , Collections.singletonMap("creationSuccess", succeeded));
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
	
	private static boolean isValidString(String str) {
		if(str != null && !str.trim().isEmpty()) {
			return true;
		}
		return false;
	}
}
