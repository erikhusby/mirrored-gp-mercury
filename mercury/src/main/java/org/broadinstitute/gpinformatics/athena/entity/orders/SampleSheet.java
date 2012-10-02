package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.Serializable;
import java.util.List;

/**
 *  Class to encapsulate a list of samples in Athena
 *  and any behaviour associated with the list.
 *  This is a list and not a set because duplicates samples
 *  are allowed in Athena.
 *
 */

public class SampleSheet implements Serializable {
    private List<AthenaSample> samples;

    public SampleSheet() {
    }

    public List<AthenaSample> getSamples() {
        return samples;
    }

    public void setSamples(final List<AthenaSample> samples) {
        this.samples = samples;
    }

    public int getUniqueParticipantCount() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
//        return 0;
    }

    public int getUniqueSampleCount() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
//        return 0;
    }

    public int getTotalSampleCount() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
//        return 0;
    }

    public int getDuplicateCount() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
//         return 0;
    }

    public ImmutablePair getDiseaseNormalCounts() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
//            return 0;
    }

    public ImmutablePair getGenderCount() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
//         return 0;
    }

    public boolean areAllSampleBSPFormat() {
        boolean result = true;
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
    }
}