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
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "b", "c", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testSampleNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "b", "c", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testFileNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "c", BassFileType.PICARD);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testVersionNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "d", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testProjectNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P456", "a", "c", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullSampleNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", null, "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "d", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullProjectNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple(null, "a", "c", BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "c", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullFileNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", "c", null);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "d", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullVersionNotEquals() {
        SubmissionTuple tuple1 = new SubmissionTuple("P123", "a", null, BassFileType.BAM);
        SubmissionTuple tuple2 = new SubmissionTuple("P123", "a", "d", BassFileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }
}
