package org.broadinstitute.gpinformatics.infrastructure.common;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Methods useful when testing.
 */
public class TestUtils {
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
