package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 *
 * BilledNotBilledCounts is a helper tuple to encapsulate the counts, with respect to an order, for sample Items that
 * have been billed and sample items that have not been billed
 *
 *
 * @author Scott Matthews
 *         Date: 10/10/12
 *         Time: 10:05 AM
 */
public class BilledNotBilledCounts {
    private ImmutablePair<Integer,Integer> pair;

    public BilledNotBilledCounts(int billedCount, int notBilledCount) {
        pair = new ImmutablePair<Integer, Integer>(billedCount, notBilledCount);
    }

    /**
     * getBilledCount returns the count of all samples, for a particular order, that have already been billed
     * @return an integer representing the number of billed samples
     */
    public int getBilledCount() {
        return pair.getLeft();
    }

    /**
     * getNotBilledCount returns the count of all samples, for a particular order, that have not yet been billed
     * @return an integer representing the number of samples that have not been billed
     */
    public int getNotBilledCount() {
        return pair.getRight();
    }

}
