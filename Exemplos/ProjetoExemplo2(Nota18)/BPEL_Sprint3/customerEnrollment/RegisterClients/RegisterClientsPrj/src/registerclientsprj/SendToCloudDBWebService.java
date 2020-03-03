package registerclientsprj;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebService;

import javax.xml.ws.BindingType;
import javax.xml.ws.soap.SOAPBinding;

@WebService(serviceName = "SendToCloudDBWebService", portName = "SendToCloudDBWebServiceSoap12HttpPort")
@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
public class SendToCloudDBWebService {
    public SendToCloudDBWebService() {
        super();
    }

    @WebMethod
    @Oneway
    public void sendToCloudDB(int userId, String status, float discount) {

        Connection conn = null;
        boolean bd_ok = false;

        try {
            
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://mytestdb.crjjgaudsykb.us-east-1.rds.amazonaws.com:3306/CustomerManagementService", "storemessages", "pedro1234"); // ("jdbc:mysql://yourAWSDBIP:3306/YOURDATABASENAME","YOURMasterUSERNAME","YOURPASSWORD")												
            bd_ok = true;
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        try {
            if (bd_ok) {
                PreparedStatement s = null; //so uma inicializacao
                s = conn.prepareStatement("INSERT INTO CustomerStatus VALUES(?,?,?)");

                s.setInt(1, userId); // user_id
                s.setString(2, status); // topic
                s.setFloat(3, discount); // discount
                s.executeUpdate();
                s.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

/*
    public static void main(String[] args) {
        SendToCloudDBWebService a = new SendToCloudDBWebService();
        a.sendToCloudDB(2, "valid", 0); //int userId, String status, float discount
    }
*/
}
