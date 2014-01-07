package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.apache.commons.collections4.CollectionUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.Collection;

public class NullOrEmptyCollection extends TypeSafeMatcher<Collection<?>> {
    @Override
    protected boolean matchesSafely(Collection<?> item) {
        return CollectionUtils.isEmpty(item);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("null or empty Collection");
    }

    @Factory
    public static <T> Matcher<Collection<?>> nullOrEmptyCollection() {
        return new NullOrEmptyCollection();
    }
}
