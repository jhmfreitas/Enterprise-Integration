package Webservice;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

//Service Endpoint Interface
@WebService
public interface OperatorManagementService{
	@WebMethod
	public void startService();
	@WebMethod
	public void createOperator(@WebParam(name="operatorEvent")String operatorEvent);
	@WebMethod
	public void consumeTripCostEvent(String tripCostEvent);
	@WebMethod
	public void createDiscount(String discountEvent);
}