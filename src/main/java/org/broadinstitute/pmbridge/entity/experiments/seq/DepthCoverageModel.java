package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.broad.squid.services.TopicService.ProgramPseudoDepthCoverageModel;

import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:40 AM
 */
public class DepthCoverageModel extends SeqCoverageModel {

    private final org.broad.squid.services.TopicService.ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel;
    public final static BigInteger DEFAULT_DEPTH = BigInteger.ZERO;


    public DepthCoverageModel() {
        this(DEFAULT_DEPTH);
    }

    public DepthCoverageModel(BigInteger desiredCoverage) {
        this(new org.broad.squid.services.TopicService.ProgramPseudoDepthCoverageModel());
        programPseudoDepthCoverageModel.setCoverageDesired(desiredCoverage);
    }

    public DepthCoverageModel(final ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel) {
        this.programPseudoDepthCoverageModel = programPseudoDepthCoverageModel;
    }

    public BigInteger getCoverageDesired() {
        return this.programPseudoDepthCoverageModel.getCoverageDesired();
    }

    public void setCoverageDesired(final BigInteger coverageDesired) {
        this.programPseudoDepthCoverageModel.setCoverageDesired( coverageDesired );
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.DEPTH;
     }
}
