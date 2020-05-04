package Webservice;


import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

//Service Endpoint Interface
@WebService(targetNamespace="http://ec2-54-196-98-231.compute-1.amazonaws.com:9997/operatorManagementService")
public interface OperatorManagementService{
	@WebMethod
	public void startService();
	@WebMethod
	public String createOperator(@WebParam(name = "operator") String operator,@WebParam(name = "operatorType") String operatorType,@WebParam(name = "price") String price);
	@WebMethod
	public String createTripCost(@WebParam(name = "baseCostString") String baseCostString,@WebParam(name = "operatorName") String operatorName,@WebParam(name = "timeStamp") String timeStamp,@WebParam(name = "planType") String planType,@WebParam(name = "token") String token);
}