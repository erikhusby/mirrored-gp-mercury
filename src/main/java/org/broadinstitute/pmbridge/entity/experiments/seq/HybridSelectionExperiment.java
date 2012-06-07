package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.broad.squid.services.TopicService.*;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 9:48 AM
 */
public class HybridSelectionExperiment extends SeqExperimentRequest {


    private final DirectedPass directedPass;

    public static final CoverageModelType DEFAULT_COVERAGE_MODEL = CoverageModelType.LANES;
    public static final Long DEFAULT_BAIT_SET_ID = new Long(-1);
    private static Set<CoverageModelType> coverageModelTypes =
            EnumSet.of(CoverageModelType.LANES, CoverageModelType.TARGETCOVERAGE, CoverageModelType.MEANTARGETCOVERAGE);

    private static final HashSet<BigInteger> supportedDepths = new HashSet<BigInteger>();
    static {
        supportedDepths.add( new BigInteger("2") );
        supportedDepths.add( BigInteger.TEN );
        supportedDepths.add( new BigInteger("20") );
        supportedDepths.add( new BigInteger("30") );
    }

    public HybridSelectionExperiment(final ExperimentRequestSummary experimentRequestSummary ) {
        this(experimentRequestSummary, new DirectedPass());
        // Set defaults
        this.directedPass.setBaitSetID( DEFAULT_BAIT_SET_ID );
    }

    public HybridSelectionExperiment(final ExperimentRequestSummary experimentRequestSummary, final DirectedPass directedPass) {
        super(experimentRequestSummary, PassType.DIRECTED);
        this.directedPass = directedPass;
    }

    @Override
    protected AbstractPass getConcretePass() {
        return directedPass;
    }

    @Override
    public Set<CoverageModelType> getCoverageModelTypes() {
        return coverageModelTypes;
    }

    public Long getBaitSetID() {
        return  directedPass.getBaitSetID();
    }

    public void setBaitSetID(final Long baitSetID) {
        this.directedPass.setBaitSetID( baitSetID );
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
        getOrCreateCoverageAndAnalysisInformation().setAligner( alignerType );
    }

    @Override
    protected CoverageAndAnalysisInformation createDefaultCoverageModel() {
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();

        if (  DEFAULT_COVERAGE_MODEL.equals(CoverageModelType.LANES)) {
            AttemptedLanesCoverageModel attemptedLanesCoverageModel = new AttemptedLanesCoverageModel();
            attemptedLanesCoverageModel.setAttemptedLanes( LanesCoverageModel.DEFAULT_LANES );
//            attemptedLanesCoverageModel.setAttemptedLanes( lanesCoverageModel.getLanesCoverage() );
            LanesCoverageModel lanesCoverageModel = new LanesCoverageModel(attemptedLanesCoverageModel);
            coverageAndAnalysisInformation.setAttemptedLanesCoverageModel(attemptedLanesCoverageModel);
            setSeqCoverageModel( lanesCoverageModel );
        } else {
            org.broadinstitute.pmbridge.entity.experiments.seq.TargetCoverageModel seqTargetCoverageModel =
                    new org.broadinstitute.pmbridge.entity.experiments.seq.TargetCoverageModel();

            org.broad.squid.services.TopicService.TargetCoverageModel squidTargetCoverageModel =
                    new org.broad.squid.services.TopicService.TargetCoverageModel();
            squidTargetCoverageModel.setCoveragePercentage(seqTargetCoverageModel.getPercent());
            squidTargetCoverageModel.setDepth(seqTargetCoverageModel.getDepth());
            coverageAndAnalysisInformation.setTargetCoverageModel(squidTargetCoverageModel);

            setSeqCoverageModel( seqTargetCoverageModel );
        }

        //Set default analysis pipeline
        coverageAndAnalysisInformation.setAnalysisPipeline( AnalysisPipelineType.MPG  );

        //Set default Aligner
        coverageAndAnalysisInformation.setAligner( AlignerType.BWA );

        //Set remaining defaults
        coverageAndAnalysisInformation.setSamplesPooled(Boolean.FALSE);
        coverageAndAnalysisInformation.setPlex(BigDecimal.ZERO);
        coverageAndAnalysisInformation.setKeepFastQs(Boolean.FALSE);
        coverageAndAnalysisInformation.setReferenceSequenceId( -1 );

        return coverageAndAnalysisInformation;
    }


    @Override
    protected void setSeqCoverageModel(final SeqCoverageModel seqCoverageModel) {

        if ( (seqCoverageModel != null) && getCoverageModelTypes().contains( seqCoverageModel.getConcreteModelType()) ) {

            // Explicit check for depths values specific to HybridSelection
            if ( CoverageModelType.DEPTH.equals( seqCoverageModel.getConcreteModelType()) ) {
                TargetCoverageModel targetCoverageModel = (TargetCoverageModel) seqCoverageModel;
                if (! supportedDepths.contains( targetCoverageModel.getDepth()) ) {
                    throwInvalidDepthException( targetCoverageModel.getDepth() );
                }
            }
            this.seqCoverageModel = seqCoverageModel;
        } else {
            throwInvalidCoverageRuntimeException(seqCoverageModel.getConcreteModelType());
        }
    }




    private void throwInvalidDepthException(BigInteger depth) {
        String invalidVal = ( depth != null ) ? "" + depth.intValue() : "null";
        StringBuilder msg = new StringBuilder( "Invalid depth " + invalidVal + " - " +
                "Valid depths for HybridSelection are :" );
        for ( BigInteger supportedDepth : supportedDepths ) {
            msg.append(" ").append( supportedDepth.intValue() );
        }
        throw new IllegalArgumentException(msg.toString());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof HybridSelectionExperiment)) return false;
        if (!super.equals(o)) return false;

        final HybridSelectionExperiment that = (HybridSelectionExperiment) o;

        if (getBaitSetID() != null ? !getBaitSetID().equals(that.getBaitSetID()) : that.getBaitSetID() != null) return false;
        if (getAlignerType() != null ? !getAlignerType().equals(that.getAlignerType()) : that.getAlignerType() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getAlignerType() != null ? getAlignerType().hashCode() : 0);
        result = 31 * result + (getBaitSetID() != null ? getBaitSetID().intValue() : 0);

        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
