package org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * This wraps the sample details so that other information can be added in the future and to hold multiple sets.
 * Copied from BSP.
 */
@XmlRootElement(name = "details")
public class Details implements Serializable {

    private final String batchId;

    private SampleDetails sampleDetails;

    public Details() {
        this.batchId = "";
    }

    public Details(String batchId) {
        this.batchId = batchId;
    }

    public String getBatchId() {
        return batchId;
    }

    public SampleDetails getSampleDetails() {
        return sampleDetails;
    }

    public void setSampleDetails(SampleDetails sampleDetails) {
        this.sampleDetails = sampleDetails;
    }
}
