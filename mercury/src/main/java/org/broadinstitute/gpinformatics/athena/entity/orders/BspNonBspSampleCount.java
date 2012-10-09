package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * @author Scott Matthews
 *         Date: 10/9/12
 *         Time: 1:37 PM
 */
public class BspNonBspSampleCount {
    private ImmutablePair<Integer,Integer> pair;

    public BspNonBspSampleCount(int bspSampleCount, int nonBspSampleCount) {
        pair = new ImmutablePair<Integer, Integer>(bspSampleCount, nonBspSampleCount);
    }

    public int getBspSampleCount() {
        return pair.getLeft();
    }

    public int getNonBspSampleCount() {
        return pair.getRight();
    }
}
