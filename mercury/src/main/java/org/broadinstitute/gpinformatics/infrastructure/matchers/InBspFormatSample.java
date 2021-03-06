package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class InBspFormatSample extends TypeSafeMatcher<ProductOrderSample> {

    @Override
    public boolean matchesSafely(ProductOrderSample sample) {
        return BSPUtil.isInBspFormat(sample.getName());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("in BSP format");
    }

    @Factory
    public static <T> Matcher<ProductOrderSample> inBspFormat() {
        return new InBspFormatSample();
    }
}
