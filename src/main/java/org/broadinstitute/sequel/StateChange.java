package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A change to the state (of a SampleSheet).
 */
public class StateChange {

    private static Log gLog = LogFactory.getLog(StateChange.class);

    public MolecularState getMolecularState() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    public Project getProjectOverride() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public ReadBucket getReadBucketOverride() {
        throw new RuntimeException("Method not yet implemented.");
    }

    
}
