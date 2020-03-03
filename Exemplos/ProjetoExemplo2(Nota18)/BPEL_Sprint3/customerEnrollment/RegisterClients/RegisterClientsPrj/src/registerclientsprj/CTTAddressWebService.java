package registerclientsprj;

import	java.io.*;		
import	java.net.*;

import javax.jws.WebService;

import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

import	oracle.xml.parser.v2.*;	
import	org.xml.sax.InputSource;

@WebService(serviceName = "CTTAddressWebService", portName = "CTTAddressWebServiceSoap12HttpPort")
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class CTTAddressWebService {
    public CTTAddressWebService() {
        super();
    }
    
    public Boolean isValidZipcode(String zipcode) {
        
        String endpoint = "http://www.ctt.pt/pdcp/xml_pdcp";

        StringBuilder responseString = null;
        String xpathResult=null;       

        String charset = "UTF-8";
        String url = endpoint + "?incodpos=" + zipcode;
        System.out.println(url);

        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", charset);
            InputStream response = connection.getInputStream();

            HttpURLConnection httpConnection = (HttpURLConnection)connection;
            int status = httpConnection.getResponseCode();
            System.out.println("Status: " + status);

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

        try {
            DOMParser domParser = new DOMParser();
            domParser.parse(new InputSource(new StringReader(responseString.toString())));
            XMLDocument document = domParser.getDocument();

            XMLNode node = (XMLNode)document.selectSingleNode("//Freguesia/@idfr"); //If this property exists (not null), then it's a valid zipcode
            xpathResult = node.getText(); 
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return false; //Invalid zipcode
        }

        return true; //Valid zipcode!
    }
    
    /*
     * Test web service
    public static void main(String[] args) {
        String codpost = "1234-567";
        
        CTTAddressWebService ctt = new CTTAddressWebService();
        
        System.out.println(ctt.isValidZipcode(codpost));
    }
    */
}
