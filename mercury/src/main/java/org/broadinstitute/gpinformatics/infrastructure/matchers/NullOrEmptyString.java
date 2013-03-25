package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class NullOrEmptyString extends TypeSafeMatcher<String> {

    @Override
    public boolean matchesSafely(String s) {
        return StringUtils.isEmpty(s);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("null or empty String");
    }

    @Factory
    public static <T> Matcher<String> nullOrEmptyString() {
        return new NullOrEmptyString();
    }
}
