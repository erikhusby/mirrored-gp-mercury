package org.broadinstitute.gpinformatics.infrastructure.common;

import java.util.*;

@SuppressWarnings("UnusedDeclaration")
public class BaseSplitter {

    /**
     * The maximum number of arguments for an Oracle IN clause.
     */
    protected static final int DEFAULT_SPLIT_SIZE = 1000;

    /**
     * Split the data into chunks of a certain size, then return a list of collections,
     * where each collection is a 'chunk' of data, using the default split size of
     * {@link #DEFAULT_SPLIT_SIZE}.
     *
     * @param collection the Collection to be split out
     * @param <T> type of data
     * @return list of collections of split data
     */
    public static <T> List<Collection<T>> split(Collection<T> collection) {
        return BaseSplitter.split(collection, DEFAULT_SPLIT_SIZE);
    }

    /**
     * Split the data into chunks of a certain size, then return a list of collections,
     * where each collection is a 'chunk' of data.<p/>
     * For example calling split(foo, 100) where
     * foo is a List of Longs of size 375 will result in a Collection being returned
     * containing four Collection<Long>, the first three with 100 elements each and the
     * last with 75 elements.
     *
     * @param collection the collection to be split into pieces.
     * @param size number of data in each split
     * @param <T> type of data
     * @return A list of collections of data that has been split.
     */
    public static <T> List<Collection<T>> split(Collection<T> collection, int size) {

        if (collection.isEmpty()) {
            return Collections.emptyList();
        }

        if (collection.size() <= size) {
            return Collections.singletonList(collection);
        }

        // The full split up results
        List<Collection<T>> result = new ArrayList<>(collection.size() / size);

        // If the collection is a list, use List.subList() to avoid unnecessary allocation and copying.

        if (collection instanceof List) {
            List<T> list = (List<T>) collection;
            for (int i = 0; i < list.size(); i += size) {
                result.add(list.subList(i, Math.min(list.size(), i + size)));
            }
        } else {
            // Each chunk is here
            Collection<T> temp = new ArrayList<>(size);

            // Go through each item in the list and add it to the temp list
            for (T d : collection) {

                // If we have reached the chunk size, then add it to the chunked result and clear the temp
                if (temp.size() == size) {
                    result.add(new ArrayList<>(temp));
                    temp.clear();
                }

                // add the item
                temp.add(d);
            }

            // Add the last chunk right in.
            result.add(temp);
        }
        return result;
    }

    /**
     * Split the data into chunks of a certain size, then return a list of arrays,
     * where each array is a 'chunk' of data, using the default split size of
     * {@link #DEFAULT_SPLIT_SIZE}.
     *
     * @param array the array to be split out
     * @param <T> type of data
     * @return list of arrays of split data
     */
    public static <T> List<T[]> split(T[] array) {
        return BaseSplitter.split(array, DEFAULT_SPLIT_SIZE);
    }

    /**
     * Split the data into chunks of a certain size, then return a list of arrays,
     * where each array is a 'chunk' of data.<p/>
     * For example calling split(foo, 100) where
     * foo is an array of long of size 375 will result in a Collection being returned
     * containing four long[], the first three with 100 elements each and the
     * last with 75 elements.
     *
     * @param array the array to be split out
     * @param size number of data in each split
     * @param <T> type of data
     * @return A list of arrays of data that has been split.
     */
    public static <T> List<T[]> split(T[] array, int size) {
        if (array.length == 0) {
            return Collections.emptyList();
        }

        if (array.length <= size) {
            return Collections.singletonList(array);
        }

        // The full split up results
        List<T[]> result = new ArrayList<>(array.length / size);

        for (int i = 0; i < array.length; i += size) {
            result.add(Arrays.copyOfRange(array, i, Math.min(array.length, i + size)));
        }
        return result;
    }

}
