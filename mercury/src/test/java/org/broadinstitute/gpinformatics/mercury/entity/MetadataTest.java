package org.broadinstitute.gpinformatics.mercury.entity;


import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.HashSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Basic sanity check for Metadata.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class MetadataTest {

    public void basics() {
        // Cover the no-arg constructor.

        // equals and hashCode.
        final Metadata.Key KEY = Metadata.Key.SAMPLE_ID;
        final String VALUE = "value";
        final Metadata metadata = new Metadata(KEY, VALUE);
        assertThat(metadata.getKey(), is(equalTo(KEY)));
        assertThat(metadata.getValue(), is(equalTo(VALUE)));

        new HashSet<Metadata>() {{
            add(metadata);
            add(new Metadata(KEY, VALUE));
        }};
    }

    public void testBasicConstructorMetadata() {
        new Metadata();

    }
}