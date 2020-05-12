package Webservice;


import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlElement;

//Service Endpoint Interface
@WebService(targetNamespace="http://ec2-18-232-164-46.compute-1.amazonaws.com:9997/operatorManagementService")
public interface OperatorManagementService{
	@WebMethod
	public void startService();
	@WebMethod
	public String createOperator(@WebParam(name = "operator") @XmlElement(required=true)String operator,@WebParam(name = "operatorType")  @XmlElement(required=true)String operatorType,@WebParam(name = "price")  @XmlElement(required=true)String price);
	@WebMethod
	public String createTripCost(@WebParam(name = "baseCostString")  @XmlElement(required=true)String baseCostString,@WebParam(name = "operatorName")  @XmlElement(required=true)String operatorName,@WebParam(name = "timeStamp")  @XmlElement(required=true)String timeStamp,@WebParam(name = "planType")  @XmlElement(required=true)String planType,@WebParam(name = "token")  @XmlElement(required=true)String token);
	@WebMethod
	public String createDiscount(@WebParam(name = "operators")  @XmlElement(required=true)String[] operators,@WebParam(name = "discountId")  @XmlElement(required=true) String discountId,@WebParam(name = "discountName")  @XmlElement(required=true)String discountName,@WebParam(name = "value")  @XmlElement(required=true) String value,@WebParam(name = "beginAt")  @XmlElement(required=true) String beginAt,@WebParam(name = "endAt")  @XmlElement(required=true) String endAt,@WebParam(name = "planTypes")  @XmlElement(required=true) String[] planTypes);
}