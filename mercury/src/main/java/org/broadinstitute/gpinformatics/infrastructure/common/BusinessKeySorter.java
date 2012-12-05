package org.broadinstitute.gpinformatics.infrastructure.common;

/**
 * We need this class and the level of indirection it provides so we can expose a static compare method for
 * PrimeFaces DataTable.
 */
public class BusinessKeySorter {

    public static final BusinessKeyComparator comparator = new BusinessKeyComparator();

    public static final int sort(Object o1, Object o2) {
        String key1 = (String) o1;
        String key2 = (String) o2;

        return comparator.compare(key1, key2);
    }
}
