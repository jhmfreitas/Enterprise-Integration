package org.camunda.bpm.pools.userRegister;

import java.io.IOException;
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
		ExternalTaskClient registrationClient = ExternalTaskClient.create().baseUrl("http://192.168.99.100:8080/engine-rest")
				.asyncResponseTimeout(10000).build();
		registrationClient.subscribe("create-user").lockDuration(1000).handler((externalTask, externalTaskService) -> {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			LOGGER.info("call-ws Started");
			try {
				HttpPost postRequest = new HttpPost("http://ec2-54-236-120-160.compute-1.amazonaws.com:8000");
				postRequest.addHeader("content-type", "application/json");
				postRequest.addHeader("Host", "new-user.com");
				String query = "{\"id\":\"id34\",\"email\":\"andrerafa@gmail.com\",\"planType\":\"post-paid\",\"firstName\":\"André\",\"lastName\":\"Rafael\",\"balance\":\"1000\"}";
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
}
