package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 2:59 PM
 */
public class MaleFemaleCount {
    private ImmutablePair<Integer,Integer> pair;

    public MaleFemaleCount(int maleCount, int femaleCount) {
        pair = new ImmutablePair<Integer, Integer>(maleCount, femaleCount);
    }

    public int getMaleCount() {
        return pair.getLeft();
    }

    public int getFemaleCount() {
        return pair.getRight();
    }
}
