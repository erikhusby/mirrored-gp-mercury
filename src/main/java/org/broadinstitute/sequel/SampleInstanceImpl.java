package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Collection;
import java.util.HashSet;

public class SampleInstanceImpl implements SampleInstance {

    private static Log gLog = LogFactory.getLog(SampleInstanceImpl.class);

    private StartingSample sample;
    
    private GSP_CONTROL_ROLE controlRole;
    
    private Project project;
    
    private Collection<ReadBucket> readBuckets = new HashSet<ReadBucket>();
    
    private MolecularState molecularState = new MolecularStateImpl();
    
    public SampleInstanceImpl(StartingSample sample,
                              GSP_CONTROL_ROLE controlRole,
                              Project project,
                              MolecularState molecularState,
                              WorkflowDescription workflowDescription) {
        this.sample = sample;
        this.controlRole = controlRole;
        this.project = project;
        this.molecularState = molecularState;
    }
    
    @Override
    public GSP_CONTROL_ROLE getControlRole() {
        return controlRole;
    }

    @Override
    public StartingSample getStartingSample() {
        return sample;
    }

    @Override
    public Project getProject() {
        return project;
    }

    @Override
    public MolecularState getMolecularState() {
        return molecularState;
    }

    @Override
    public Collection<ReadBucket> getReadBuckets() {
        throw new RuntimeException("I haven't been written yet.");
    }
    
    public void applyChange(StateChange change) {
        if (change != null) {
            if (change.getControlRole() != null) {
                controlRole = change.getControlRole();
            }
            if (change.getProjectOverride() != null) {
                project = change.getProjectOverride();
            }
            if (change.getReadBucketOverrides() != null) {
                readBuckets.clear();
                readBuckets.addAll(change.getReadBucketOverrides());
            }
            if (change.getMolecularState() != null) {
                if (change.getMolecularState().getMolecularEnvelope() != null) {
                    MolecularEnvelope envelopeDelta = change.getMolecularState().getMolecularEnvelope();
                    getMolecularState().getMolecularEnvelope().surroundWith(envelopeDelta);
                }
            }
        }
    }
}
