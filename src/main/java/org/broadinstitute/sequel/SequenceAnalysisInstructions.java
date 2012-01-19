package org.broadinstitute.sequel;

public interface SequenceAnalysisInstructions {

    /**
     * Currently the analysis_type
     * @return
     */
    public String getAnalysisMode();

    /**
     * Encodes name and version of the reference
     * in a single string.
     * @return
     */
    public ReferenceSequence getReferenceSequence();

    public AggregationInstructions getAggregationInstructions();
}
