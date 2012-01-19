package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * The basic plan here is to store only the
 * name then
 */
public class BSPSample implements StartingSample {

    private static Log gLog = LogFactory.getLog(BSPSample.class);

    private final String sampleName;

    /**
     * Is there a distinction in BSP between
     * the name of the sample and the container
     * in which the sample resides?
     * @param sampleName
     */
    public BSPSample(String sampleName) {
        this.sampleName = sampleName;
    }

    @Override
    public String getContainerId() {
        return sampleName;
    }

    @Override
    public String getSampleName() {
        return sampleName;
    }

    @Override
    public String getPatientId() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getOrganism() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void logNote(StatusNote note) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
