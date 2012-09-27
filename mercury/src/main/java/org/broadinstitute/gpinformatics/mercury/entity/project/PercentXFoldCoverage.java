package org.broadinstitute.gpinformatics.mercury.entity.project;

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
        return this.percentOfTargets + "% @ " + this.coverageDepth + "x";
    }

    /**
     * {@inheritDoc}
     *
     * The order for PercentXFoldCoverage is as follows:
     *
     * percentOfTargets
     * then Comma
     * then coverageDepth
     *
     * @return
     */
    @Override
    public String coverageGoalToParsableText() {
        final StringBuilder valuesAsString = new StringBuilder();

        valuesAsString.append(this.percentOfTargets);
        valuesAsString.append(",");
        valuesAsString.append(this.coverageDepth);
        
        return valuesAsString.toString();
    }
}
