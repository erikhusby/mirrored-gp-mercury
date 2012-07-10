package org.broadinstitute.pmbridge.entity.experiments.seq;

import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:59 AM
 */
public class TargetCoverageModel extends SeqCoverageModel {

    private final org.broad.squid.services.TopicService.TargetCoverageModel targetCoverageModel;
    public final static BigInteger DEFAULT_DEPTH = BigInteger.ZERO;
    public final static BigInteger DEFAULT_PERCENT = BigInteger.ZERO;

    public TargetCoverageModel() {
        this(DEFAULT_DEPTH, DEFAULT_PERCENT);
    }

    public TargetCoverageModel(final BigInteger targetDepth, final BigInteger targetPercent) {
        this(new org.broad.squid.services.TopicService.TargetCoverageModel());
        this.targetCoverageModel.setDepth(targetDepth);
        this.targetCoverageModel.setCoveragePercentage(targetPercent);
    }

    public TargetCoverageModel(final org.broad.squid.services.TopicService.TargetCoverageModel targetCoverageModel) {
        this.targetCoverageModel = targetCoverageModel;
    }

    public BigInteger getPercent() {
        return this.targetCoverageModel.getCoveragePercentage();
    }

    public void setPercent(final BigInteger percent) {
        // Check if percent is greater than or equal to 0   AND is percent is less than or equal to 100.
        if (!((BigInteger.ZERO.compareTo(percent) >= 0) && (BigInteger.TEN.multiply(BigInteger.TEN).compareTo(percent) <= 0))) {
            throw new IllegalArgumentException("percentage must be an integer between 0 and 100.");
        }
        this.targetCoverageModel.setCoveragePercentage(percent);
    }

    public BigInteger getDepth() {
        return this.targetCoverageModel.getDepth();
    }

    public void setDepth(final BigInteger depth) {
        this.targetCoverageModel.setDepth(depth);
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.TARGETCOVERAGE;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TargetCoverageModel)) return false;

        final TargetCoverageModel that = (TargetCoverageModel) o;

        if (targetCoverageModel != null ? !targetCoverageModel.equals(that.targetCoverageModel) : that.targetCoverageModel != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return targetCoverageModel != null ? targetCoverageModel.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TargetCoverageModel{" +
                "targetCoverageModel=" +
                ((targetCoverageModel == null) ? "null" : targetCoverageModel.toString()) +
                '}';
    }
}
