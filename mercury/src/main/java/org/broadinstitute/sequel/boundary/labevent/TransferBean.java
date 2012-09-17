package org.broadinstitute.sequel.boundary.labevent;

import org.broadinstitute.sequel.boundary.Namespaces;

import javax.xml.bind.annotation.XmlType;

/**
 * A JAX-RS DTO for transfers
 */
@XmlType(namespace = Namespaces.LAB_EVENT)
public class TransferBean {
    /** Cherry pick, section, tube to tube etc. */
    private String type;
    private String sourceBarcode;
    private String sourceSection;
    private String sourcePosition;
    private String targetBarcode;
    private String targetSection;
    private String targetPosition;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSourceBarcode() {
        return sourceBarcode;
    }

    public void setSourceBarcode(String sourceBarcode) {
        this.sourceBarcode = sourceBarcode;
    }

    public String getSourceSection() {
        return sourceSection;
    }

    public void setSourceSection(String sourceSection) {
        this.sourceSection = sourceSection;
    }

    public String getSourcePosition() {
        return sourcePosition;
    }

    public void setSourcePosition(String sourcePosition) {
        this.sourcePosition = sourcePosition;
    }

    public String getTargetBarcode() {
        return targetBarcode;
    }

    public void setTargetBarcode(String targetBarcode) {
        this.targetBarcode = targetBarcode;
    }

    public String getTargetSection() {
        return targetSection;
    }

    public void setTargetSection(String targetSection) {
        this.targetSection = targetSection;
    }

    public String getTargetPosition() {
        return targetPosition;
    }

    public void setTargetPosition(String targetPosition) {
        this.targetPosition = targetPosition;
    }
}
