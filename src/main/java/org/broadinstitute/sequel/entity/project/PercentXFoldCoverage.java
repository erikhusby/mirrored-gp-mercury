package org.broadinstitute.sequel.entity.project;

/**
 * This coverage goal says "sequence
 * until on average Y percent of the
 * regions I care about have at least
 * X-fold coverage.
 */
public class PercentXFoldCoverage implements CoverageGoal

    pri

    public PercentXFoldCoverage(int percentOfTargets,int coverageDepth) {

    }


    @Override
    public String coverageGoalToText() {
        return percentOfTargets + "% @ " + coverageDepth + "x";
    }
}
