package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * BspNonBspSampleCount is a helper tuple to encapsulate the counts, with respect to an order, for sample Items that
 * have come from the BSP platform and sampes that have not come from BSP
 *
 * @author Scott Matthews
 *         Date: 10/9/12
 *         Time: 1:37 PM
 */
public class BspNonBspSampleCount {
    private ImmutablePair<Integer,Integer> pair;

    public BspNonBspSampleCount(int bspSampleCount, int nonBspSampleCount) {
        pair = new ImmutablePair<>(bspSampleCount, nonBspSampleCount);
    }

    /**
     * getBspSampleCount provides the user with visibility, for a given order, of how many samples are from BSP
     *
     * @return an integer representing the total number of BSP samples for an order
     */
    public int getBspSampleCount() {
        return pair.getLeft();
    }

    /**
     * getNonBspSampleCount provides the user with visibility, for a given order, of how many samples are not from BSP
     *
     * @return an integer representing the total number of non BSP samples for an order
     */
    public int getNonBspSampleCount() {
        return pair.getRight();
    }
}
