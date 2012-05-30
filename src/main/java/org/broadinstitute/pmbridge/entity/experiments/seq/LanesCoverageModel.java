package org.broadinstitute.pmbridge.entity.experiments.seq;

import org.broad.squid.services.TopicService.AttemptedLanesCoverageModel;

import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:31 AM
 */
public class LanesCoverageModel extends SeqCoverageModel {

    private final org.broad.squid.services.TopicService.AttemptedLanesCoverageModel attemptedLanesCoverageModel;
    public final static BigDecimal DEFAULT_LANES = new BigDecimal(8);


    public LanesCoverageModel() {
        this(DEFAULT_LANES);
    }

    public LanesCoverageModel(final BigDecimal lanesCoverage) {
        this(new org.broad.squid.services.TopicService.AttemptedLanesCoverageModel());
        this.attemptedLanesCoverageModel.setAttemptedLanes(lanesCoverage);
    }

    public LanesCoverageModel(final AttemptedLanesCoverageModel attemptedLanesCoverageModel) {
        this.attemptedLanesCoverageModel = attemptedLanesCoverageModel;
    }

    public BigDecimal getLanesCoverage() {
        return attemptedLanesCoverageModel.getAttemptedLanes();
    }

    public void setLanesCoverage(final BigDecimal lanesCoverage) {
        this.attemptedLanesCoverageModel.setAttemptedLanes( lanesCoverage );
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.LANES;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof LanesCoverageModel)) return false;

        final LanesCoverageModel that = (LanesCoverageModel) o;

        if (attemptedLanesCoverageModel != null ? !attemptedLanesCoverageModel.equals(that.attemptedLanesCoverageModel) : that.attemptedLanesCoverageModel != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return attemptedLanesCoverageModel != null ? attemptedLanesCoverageModel.hashCode() : 0;
    }
}
