package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *  Class to encapsulate a list of samples in Athena
 *  and any behaviour associated with the list.
 *  This is a list and not a set because duplicates samples
 *  are allowed in Athena.
 */

public class SampleSheet implements Serializable {

    private List<BillableSample> samples;

    SampleSheet() {
        samples = new ArrayList<BillableSample>();
    }

    public SampleSheet(final List<BillableSample> samples) {
        this.samples = samples;
    }

    public List<BillableSample> getSamples() {
        return samples;
    }

    public void addSample(final BillableSample sample) {
        samples.add(sample);
    }

    public int getUniqueParticipantCount() {
        Set<String> uniqueParticipants = new HashSet<String>();

        if (! isSheetEmpty() ) {
            if ( needsBspMetaData() ) {
                //TODO hmc fetch list of sample meta data from bsp via the fetcher
                throw new IllegalStateException("Not Yet Implemented");
            }

            for ( BillableSample billableSample : samples ) {
                String participantId = billableSample.getParticipantId();
                if (StringUtils.isNotBlank(participantId)) {
                    uniqueParticipants.add(participantId);
                }
            }
        }
        return uniqueParticipants.size();
    }

    /**
     * returns the number of unique participants
     * @return
     */
    public int getUniqueSampleCount() {
        int result = 0;
        Set<String> uniqueSamples = getUniqueSampleNames();
        return uniqueSamples.size();
    }

    private Set<String> getUniqueSampleNames() {
        Set<String> uniqueSamples = new HashSet<String>();
        for ( BillableSample billableSample : samples ) {
            String sampleName = billableSample.getSampleName();
            if (StringUtils.isNotBlank(sampleName)) {
                uniqueSamples.add(sampleName);
            }
        }
        return uniqueSamples;
    }

    public int getTotalSampleCount() {
        return samples.size();
    }

    public int getDuplicateCount() {
        return ( getTotalSampleCount() - getUniqueSampleCount());
    }

    public ImmutablePair getTumorNormalCounts() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
    }

    public ImmutablePair getMaleFemaleCounts() {
        //TODO
        throw new RuntimeException("Not Yet Implemented.");
    }

    /**
     * Returns true is any and all samples are of BSP Format.
     * Note will return false if there are no samples on the sheet.
     * @return
     */
    public boolean areAllSampleBSPFormat() {
        boolean result = true;
        if (! isSheetEmpty() ) {
            for ( BillableSample billableSample : samples) {
                if (! billableSample.isInBspFormat() ) {
                    result = false;
                    break;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    private boolean isSheetEmpty() {
        return (samples == null ) ||  samples.isEmpty();
    }

    private boolean needsBspMetaData() {
        boolean needed = false;
        if (! isSheetEmpty() ) {
            for ( BillableSample billableSample : samples ) {
                if ( billableSample.isInBspFormat() &&
                     ! billableSample.hasBSPDTOBeenInitialized() ) {
                    needed = true;
                    break;
                }
            }
        }
        return needed;
    }

}