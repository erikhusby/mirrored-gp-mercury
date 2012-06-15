package org.broadinstitute.sequel.entity.sample;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.vessel.MolecularState;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import java.util.Collection;

/**
 * A change to the state (of a SampleSheet).
 */
@Entity
public class StateChange {

    @Id
    @SequenceGenerator(name = "SEQ_STATE_CHANGE", sequenceName = "SEQ_STATE_CHANGE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STATE_CHANGE")
    private Long stateChangeId;

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
    
    public BasicProjectPlan getProjectPlanOverride() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public Collection<ReadBucket> getReadBucketOverrides() {
        throw new RuntimeException("Method not yet implemented.");
    }

    
}
