
package Webservice.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "createTripCost", namespace = "http://ec2-54-84-79-209.compute-1.amazonaws.com:9997/operatorManagementService")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "createTripCost", namespace = "http://ec2-54-84-79-209.compute-1.amazonaws.com:9997/operatorManagementService", propOrder = {
    "baseCostString",
    "operatorName",
    "timeStamp",
    "planType",
    "token"
})
public class CreateTripCost {

    @XmlElement(name = "baseCostString", namespace = "")
    private String baseCostString;
    @XmlElement(name = "operatorName", namespace = "")
    private String operatorName;
    @XmlElement(name = "timeStamp", namespace = "")
    private String timeStamp;
    @XmlElement(name = "planType", namespace = "")
    private String planType;
    @XmlElement(name = "token", namespace = "")
    private String token;

    /**
     * 
     * @return
     *     returns String
     */
    public String getBaseCostString() {
        return this.baseCostString;
    }

    /**
     * 
     * @param baseCostString
     *     the value for the baseCostString property
     */
    public void setBaseCostString(String baseCostString) {
        this.baseCostString = baseCostString;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getOperatorName() {
        return this.operatorName;
    }

    /**
     * 
     * @param operatorName
     *     the value for the operatorName property
     */
    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getTimeStamp() {
        return this.timeStamp;
    }

    /**
     * 
     * @param timeStamp
     *     the value for the timeStamp property
     */
    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getPlanType() {
        return this.planType;
    }

    /**
     * 
     * @param planType
     *     the value for the planType property
     */
    public void setPlanType(String planType) {
        this.planType = planType;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getToken() {
        return this.token;
    }

    /**
     * 
     * @param token
     *     the value for the token property
     */
    public void setToken(String token) {
        this.token = token;
    }

}
