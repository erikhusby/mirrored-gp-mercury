package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.workflow.LabBatch;

import java.util.Set;

public interface Starter {

    public String getLabel();

    public Set<SampleInstance> getSampleInstances();

    public boolean isAliquotExpected();

    public Set<LabBatch> getLabBatches();

    public void addLabBatch(LabBatch labBatch);

}
