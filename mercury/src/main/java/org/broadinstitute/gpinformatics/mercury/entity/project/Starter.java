package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.Set;

public interface Starter {

    public String getLabel();

    public Set<SampleInstance> getSampleInstances();

    public boolean isAliquotExpected();

    public Set<LabBatch> getLabBatches();

    public void addLabBatch(LabBatch labBatch);

}
