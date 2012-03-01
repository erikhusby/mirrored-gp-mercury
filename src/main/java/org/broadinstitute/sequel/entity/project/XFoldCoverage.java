package org.broadinstitute.sequel.entity.project;


/**
 * X-fold coverage is coverage expressed in
 * terms of on average how many independent readings
 * cover a target base.
 */
public class XFoldCoverage implements CoverageGoal {
    
    private int coverageDepth;
    
    public XFoldCoverage(int coverageDepth) {
        if (coverageDepth < 1) {
            throw new RuntimeException("Coverage depth must be at least 1.");
        }
        this.coverageDepth = coverageDepth;
    }
    
    public int getCoverageDepth() {
        return coverageDepth;
    }

    @Override
    public String coverageGoalToText() {
        return coverageDepth + "x";
    }
}
