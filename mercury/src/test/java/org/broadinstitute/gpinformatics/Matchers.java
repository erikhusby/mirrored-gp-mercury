package org.broadinstitute.gpinformatics;

import org.hamcrest.Matcher;

import java.util.Collection;

/**
 */
public class Matchers {

    /**
     * Specific version of {@link org.mockito.Matchers#argThat(Matcher)} for collections that centralizes suppression of
     * unchecked cast warnings.
     *
     * @see org.mockito.Matchers#argThat(Matcher)
     */
    @SuppressWarnings("unchecked")
    public static <T> Collection<T> argThat(Matcher<Iterable<? extends T>> matcher) {
        return (Collection<T>) org.mockito.Matchers.argThat(matcher);
    }
}
