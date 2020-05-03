
package Webservice.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "createOperator", namespace = "http://ec2-54-84-79-209.compute-1.amazonaws.com:9997/operatorManagementService")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "createOperator", namespace = "http://ec2-54-84-79-209.compute-1.amazonaws.com:9997/operatorManagementService", propOrder = {
    "operator",
    "operatorType",
    "price"
})
public class CreateOperator {

    @XmlElement(name = "operator", namespace = "")
    private String operator;
    @XmlElement(name = "operatorType", namespace = "")
    private String operatorType;
    @XmlElement(name = "price", namespace = "")
    private String price;

    /**
     * 
     * @return
     *     returns String
     */
    public String getOperator() {
        return this.operator;
    }

    /**
     * 
     * @param operator
     *     the value for the operator property
     */
    public void setOperator(String operator) {
        this.operator = operator;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getOperatorType() {
        return this.operatorType;
    }

    /**
     * 
     * @param operatorType
     *     the value for the operatorType property
     */
    public void setOperatorType(String operatorType) {
        this.operatorType = operatorType;
    }

    /**
     * 
     * @return
     *     returns String
     */
    public String getPrice() {
        return this.price;
    }

    /**
     * 
     * @param price
     *     the value for the price property
     */
    public void setPrice(String price) {
        this.price = price;
    }

}
