package org.camunda.bpm.pools.discountCreation;

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

public class DiscountCreationWorker {
	private final static Logger LOGGER = Logger.getLogger(DiscountCreationWorker.class.getName());
	private final static String KongIP = "http://ec2-3-86-40-136.compute-1.amazonaws.com:8000";
	private final static String CamundaIP = "http://ec2-54-175-77-51.compute-1.amazonaws.com:8080/engine-rest";
	
	public static void main(String[] args) {

		ExternalTaskClient validationClient = ExternalTaskClient.create()
				.baseUrl(CamundaIP).asyncResponseTimeout(10000).build();
		validationClient.subscribe("validate-info").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			LOGGER.info("Info Validation Started");
			String discountId = (String) externalTask.getVariable("discountId");
			String discountName = (String) externalTask.getVariable("discountName");
			String value = (String) externalTask.getVariable("value");
			String beginAt = (String) externalTask.getVariable("beginAt");
			String endAt = (String) externalTask.getVariable("endAt");

			Boolean validData;

			if (isValidString(value) && Float.parseFloat(value) > 0 && isValidString(discountId) && isValidString(discountName)
					&& isValidString(beginAt) && isValidString(endAt)) {
				LOGGER.info("Data is valid");
				validData = true;
			} else {
				LOGGER.info("Data is invalid");
				validData = false;
			}
			externalTaskService.complete(externalTask, Collections.singletonMap("validInfo", validData));
		}).open();

		ExternalTaskClient createDiscountClient = ExternalTaskClient.create()
				.baseUrl(CamundaIP).asyncResponseTimeout(10000).build();
		createDiscountClient.subscribe("create-discount").lockDuration(1000)
				.handler((externalTask, externalTaskService) -> {
					DefaultHttpClient httpClient = new DefaultHttpClient();
					String discountId = (String) externalTask.getVariable("discountId");
					String discountName = (String) externalTask.getVariable("discountName");
					String value = (String) externalTask.getVariable("value");
					String beginAt = (String) externalTask.getVariable("beginAt");
					String endAt = (String) externalTask.getVariable("endAt");
					String planType = (String) externalTask.getVariable("planType");

					Boolean succeeded = null;
					try {
						LOGGER.info("Create Discount Started!");
						HttpPost postRequest = new HttpPost(KongIP);
						postRequest.addHeader("content-type", "application/json");
						postRequest.addHeader("Host", "create-discount.com");
						String query = "{\"discountId\":\"" + discountId + "\",\"discountName\":\"" + discountName
								+ "\",\"value\":\"" + value + "\",\"beginAt\":\"" + beginAt + "\",\"endAt\":\"" + endAt + "\",\"planType\":\"" + planType + "\"}";
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
							LOGGER.info("Discount Creation:" + responseValue);
							if (responseValue.equals("Success")) {
								succeeded = true;
							} else {
								succeeded = false;
							}
						}

						externalTaskService.complete(externalTask,
								Collections.singletonMap("creationSuccess", succeeded));
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
		if (str != null && !str.trim().isEmpty()) {
			return true;
		}
		return false;
	}
}
