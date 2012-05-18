package org.broadinstitute.pmbridge.entity.experiments.seq;

import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 12:14 PM
 */
public class PFReadsCoverageModel extends SeqCoverageModel{

    private final org.broad.squid.services.TopicService.PFReadsCoverageModel pfReadsCoverageModel;
    public final static BigInteger DEFAULT_READS = BigInteger.ZERO;

    public PFReadsCoverageModel() {
        this(DEFAULT_READS);
    }

    public PFReadsCoverageModel(final BigInteger readsDesired) {
        this(new org.broad.squid.services.TopicService.PFReadsCoverageModel());
        this.pfReadsCoverageModel.setReadsDesired(readsDesired);
    }

    public PFReadsCoverageModel(final org.broad.squid.services.TopicService.PFReadsCoverageModel pfReadsCoverageModel) {
        this.pfReadsCoverageModel = pfReadsCoverageModel;
    }

    public BigInteger getReadsDesired() {
        return this.pfReadsCoverageModel.getReadsDesired();
    }

    public void setReadsDesired(final BigInteger readsDesired) {
        this.pfReadsCoverageModel.setReadsDesired(readsDesired);
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.PFREADS;
    }
}
