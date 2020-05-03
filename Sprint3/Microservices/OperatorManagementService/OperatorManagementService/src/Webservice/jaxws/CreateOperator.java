
package Webservice.jaxws;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "createOperator", namespace = "http://Webservice/")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "createOperator", namespace = "http://Webservice/")
public class CreateOperator {

    @XmlElement(name = "operatorEvent", namespace = "")
    private String operatorEvent;

    /**
     * 
     * @return
     *     returns String
     */
    public String getOperatorEvent() {
        return this.operatorEvent;
    }

    /**
     * 
     * @param operatorEvent
     *     the value for the operatorEvent property
     */
    public void setOperatorEvent(String operatorEvent) {
        this.operatorEvent = operatorEvent;
    }

}
