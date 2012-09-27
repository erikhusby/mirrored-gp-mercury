package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import org.broadinstitute.gpinformatics.mercury.boundary.ProgramPseudoDepthCoverageModel;

import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:40 AM
 */
public class DepthCoverageModel extends SeqCoverageModel {

    private final org.broadinstitute.gpinformatics.mercury.boundary.ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel;
    public final static BigInteger DEFAULT_DEPTH = BigInteger.ZERO;


    public DepthCoverageModel() {
        this(DEFAULT_DEPTH);
    }

    public DepthCoverageModel(BigInteger desiredCoverage) {
        this(new org.broadinstitute.gpinformatics.mercury.boundary.ProgramPseudoDepthCoverageModel());
        programPseudoDepthCoverageModel.setCoverageDesired(desiredCoverage);
    }

    public DepthCoverageModel(final ProgramPseudoDepthCoverageModel programPseudoDepthCoverageModel) {
        this.programPseudoDepthCoverageModel = programPseudoDepthCoverageModel;
    }

    public BigInteger getCoverageDesired() {
        return this.programPseudoDepthCoverageModel.getCoverageDesired();
    }

    public void setCoverageDesired(final BigInteger coverageDesired) {
        this.programPseudoDepthCoverageModel.setCoverageDesired(coverageDesired);
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.DEPTH;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DepthCoverageModel)) return false;

        final DepthCoverageModel that = (DepthCoverageModel) o;

        if (programPseudoDepthCoverageModel != null ? !programPseudoDepthCoverageModel.equals(that.programPseudoDepthCoverageModel) : that.programPseudoDepthCoverageModel != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return programPseudoDepthCoverageModel != null ? programPseudoDepthCoverageModel.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "DepthCoverageModel{" +
                "programPseudoDepthCoverageModel=" +
                ((programPseudoDepthCoverageModel == null) ? "null" : programPseudoDepthCoverageModel.getCoverageDesired().toString()) +
                '}';
    }
}
