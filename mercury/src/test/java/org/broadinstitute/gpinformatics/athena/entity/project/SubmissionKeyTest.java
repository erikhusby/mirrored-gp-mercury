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

package org.broadinstitute.gpinformatics.athena.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionKeyTest {
    public void testNullSample() {
        SubmissionKey tuple1 = null;
        SubmissionKey tuple2 = new SubmissionKey("b", "b", "c");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testSampleNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("a", "b", "c");
        SubmissionKey tuple2 = new SubmissionKey("b", "b", "c");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testRepositoryNameDiffersNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("b", "b", "c", "NCBI_PROTECTED", "Whole Genome");
        SubmissionKey repositoryNameDiffers = new SubmissionKey("b", "b", "c", "NCBI_PRIVATE", "Whole Genome");
        assertThat(tuple1, not(equalTo(repositoryNameDiffers)));
        assertThat(repositoryNameDiffers, not(equalTo(tuple1)));
    }

    public void testLibraryNameDiffersNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("b", "b", "c", "NCBI_PROTECTED", "Whole Genome");
        SubmissionKey libraryNameDiffers = new SubmissionKey("b", "b", "c", "NCBI_PROTECTED", "Partially Hydrogenated Genome");
        assertThat(tuple1, not(equalTo(libraryNameDiffers)));
        assertThat(libraryNameDiffers, not(equalTo(tuple1)));
    }

    public void testEquals() {
        SubmissionKey tuple1 = new SubmissionKey("a", "b", "c");
        SubmissionKey tuple2 = new SubmissionKey("a", "b", "c");
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testNullEquals() {
        SubmissionKey tuple1 = null;
        SubmissionKey tuple2 = null;
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testFileNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("a", "b", "c");
        SubmissionKey tuple2 = new SubmissionKey("a", "c", "c");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testVersionNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("a", "b", "c");
        SubmissionKey tuple2 = new SubmissionKey("a", "b", "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullSampleNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey(null, "b", "c");
        SubmissionKey tuple2 = new SubmissionKey("a", "b", "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullFileNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("a", null, "c");
        SubmissionKey tuple2 = new SubmissionKey("a", "b", "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullVersionNotEquals() {
        SubmissionKey tuple1 = new SubmissionKey("a", "b", null);
        SubmissionKey tuple2 = new SubmissionKey("a", "b", "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }
}
