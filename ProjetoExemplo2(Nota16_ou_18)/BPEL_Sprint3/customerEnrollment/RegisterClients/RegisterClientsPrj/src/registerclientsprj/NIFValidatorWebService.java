package registerclientsprj;

import	java.io.*;		
import	java.net.*;

import javax.jws.WebService;

import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

@WebService(serviceName = "NIFValidatorWebService", portName = "NIFValidatorWebServiceSoap12HttpPort")
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class NIFValidatorWebService {
    public NIFValidatorWebService() {
        super();
    }

    public Boolean isValidNIF(String nif) {

        String apiID = "d89942a9280f2d92264477b57d4ac2ed";
        String endpoint = "http://www.nif.pt/";
        //http://www.nif.pt/?json=1&q=509442013&key=API_KEY

        StringBuilder responseString = null;
        String returnString = "";
        String xpathResult = null;

        String charset = "UTF-8";
        String url = endpoint + "?json=1&q=" + nif + "&key=" + apiID;
        System.out.println(url);

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", charset);
            InputStream response = connection.getInputStream();
            
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int status = httpConnection.getResponseCode();
            System.out.println("Status:     " + status);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response));
            responseString = new StringBuilder();

            String inputLine;

            while ((inputLine = rd.readLine()) != null)
                responseString.append(inputLine);
            rd.close();

            System.out.println("Response:" + responseString);
            
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        String response = responseString.toString();
            
        if (response.contains("success"))
            return true;

        return false;
    }
    
    /*
    public static void main(String[] args) {
        String nif = "501507930";
        
        NIFValidatorWebService test = new NIFValidatorWebService();
        
        System.out.println(test.isValidNIF(nif));
    }
    */
}
