package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import org.broad.squid.services.TopicService.*;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.gpinformatics.athena.entity.experiments.ExperimentType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 9:15 AM
 */
public class WholeGenomeExperiment extends SeqExperimentRequest {

    private final WholeGenomePass wholeGenomePass;
    public static final CoverageModelType DEFAULT_COVERAGE_MODEL = CoverageModelType.LANES;
    public static final BigInteger MINIMUM_WGS_DEPTH = BigInteger.ZERO;

    private static Set<CoverageModelType> coverageModelTypes =
            EnumSet.of(CoverageModelType.LANES, CoverageModelType.DEPTH);


    public WholeGenomeExperiment(final ExperimentRequestSummary experimentRequestSummary) {
        this(experimentRequestSummary, new WholeGenomePass());
    }

    public WholeGenomeExperiment(final ExperimentRequestSummary experimentRequestSummary, final WholeGenomePass wholeGenomePass) {
        super(experimentRequestSummary, ExperimentType.WholeGenomeSequencing);
        this.wholeGenomePass = wholeGenomePass;

    }

    protected AbstractPass getConcretePass() {
        return this.wholeGenomePass;
    }

    @Override
    public Set<CoverageModelType> getCoverageModelTypes() {
        return coverageModelTypes;
    }

    @Override
    protected CoverageAndAnalysisInformation createDefaultCoverageModel() {
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();

//        if (  DEFAULT_COVERAGE_MODEL.equals(CoverageModelType.LANES)) {
        AttemptedLanesCoverageModel attemptedLanesCoverageModel = new AttemptedLanesCoverageModel();
        LanesCoverageModel lanesCoverageModel = new LanesCoverageModel(attemptedLanesCoverageModel);
        attemptedLanesCoverageModel.setAttemptedLanes(lanesCoverageModel.getLanesCoverage());
        coverageAndAnalysisInformation.setAttemptedLanesCoverageModel(attemptedLanesCoverageModel);
        setSeqCoverageModel(lanesCoverageModel);
//        } else {
//            ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel = new ProgramPseudoDepthCoverageModel();
//            DepthCoverageModel depthCoverageModel = new DepthCoverageModel();
//            programPseudoDepthCoverageModel.setCoverageDesired(depthCoverageModel.getCoverageDesired());
//            coverageAndAnalysisInformation.setProgramPseudoDepthCoverageModel(programPseudoDepthCoverageModel);
//            setSeqCoverageModel( depthCoverageModel );
//        }

        //Set default analysis pipeline
        coverageAndAnalysisInformation.setAnalysisPipeline(AnalysisPipelineType.MPG);

        //Set default Aligner
        coverageAndAnalysisInformation.setAligner(AlignerType.BWA);

        //Set remaining defaults
        coverageAndAnalysisInformation.setSamplesPooled(Boolean.FALSE);
        coverageAndAnalysisInformation.setPlex(BigDecimal.ZERO);
        coverageAndAnalysisInformation.setKeepFastQs(Boolean.FALSE);
        coverageAndAnalysisInformation.setReferenceSequenceId(-1);

        return coverageAndAnalysisInformation;
    }


    @Override
    public AlignerType getAlignerType() {
        return getOrCreateCoverageAndAnalysisInformation().getAligner();
    }

    @Override
    public void setAlignerType(final AlignerType alignerType) {

        if (AlignerType.MAQ.equals(alignerType)) {
            throw new IllegalArgumentException(AlignerType.MAQ.toString() + " aligner type no longer supported. Please use BWA.");
        }
        getOrCreateCoverageAndAnalysisInformation().setAligner(alignerType);
    }

    @Override
    protected void setSeqCoverageModel(final SeqCoverageModel seqCoverageModel) {

        if ((seqCoverageModel != null) && getCoverageModelTypes().contains(seqCoverageModel.getConcreteModelType())) {

            // Explicit check for depth value for WholeGenomeExperiment
            if (CoverageModelType.DEPTH.equals(seqCoverageModel.getConcreteModelType())) {
                DepthCoverageModel depthCoverageModel = (DepthCoverageModel) seqCoverageModel;

                // If MINIMUM_WGS_DEPTH is numerically greater than the non-null depth arg then throw an exception.
                if (depthCoverageModel.getCoverageDesired() != null &&
                        ((MINIMUM_WGS_DEPTH.compareTo(depthCoverageModel.getCoverageDesired()) > 0))) {
                    String invalidVal = (depthCoverageModel.getCoverageDesired() != null) ? "" +
                            depthCoverageModel.getCoverageDesired().intValue() : "null";
                    throw new IllegalArgumentException("Invalid value " + invalidVal + " - " +
                            "Valid depth is any integer greater or equal to " + MINIMUM_WGS_DEPTH.intValue());
                }
            }
            this.seqCoverageModel = seqCoverageModel;
        } else {
            throwInvalidCoverageRuntimeException(seqCoverageModel.getConcreteModelType());
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WholeGenomeExperiment)) return false;
        if (!super.equals(o)) return false;

        final WholeGenomeExperiment that = (WholeGenomeExperiment) o;

//        if (!wholeGenomePass.equals(that.wholeGenomePass)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
