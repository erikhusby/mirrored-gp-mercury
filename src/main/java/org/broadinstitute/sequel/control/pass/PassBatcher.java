package org.broadinstitute.sequel.control.pass;

import org.broadinstitute.sequel.boundary.squid.AbstractPass;
import org.broadinstitute.sequel.boundary.squid.Sample;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.workflow.LabBatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class PassBatcher {

    public Collection<LabBatch> createBatches(AbstractPass pass,
                                              int numSamplesPerBatch) {
        /*
        int numSamplesInCurrentBatch = 0;
        final List<LabBatch> labBatches =  new ArrayList<LabBatch>();
        final List<StartingSample> samplesInBatch = new ArrayList<StartingSample>(numSamplesPerBatch);
        for (Sample passSample : pass.getSampleDetailsInformation().getSample()) {
            samplesInBatch.add(new BSPSample());
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
