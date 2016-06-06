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

import org.broadinstitute.gpinformatics.infrastructure.bass.BassFileType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTupleTest {
    public void testNullSample() {
        SubmissionTuple tuple1 = null;
        SubmissionTuple tuple2 = new SubmissionTuple("b", BassFileType.BAM, "c");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testSampleNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("a", BassFileType.BAM, "c");
        SubmissionTuple tuple2 = new SubmissionTuple("b", BassFileType.BAM, "c");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("a", BassFileType.BAM, "c");
        SubmissionTuple tuple2 = new SubmissionTuple("a", BassFileType.BAM, "c");
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testFileNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("a", BassFileType.BAM, "c");
        SubmissionTuple tuple2 = new SubmissionTuple("a", BassFileType.PICARD, "c");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testVersionNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("a", BassFileType.BAM, "c");
        SubmissionTuple tuple2 = new SubmissionTuple("a", BassFileType.BAM, "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullSampleNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple(null, BassFileType.BAM, "c");
        SubmissionTuple tuple2 = new SubmissionTuple("a", BassFileType.BAM, "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullFileNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("a", null, "c");
        SubmissionTuple tuple2 = new SubmissionTuple("a", BassFileType.BAM, "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullVersionNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("a", BassFileType.BAM, null);
        SubmissionTuple tuple2 = new SubmissionTuple("a", BassFileType.BAM, "d");
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }
}
