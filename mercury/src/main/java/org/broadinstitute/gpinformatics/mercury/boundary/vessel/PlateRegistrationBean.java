package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(namespace = Namespaces.REST, name = "plateRegistration")
@XmlType(namespace = Namespaces.REST)
@XmlAccessorType(XmlAccessType.FIELD)
public class PlateRegistrationBean {
    private String receptacleType;
    private String receptacleName;

    public PlateRegistrationBean() {
    }

    public PlateRegistrationBean(String receptacleType, String receptacleName) {
        this.receptacleType = receptacleType;
        this.receptacleName = receptacleName;
    }

    public String getReceptacleType() {
        return receptacleType;
    }

    public void setReceptacleType(String receptacleType) {
        this.receptacleType = receptacleType;
    }

    public String getReceptacleName() {
        return receptacleName;
    }

    public void setReceptacleName(String receptacleName) {
        this.receptacleName = receptacleName;
    }
}
