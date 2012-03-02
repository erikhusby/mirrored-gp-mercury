package org.broadinstitute.sequel.entity.project;

/**
 * This coverage says "sequence enough
 * stuff so that I have x reads
 * which have mate pairs."
 */
public class PairedReadCoverage implements CoverageGoal {
    
    private int numberOfReadsThatHaveAMate;
    
    public PairedReadCoverage(int numberOfReadsThatHaveAMate) {
        if (numberOfReadsThatHaveAMate < 1) {
            throw new RuntimeException("The number of reads that has a mate must be at least 1.");
        }
        numberOfReadsThatHaveAMate = numberOfReadsThatHaveAMate;
    }

    @Override
    public String coverageGoalToText() {
        return numberOfReadsThatHaveAMate + " reads which have a mate";
    }
}
