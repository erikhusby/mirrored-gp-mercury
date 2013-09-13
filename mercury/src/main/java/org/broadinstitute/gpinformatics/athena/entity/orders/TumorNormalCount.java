package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 2:59 PM
 */
public class TumorNormalCount {
    private ImmutablePair<Integer,Integer> pair;

    public TumorNormalCount(int tumorCount, int normalCount) {
        pair = new ImmutablePair<>(tumorCount, normalCount);
    }

    public int getTumorCount() {
        return pair.getLeft();
    }

    public int getNormalCount() {
        return pair.getRight();
    }
}
