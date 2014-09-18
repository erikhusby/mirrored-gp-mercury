package org.broadinstitute.gpinformatics.infrastructure.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Methods useful when testing.
 */
public class TestUtils {
    /**
     * The location of where test data is stored.
     */
    public static final String TEST_DATA_LOCATION = "src/test/resources/testdata";

    /**
     * This method returns the full path to, and including the specified file name.
     *
     * @param fileName the name file which you seek
     *
     * @return the full path to fileName.
     */
    public static String getTestData(String fileName) {
        return TEST_DATA_LOCATION + "/" + fileName;
    }

    /**
     * Convenience method to return the first item in a collection. This method will return null
     * if the collection is empty.
     *
     * @return The first item in the collection or null if it is empty.
     */
    @Nullable
    public static <T> T getFirst(@Nonnull final Collection<T> collection) {
        if (!collection.isEmpty()) {
            return collection.iterator().next();
        }
        return null;
    }
}
