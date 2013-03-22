package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

class IsInBspFormat extends TypeSafeMatcher<String> {

    @Override
    public boolean matchesSafely(String s) {
        return ProductOrderSample.isInBspFormat(s);
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("is in BSP format");
    }

    @Factory
    public static <T> Matcher<String> inBspFormat() {
        return new IsInBspFormat();
    }
}
