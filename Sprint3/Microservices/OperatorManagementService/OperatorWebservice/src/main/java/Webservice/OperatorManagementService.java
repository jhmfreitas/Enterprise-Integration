package Webservice;

import javax.jws.WebMethod;
import javax.jws.WebService;

//Service Endpoint Interface
@WebService
public interface OperatorManagementService{
	@WebMethod
	public void startService();
}