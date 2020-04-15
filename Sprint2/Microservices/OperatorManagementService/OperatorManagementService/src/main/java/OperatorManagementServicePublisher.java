package Webservice;

import javax.xml.ws.Endpoint;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import java.util.Properties;
import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;

public class OperatorManagementServicePublisher {
	public static void main(String[] args) {
		Endpoint ep = Endpoint.create(new OperatorManagementServiceImpl());
		ep.publish("http://localhost:9997/kafkamgnt");
	}
	
}