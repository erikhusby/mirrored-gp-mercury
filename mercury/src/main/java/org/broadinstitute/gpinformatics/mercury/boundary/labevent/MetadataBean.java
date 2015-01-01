package org.broadinstitute.gpinformatics.mercury.boundary.labevent;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;

/**
 * JAX-RS DTO to represent metadata in a LabEvent.
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class MetadataBean {
    private String name;
    private String value;

    /**
     * For JAXB
     */
    public MetadataBean() {
    }

    public MetadataBean(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
