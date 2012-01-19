package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Collection;

public class SampleInstanceImpl implements SampleInstance {

    private static Log gLog = LogFactory.getLog(SampleInstanceImpl.class);

    private StartingSample sample;
    
    private GSP_CONTROL_ROLE controlRole;
    
    private Project project;
    
    
    public SampleInstanceImpl(StartingSample sample,
                              GSP_CONTROL_ROLE controlRole,
                              Project project,
                              MolecularState molecularState,
                              WorkflowDescription workflowDescription) {
        this.sample = sample;
        this.controlRole = controlRole;
        this.project = project;
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
    public void setProject(Project p) {
        this.project = p;
    }

    @Override
    public MolecularState getMolecularState() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public WorkflowDescription getWorkflowDescription() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isDevelopment() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<ReadBucket> getReadBuckets() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
