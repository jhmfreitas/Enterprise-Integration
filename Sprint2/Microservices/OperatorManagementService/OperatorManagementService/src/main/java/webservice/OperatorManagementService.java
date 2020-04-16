package webservice;

import java.io.InputStream;
import java.io.OutputStream;

import javax.jws.WebMethod;
import javax.jws.WebService;

//Service Endpoint Interface
@WebService
public interface OperatorManagementService{
	@WebMethod
	public void startService(InputStream inputStream, OutputStream outputStream);
}