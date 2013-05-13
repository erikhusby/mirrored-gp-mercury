package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import java.util.Comparator;

/**
 * DTO for one lab batch and the number of vessels in that batch, for a given container.
 */
public class LabBatchComposition {
    private LabBatch labBatch;
    private int count;
    private int denominator;

    public LabBatchComposition(LabBatch labBatch, int count, int denominator) {
        this.labBatch = labBatch;
        this.count = count;
        this.denominator = denominator;
    }

    public LabBatch getLabBatch() {
        return labBatch;
    }

    public int getCount() {
        return count;
    }

    public void addCount() {
        ++count;
    }

    public int getDenominator() {
        return denominator;
    }

    /**
     * Ranks LabBatchCompositions by decreasing vessel count.
     */
    public static Comparator<LabBatchComposition> HIGHEST_COUNT_FIRST = new Comparator<LabBatchComposition>() {
        public int compare(LabBatchComposition lbc1, LabBatchComposition lbc2) {
            return lbc2.getCount() - lbc1.getCount();
        }
    };
}
