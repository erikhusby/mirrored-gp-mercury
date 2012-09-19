package org.broadinstitute.gpinformatics.athena.entity.experiments.seq;

import java.math.BigInteger;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 12:14 PM
 */
public class PFReadsCoverageModel extends SeqCoverageModel {

    private final org.broadinstitute.gpinformatics.mercury.boundary.PFReadsCoverageModel pfReadsCoverageModel;
    public final static BigInteger DEFAULT_READS = BigInteger.ONE;
    public final static BigInteger MINIMUM_READS = DEFAULT_READS;

    public PFReadsCoverageModel() {
        this(DEFAULT_READS);
    }

    public PFReadsCoverageModel(final BigInteger readsDesired) {
        this(new org.broadinstitute.gpinformatics.mercury.boundary.PFReadsCoverageModel());
        this.pfReadsCoverageModel.setReadsDesired(readsDesired);
    }

    public PFReadsCoverageModel(final org.broadinstitute.gpinformatics.mercury.boundary.PFReadsCoverageModel pfReadsCoverageModel) {
        this.pfReadsCoverageModel = pfReadsCoverageModel;
    }

    public BigInteger getReadsDesired() {
        return this.pfReadsCoverageModel.getReadsDesired();
    }

    public void setReadsDesired(final BigInteger readsDesired) {

        // If MINIMUM_READS is numerically greater than the non-null reads arg then throw an exception.
        if (readsDesired != null &&
                ((MINIMUM_READS.compareTo(readsDesired) > 0))) {
            String invalidVal = (readsDesired != null) ? "" + readsDesired.intValue() : "null";
            throw new IllegalArgumentException("Invalid pfread value " + invalidVal + " " +
                    "Valid values are any integer greater or equal to " + MINIMUM_READS.intValue());
        }
        this.pfReadsCoverageModel.setReadsDesired(readsDesired);
    }

    @Override
    protected CoverageModelType getConcreteModelType() {
        return CoverageModelType.PFREADS;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof PFReadsCoverageModel)) return false;

        final PFReadsCoverageModel that = (PFReadsCoverageModel) o;

        if (pfReadsCoverageModel != null ? !pfReadsCoverageModel.equals(that.pfReadsCoverageModel) : that.pfReadsCoverageModel != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return pfReadsCoverageModel != null ? pfReadsCoverageModel.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "PFReadsCoverageModel{" +
                "pfReadsCoverageModel=" +
                ((pfReadsCoverageModel == null) ? "null" : pfReadsCoverageModel.getReadsDesired().toString()) +
                '}';
    }
}
