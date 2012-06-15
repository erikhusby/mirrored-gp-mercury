package org.broadinstitute.sequel.control.pass;

import org.broadinstitute.sequel.boundary.squid.AbstractPass;
import org.broadinstitute.sequel.entity.workflow.LabBatch;

import java.util.Collection;

public class PassBatcher {

    public Collection<LabBatch> createBatches(AbstractPass pass,
                                              int numSamplesPerBatch) {
        /*
        int numSamplesInCurrentBatch = 0;
        final List<LabBatch> labBatches =  new ArrayList<LabBatch>();
        final List<StartingSample> samplesInBatch = new ArrayList<StartingSample>(numSamplesPerBatch);
        for (Sample passSample : pass.getSampleDetailsInformation().getSample()) {
            samplesInBatch.add(new BSPStartingSample());
            numSamplesInCurrentBatch++;
            if (numSamplesInCurrentBatch == numSamplesPerBatch) {
                LabBatch labBatch = new LabBatch(samplesInBatch);
                numSamplesInCurrentBatch = 0;
            }
        }
        */
        throw new RuntimeException("I haven't been written yet.");
    }
}
