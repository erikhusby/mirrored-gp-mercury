package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.broad.squid.services.TopicService.*;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 9:16 AM
 */
public class RNASeqExperiment extends SeqExperimentRequest {

    private final RNASeqPass rnaSeqPass;
    public static final RNASeqProtocolType DEFAULT_RNA_PROTOCOL = RNASeqProtocolType.TRU_SEQ;
    public static final CoverageModelType DEFAULT_COVERAGE_MODEL = CoverageModelType.LANES;
    public static final Long DEFAULT_REFERENCE_SEQUENCE_ID = new Long(-1);
    private static Set<CoverageModelType> coverageModelTypes =
            EnumSet.of(CoverageModelType.LANES, CoverageModelType.PFREADS);

    public RNASeqExperiment(final ExperimentRequestSummary experimentRequestSummary ) {
        this(experimentRequestSummary, new RNASeqPass());
        // Set defaults
        CoverageAndAnalysisInformation coverageAndAnalysisInformation = new CoverageAndAnalysisInformation();
        coverageAndAnalysisInformation.setAligner(AlignerType.TOPHAT);
        this.rnaSeqPass.setCoverageAndAnalysisInformation(coverageAndAnalysisInformation);
        this.rnaSeqPass.setProtocol(DEFAULT_RNA_PROTOCOL);
        this.rnaSeqPass.setTranscriptomeReferenceSequenceID(DEFAULT_REFERENCE_SEQUENCE_ID);
    }

    public RNASeqExperiment(final ExperimentRequestSummary experimentRequestSummary, final RNASeqPass rnaSeqPass) {
        super(experimentRequestSummary, PassType.RNASEQ );
        this.rnaSeqPass = rnaSeqPass;
    }

    @Override
    protected AbstractPass getConcretePass() {
        return this.rnaSeqPass;
    }

    @Override
    public Set<CoverageModelType> getCoverageModelTypes() {
        return coverageModelTypes;
    }

    @Override
    public AlignerType getAlignerType() {
        return getOrCreateCoverageAndAnalysisInformation().getAligner();
    }

    @Override
    public void setAlignerType(final AlignerType alignerType) {
        if (! AlignerType.TOPHAT.equals(alignerType) ) {
            throw new IllegalArgumentException(alignerType.toString() + " aligner type no longer supported. Please use TopHat.");
        }
        getOrCreateCoverageAndAnalysisInformation().setAligner( alignerType );
    }

    /**
     * Gets the value of the transcriptomeReferenceSequenceID property.
     * @return
     *     possible object is
     *     {@link Long }
     *
     */
    public Long  getTranscriptomeReferenceSequenceID() {
        return rnaSeqPass.getTranscriptomeReferenceSequenceID();
    }

    /**
     * Sets the value of the transcriptomeReferenceSequenceID property.
     *
     * @param value
     *     allowed object is
     *     {@link Long }
     *
     */
    public void setTranscriptomeReferenceSequenceID(Long value) {
        rnaSeqPass.setTranscriptomeReferenceSequenceID( value );
    }


    public String getRNAProtocol() {
        String rnaSeqProtocol = null;
        RNASeqProtocolType rnaSeqProtocolType = rnaSeqPass.getProtocol();
        if ( null != rnaSeqProtocolType) {
            rnaSeqProtocol = rnaSeqProtocolType.value();
        }
        return rnaSeqProtocol;
    }

    public void setRNAProtocol(final String rnaProtocol) {
        RNASeqProtocolType rnaSeqProtocolType = convertToRNASeqProtocolEnumElseNull(rnaProtocol);
        if ( rnaSeqProtocolType == null ) {
            throw new IllegalArgumentException("Unrecognized RNA protocol type.");
        }
        rnaSeqPass.setProtocol(rnaSeqProtocolType );
    }

    public static RNASeqProtocolType convertToRNASeqProtocolEnumElseNull(String str) {
        for (RNASeqProtocolType eValue : RNASeqProtocolType.values()) {
            if (eValue.value().equalsIgnoreCase(str))
                return eValue;
        }
        return null;
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
            org.broad.squid.services.TopicService.PFReadsCoverageModel squidPFReadsCoverageModel =
                    new org.broad.squid.services.TopicService.PFReadsCoverageModel();
            org.broadinstitute.pmbridge.entity.experiments.seq.PFReadsCoverageModel seqPFReadsCoverageModel =
                    new org.broadinstitute.pmbridge.entity.experiments.seq.PFReadsCoverageModel();
            squidPFReadsCoverageModel.setReadsDesired( seqPFReadsCoverageModel.getReadsDesired() );
            setSeqCoverageModel( seqPFReadsCoverageModel );
        }

        //Set default analysis pipeline
        coverageAndAnalysisInformation.setAnalysisPipeline( AnalysisPipelineType.MPG  );

        //Set default Aligner for RNASeq
        coverageAndAnalysisInformation.setAligner( AlignerType.TOPHAT );

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
        if (!(o instanceof RNASeqExperiment)) return false;
        if (!super.equals(o)) return false;

        final RNASeqExperiment that = (RNASeqExperiment) o;

        if (getRNAProtocol() != null ? !getRNAProtocol().equals(that.getRNAProtocol()) : that.getRNAProtocol() != null) return false;
        if (getTranscriptomeReferenceSequenceID() != null
                ? !getTranscriptomeReferenceSequenceID().equals(that.getTranscriptomeReferenceSequenceID())
                : that.getTranscriptomeReferenceSequenceID() != null) return false;
        if (getAlignerType() != null ? !getAlignerType().equals(that.getAlignerType()) : that.getAlignerType() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getRNAProtocol() != null ? getRNAProtocol().hashCode() : 0);
        result = 31 * result + (getTranscriptomeReferenceSequenceID() != null ? getTranscriptomeReferenceSequenceID().intValue() : 0);
        result = 31 * result + (getAlignerType() != null ? getAlignerType().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RNASeqExperiment{" +
                "RNAProtocol=" + getRNAProtocol() +
                "TranscriptomeReferenceSequenceID=" + getTranscriptomeReferenceSequenceID() +
                "AlignerType=" + getAlignerType() +
                '}';
    }
}
