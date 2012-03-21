package org.broadinstitute.sequel.entity.analysis;

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

    /**
     * In squid, PMs have to put all samples from the same
     * patient together in a single project to enable co-cleaning
     * because picard/zamboni co-cleans all samples in a project.
     *
     * We want to change this so that PMs can make a project
     * be anything they want, but configure co-cleaning independently.
     * @return
     */
    public CoCleaningInstructions getCoCleaningInstructions();
}
