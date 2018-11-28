package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import org.broadinstitute.gpinformatics.mercury.boundary.Namespaces;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.List;

/**
 * JAX-RS DTO for a Sample Kit
 */
@XmlRootElement(namespace = Namespaces.VESSEL)
@XmlType(namespace = Namespaces.VESSEL)
@XmlAccessorType
public class SampleKitInfo {
    protected String kitId;
    protected String containerId;
    protected String status;
    protected Boolean isPlate;
    @XmlElement(nillable = true)
    protected List<SampleInfo> sampleInfos;

    /** No-arg constructor required for use by the JAX-RS framework. */
    @SuppressWarnings("UnusedDeclaration")
    public SampleKitInfo() {
    }

    public SampleKitInfo(String kitId, String containerId, String status, Boolean isPlate,
                         List<SampleInfo> sampleInfos) {
        this.kitId = kitId;
        this.containerId = containerId;
        this.isPlate = isPlate;
        this.status = status;
        this.sampleInfos = sampleInfos;
    }

    public String getKitId() {
        return kitId;
    }

    public String getContainerId() {
        return containerId;
    }

    public String getStatus() {
        return status;
    }

    public Boolean isPlate() {
        return isPlate;
    }

    public List<SampleInfo> getSampleInfos() {
        return sampleInfos;
    }
}
