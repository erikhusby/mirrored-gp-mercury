package org.broadinstitute.sequel.entity.project;

/**
 * This coverage goal says "sequence
 * until on average Y percent of the
 * regions I care about have at least
 * X-fold coverage.
 */
public class PercentXFoldCoverage implements CoverageGoal {
    
    private int percentOfTargets;
    
    private int coverageDepth;

    public PercentXFoldCoverage(int percentOfTargets,int coverageDepth) {
        if (percentOfTargets < 1) {
            throw new RuntimeException("percent of targets must be at least 1");
        }
        if (coverageDepth < 1) {
            throw new RuntimeException("Coverage depth must be at least 1");
        }
        this.percentOfTargets = percentOfTargets;
        this.coverageDepth = coverageDepth;
    }


    @Override
    public String coverageGoalToText() {
        return percentOfTargets + "% @ " + coverageDepth + "x";
    }
}
