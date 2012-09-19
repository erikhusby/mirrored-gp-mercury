package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:59 AM
 */
public class MeanTargetCoverageModel extends SeqCoverageModel {

    private final org.broad.squid.services.TopicService.MeanTargetCoverageModel meanTargetCoverageModel;
    public final static BigInteger DEFAULT_COVERAGE = BigInteger.ZERO;

    public MeanTargetCoverageModel() {
        this(DEFAULT_COVERAGE);
    }

    public MeanTargetCoverageModel(final BigInteger meanTargetCoverage) {
        this(new org.broad.squid.services.TopicService.MeanTargetCoverageModel());
        this.meanTargetCoverageModel.setCoverageDesired(meanTargetCoverage);
    }

    public MeanTargetCoverageModel(final org.broad.squid.services.TopicService.MeanTargetCoverageModel meanTargetCoverageModel) {
        this.meanTargetCoverageModel = meanTargetCoverageModel;
    }

    public BigInteger getMeanTargetCoverage() {
        return this.meanTargetCoverageModel.getCoverageDesired();
    }

    public void setMeanTargetCoverage(final BigInteger meanTargetCoverage) {
        this.meanTargetCoverageModel.setCoverageDesired(meanTargetCoverage);
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.MEANTARGETCOVERAGE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof MeanTargetCoverageModel)) return false;

        final MeanTargetCoverageModel that = (MeanTargetCoverageModel) o;

        if (meanTargetCoverageModel != null ? !meanTargetCoverageModel.equals(that.meanTargetCoverageModel) : that.meanTargetCoverageModel != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return meanTargetCoverageModel != null ? meanTargetCoverageModel.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "MeanTargetCoverageModel{" +
                "meanTargetCoverageModel=" +
                ((meanTargetCoverageModel == null) ? "null" : meanTargetCoverageModel.getCoverageDesired().toString()) +
                '}';
    }
}
