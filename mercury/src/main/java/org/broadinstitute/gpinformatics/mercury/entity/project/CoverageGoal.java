package org.broadinstitute.gpinformatics.mercury.entity.project;

/**
 * A org.broadinstitute.gpinformatics.mercury.entity.project.CoverageGoal tells us how much
 * sequencing we're planning on doing.
 */
public interface CoverageGoal {

    /**
     * Human readable representation,
     * like "20x", "80% @ 20x", "2 runs",
     * "1 lane"
     * @return a String representation of the coverage details (As described in the description above)
     */
    public String coverageGoalToText();

    /**
     * Similar to {@link #coverageGoalToText()} coverageGoalToParsableText will return an interpreted version of the
     * values that make up the specific implementation of CoverageGoal that this instance represents.  The values
     * will (if necessary) be comma separated in the order specified by the specific instance.
     * @return comma separated (if necessary) String representing the CoverageGoal values
     */
    public String coverageGoalToParsableText();
}
