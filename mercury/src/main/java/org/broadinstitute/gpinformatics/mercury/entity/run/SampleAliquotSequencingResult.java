package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;

import java.util.Collection;

public class SampleAliquotSequencingResult implements SequencingResult {

    /**
     * Makes a sequencing result that is only
     * pertinant to the given sample.
     * @param s
     */
    public SampleAliquotSequencingResult(SampleInstance s) {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * Gets run data that is only pertinent to a
     * particular sample.
     * @return
     */
    public Collection<SequencingRun> getSequencingRuns() {
        throw new RuntimeException("I haven't been written yet.");
    }
}
