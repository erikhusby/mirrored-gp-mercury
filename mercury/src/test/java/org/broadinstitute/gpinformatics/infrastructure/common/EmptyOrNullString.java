package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class EmptyOrNullString extends TypeSafeMatcher<String> {

    @Override
    public boolean matchesSafely(String s) {
        return StringUtils.isEmpty(s);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("empty or null String");
    }

    @Factory
    public static <T> Matcher<String> emptyOrNullString() {
        return new EmptyOrNullString();
    }
}
