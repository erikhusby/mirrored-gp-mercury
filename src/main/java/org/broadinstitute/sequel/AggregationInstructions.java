package org.broadinstitute.sequel;

/**
 * How should Picard/Zamboni aggregate
 * the data?
 */
public interface AggregationInstructions {

    public enum AggregationUnit {
        LIBRARY,
        SAMPLE_AND_PROJECT,
        EXPERIMENT_AND_CONDITIONS
    }

    public AggregationUnit getAggregationUnit();

    /**
     * For {@link AggregationUnit#LIBRARY}: Solexa-123
     * For {@link AggregationUnit#SAMPLE_AND_PROJECT}: G585/NA12878
     * For {@link AggregationUnit#EXPERIMENT_AND_CONDITIONS}: DEV-123/Condition1-Condition2-Condition3
     * @return
     */
    public String getAggregationName();
    
}
