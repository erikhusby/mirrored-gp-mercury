package org.broadinstitute.gpinformatics.mercury.entity.project;

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
        this.numberOfReadsThatHaveAMate = numberOfReadsThatHaveAMate;
    }

    @Override
    public String coverageGoalToText() {
        return this.numberOfReadsThatHaveAMate + " reads which have a mate";
    }

    @Override
    public String coverageGoalToParsableText() {
        return String.valueOf(this.numberOfReadsThatHaveAMate);
    }
}
