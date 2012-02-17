package org.broadinstitute.sequel.entity.sample;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.vessel.MolecularState;

import java.util.Collection;

/**
 * A change to the state (of a SampleSheet).
 */
public class StateChange {

    private static Log gLog = LogFactory.getLog(StateChange.class);


    /**
     * Positive control or negative control?
     * not a sample attribute, but an attribute
     * of the sample in a group of samples
     * in a container.
     * @return
     */
    public SampleInstance.GSP_CONTROL_ROLE getControlRole() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public MolecularState getMolecularState() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    public Project getProjectOverride() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public Collection<ReadBucket> getReadBucketOverrides() {
        throw new RuntimeException("Method not yet implemented.");
    }

    
}
