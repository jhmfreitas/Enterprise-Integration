package Webservice;

import javax.jws.WebMethod;
import javax.jws.WebService;

//Service Endpoint Interface
@WebService
public interface OperatorManagementService{
	@WebMethod
	public int addTopic(String topic1, String topic2, String topic3);
	public void startAction(InputStream inputStream, OutputStream outputStream);
	public String startService(InputStream inputStream);
}