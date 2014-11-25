/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.matchers;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;

public class MetadataMatcher {

    public static Matcher<ProductOrderSample> isMetadataSource(final MercurySample.MetadataSource metadataSource) {
        return new TypeSafeMatcher<ProductOrderSample>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("metadataSource should be ").appendValue(metadataSource);
            }

            @Override
            protected void describeMismatchSafely(ProductOrderSample sample, Description mismatchDescription) {
                mismatchDescription.appendText(" was ").appendValue(sample.getMetadataSource());
            }

            @Override
            protected boolean matchesSafely(ProductOrderSample sample) {
                return metadataSource == sample.getMetadataSource();
            }
        };

    }

    @Test
    public void testMercuryMetadataSourceWithMercurySamples() {
        ProductOrderSample fooSample = getProductOrderSample("foo", MercurySample.MetadataSource.MERCURY);
        ProductOrderSample barSample = getProductOrderSample("bar", MercurySample.MetadataSource.MERCURY);
        List<ProductOrderSample> samples = Arrays.asList(fooSample, barSample);

        assertThat(samples,
                 everyItem(isMetadataSource(MercurySample.MetadataSource.MERCURY)));
    }

    private ProductOrderSample getProductOrderSample(String sampleKey, MercurySample.MetadataSource metadataSource) {
//        MercurySampleData sampleData = new MercurySampleData(sampleKey, Collections.<Metadata>emptySet());
        MercurySample mercurySample = new MercurySample(sampleKey, metadataSource);
        ProductOrderSample sample = new ProductOrderSample(sampleKey);
        sample.setMercurySample(mercurySample);
        return sample;
    }
//    @Test
//    public void testMixedMetadataSourceWithMercurySamples() {
//        MercurySample fooSample = new MercurySample("foo", MercurySample.MetadataSource.BSP);
//        MercurySample barSample = new MercurySample("bar", MercurySample.MetadataSource.MERCURY);
//        List<MercurySample> samples = Arrays.asList(fooSample, barSample);
//
//        assertThat(samples,
//                Matchers.<List<MercurySample>>not(
//                        (List<MercurySample>) everyItem(isMetadataSource(MercurySample.MetadataSource.MERCURY))));
//        assertThat(samples,
//                (Matcher<? super List<MercurySample>>) not(everyItem(isMetadataSource(MercurySample.MetadataSource.BSP))));
//    }
}



