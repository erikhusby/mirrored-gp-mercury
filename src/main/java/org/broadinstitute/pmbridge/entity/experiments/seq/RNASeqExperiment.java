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
    public static final CoverageModelType DEFAULT_COVERAGE_MODEL = CoverageModelType.LANES;
    private static Set<CoverageModelType> coverageModelTypes =
            EnumSet.of(CoverageModelType.LANES, CoverageModelType.PFREADS);

    public RNASeqExperiment(final ExperimentRequestSummary experimentRequestSummary ) {
        this(experimentRequestSummary, new RNASeqPass());
        // Set defaults
        this.rnaSeqPass.setProtocol(RNASeqProtocolType.TRU_SEQ);
        this.rnaSeqPass.setTranscriptomeReferenceSequenceID( new Long(-1) );
    }

    public RNASeqExperiment(final ExperimentRequestSummary experimentRequestSummary, final RNASeqPass rnaSeqPass) {
        super(experimentRequestSummary, PMBPassType.RNASeq );
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
        //TODO hmc Need to request TopHat be added to the AlignerType.
        if (AlignerType.MAQ.equals(alignerType) || AlignerType.BWA.equals(alignerType)) {
            throw new IllegalStateException(alignerType.toString() + " aligner type no longer supported. Please use TopHat.");
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
    public Long getTranscriptomeReferenceSequenceID() {
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
            if (eValue.name().equalsIgnoreCase(str))
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

        //Set default Aligner
        coverageAndAnalysisInformation.setAligner( AlignerType.BWA );

        //Set remaining defaults
        coverageAndAnalysisInformation.setSamplesPooled(Boolean.FALSE);
        coverageAndAnalysisInformation.setPlex(BigDecimal.ZERO);
        coverageAndAnalysisInformation.setKeepFastQs(Boolean.FALSE);
        coverageAndAnalysisInformation.setReferenceSequenceId( -1 );

        return coverageAndAnalysisInformation;
    }
}
