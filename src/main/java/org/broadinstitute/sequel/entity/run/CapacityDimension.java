package org.broadinstitute.sequel.entity.run;

/**
 * When we say that we have reserved 10% of
 * sequencing for NIAID, 30% of sequencing
 * for NHGRI, that's what we're calling
 * a capacity dimension.
 *
 * This is basically a reporting stub.
 */
public interface CapacityDimension {

    public String getCapacityCategoryName();
}
