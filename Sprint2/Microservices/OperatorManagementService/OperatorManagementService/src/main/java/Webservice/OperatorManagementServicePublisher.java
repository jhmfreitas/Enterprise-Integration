package Webservice;

import javax.xml.ws.Endpoint;

public class OperatorManagementServicePublisher {
	public static void main(String[] args) {
		String AWSIP = "ec2-34-228-247-65.compute-1.amazonaws.com";
		Endpoint ep = Endpoint.create(new OperatorManagementServiceImpl());
		ep.publish("http://" + AWSIP + ":9997/operatorManagementService");
	}
	
}