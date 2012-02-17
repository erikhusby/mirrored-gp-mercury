package org.broadinstitute.sequel.entity.run;

import java.util.Collection;

/**
 * Really just a collection of sequencing
 * runs.
 */
public interface SequencingResult {

    /**
     * Implementations will do their own filtering.
     * Instead of asking a run for only the reads for a
     * specific sample or project, one builds
     * an implementation of SequencingResult to only
     * hand the parts of the run that are pertient.
     * @return
     */
    public Collection<SequencingRun> getSequencingRuns();
}
