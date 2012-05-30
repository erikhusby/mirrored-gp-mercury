package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broad.squid.services.TopicService.*;
import org.broad.squid.services.TopicService.TargetCoverageModel;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;

import javax.xml.bind.annotation.XmlAttribute;
import java.math.BigDecimal;
import java.util.EnumSet;
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


    public HybridSelectionExperiment(final ExperimentRequestSummary experimentRequestSummary ) {
        this(experimentRequestSummary, new DirectedPass());
        // Set defaults
        this.directedPass.setBaitSetID( DEFAULT_BAIT_SET_ID );
    }

    public HybridSelectionExperiment(final ExperimentRequestSummary experimentRequestSummary, final DirectedPass directedPass) {
        super(experimentRequestSummary, PMBPassType.DIRECTED);
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
            LanesCoverageModel lanesCoverageModel = new LanesCoverageModel(attemptedLanesCoverageModel);
            attemptedLanesCoverageModel.setAttemptedLanes( lanesCoverageModel.getLanesCoverage() );
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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof HybridSelectionExperiment)) return false;
        if (!super.equals(o)) return false;

        final HybridSelectionExperiment that = (HybridSelectionExperiment) o;

        if (directedPass != null ? !directedPass.equals(that.directedPass) : that.directedPass != null) return false;

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
