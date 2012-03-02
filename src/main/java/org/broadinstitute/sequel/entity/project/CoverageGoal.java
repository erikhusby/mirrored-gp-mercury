package org.broadinstitute.sequel.entity.project;

/**
 * A org.broadinstitute.sequel.entity.project.CoverageGoal tells us how much
 * sequencing we're planning on doing.
 */
public interface CoverageGoal {

    /**
     * Human readable representation,
     * like "20x", "80% @ 20x", "2 runs",
     * "1 lane"
     * @return
     */
    public String coverageGoalToText();
}
