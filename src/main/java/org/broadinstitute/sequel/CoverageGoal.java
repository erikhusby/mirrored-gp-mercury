package org.broadinstitute.sequel;

/**
 * A org.broadinstitute.sequel.CoverageGoal tells us how much
 * sequencing we're planning on doing.
 */
public interface CoverageGoal {

    /**
     * Human readable representation,
     * like "20x", "80% @ 20x", "2 runs",
     * "1 lane"
     * @return
     */
    public String toText();
}
