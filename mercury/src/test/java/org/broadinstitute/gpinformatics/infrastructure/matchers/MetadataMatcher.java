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

import org.broadinstitute.gpinformatics.infrastructure.common.AbstractSample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class MetadataMatcher {

    public static <T extends AbstractSample> Matcher isMetadataSource(final MercurySample.MetadataSource metadataSource) {
        return new TypeSafeMatcher<T>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("metadataSource should be ").appendValue(metadataSource);
            }

            @Override
            protected void describeMismatchSafely(T sample, Description mismatchDescription) {
                mismatchDescription.appendText(" was ").appendValue(sample.getMetadataSource());
            }

            @Override
            protected boolean matchesSafely(T sample) {
                return metadataSource == sample.getMetadataSource();
            }
        };

    }
}



