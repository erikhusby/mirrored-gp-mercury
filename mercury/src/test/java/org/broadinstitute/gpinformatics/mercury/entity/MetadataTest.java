package org.broadinstitute.gpinformatics.mercury.entity;


import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashSet;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

/**
 * Basic sanity check for Metadata.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class MetadataTest {

    private Metadata metadata;

    private static final Metadata.Key KEY = Metadata.Key.SAMPLE_ID;

    private static final String VALUE = "value";

    @BeforeMethod
    public void before() {
        // equals and hashCode.
        metadata = new Metadata(KEY, VALUE);
    }

    public void basics() {
        assertThat(metadata.getKey(), is(equalTo(KEY)));
        assertThat(metadata.getValue(), is(equalTo(VALUE)));
    }

    public void hashCodeAndEquals() {
        HashSet<Metadata> metadataSet = new HashSet<Metadata>() {{
            add(metadata);
            add(new Metadata(KEY, VALUE));
        }};

        assertThat("Even though 2 entries were added, they had the same essential values.  Hash set should only be 1",
                metadataSet.size(), is(1));
        Metadata secondMetadata = new Metadata(Metadata.Key.PATIENT_ID, "value2");
        metadataSet.add(secondMetadata);
        assertThat("Hash set size should have increased to 2.  That failed", metadataSet.size(), is(2));
        assertThat("Existence check of known records failed", metadataSet, hasItems(metadata, secondMetadata));
        assertThat("Different key same value match failed", metadataSet, not(hasItem(new Metadata(Metadata.Key.GENDER, VALUE))));
        assertThat("Same key different value match failed", metadataSet, not(hasItem(new Metadata(Metadata.Key.SAMPLE_ID, "M"))));
    }
}