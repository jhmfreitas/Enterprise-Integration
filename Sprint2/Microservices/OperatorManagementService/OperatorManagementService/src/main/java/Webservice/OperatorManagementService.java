package Webservice;


import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

@WebService
public interface OperatorManagementService{
	@WebMethod
	public void startService();
}