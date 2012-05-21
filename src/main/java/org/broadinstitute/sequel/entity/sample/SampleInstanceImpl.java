package org.broadinstitute.sequel.entity.sample;

import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.vessel.MolecularStateImpl;
import org.broadinstitute.sequel.entity.analysis.ReadBucket;

import java.util.Collection;
import java.util.HashSet;

public class SampleInstanceImpl implements SampleInstance {

    private StartingSample sample;
    
    private GSP_CONTROL_ROLE controlRole;
    
    private Collection<ProjectPlan> projectPlans = new HashSet<ProjectPlan>();
    
    private Collection<ReadBucket> readBuckets = new HashSet<ReadBucket>();
    
    private MolecularState molecularState = new MolecularStateImpl();
    
    public SampleInstanceImpl(StartingSample sample,
                              GSP_CONTROL_ROLE controlRole,
                              ProjectPlan projectPlan,
                              MolecularState molecularState,
                              WorkflowDescription workflowDescription) {
        this.sample = sample;
        this.controlRole = controlRole;
        projectPlans.add(projectPlan);
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
    public ProjectPlan getSingleProjectPlan() {
        if (projectPlans.isEmpty()) {
            return null;
        }
        else if (projectPlans.size() <= 1) {
            return projectPlans.iterator().next();
        }
        else {
            throw new RuntimeException("There are " + projectPlans.size() + " possible project plans for " + this);
        }
    }

    @Override
    public Collection<ProjectPlan> getAllProjectPlans() {
        return projectPlans;
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
            if (change.getProjectPlanOverride() != null) {
                projectPlans.add(change.getProjectPlanOverride());
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
