package org.broadinstitute.sequel.entity.project;

/**
 * This coverage goal says "sequence x lanes for me."
 */
public class NumberOfLanesCoverage implements CoverageGoal {
    
    private int numberOfLanes;

    public NumberOfLanesCoverage(int numberOfLanes) {
        if (numberOfLanes < 0) {
            throw new RuntimeException("number of lanes must be > 0");
        }
        this.numberOfLanes = numberOfLanes;
    }

    @Override
    public String coverageGoalToText() {
        return numberOfLanes + " lanes";
    }
}
