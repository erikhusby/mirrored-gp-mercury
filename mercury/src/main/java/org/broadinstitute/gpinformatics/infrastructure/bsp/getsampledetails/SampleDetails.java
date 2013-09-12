package org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails;

import java.io.Serializable;
import java.util.List;

/**
 * Information about a sample that is sent out as XML through web services used by Automation Engineering.
 * Copied from BSP.
 */
@SuppressWarnings("UnusedDeclaration")
public class SampleDetails implements Serializable {

    private List<SampleInfo> sampleInfo;

    public List<SampleInfo> getSampleInfo() {
        return sampleInfo;
    }

    public void setSampleInfo(List<SampleInfo> sampleInfo) {
        this.sampleInfo = sampleInfo;
    }
}

