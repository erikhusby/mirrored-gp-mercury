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

import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTupleTest {
    public void testNullSample() {
        SubmissionTuple tuple1 = null;
        SubmissionTuple tuple2 = buildTuple("P123", "b", "c", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testSampleNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "b", "c", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "y", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "y", FileType.BAM);
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testFileNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "x", FileType.PICARD);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testVersionNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "y", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "y", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testProcessingLocationNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "x", FileType.PICARD);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }
    
    public void testProjectNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "x", "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P456", "a", "c", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullSampleNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", null, "c", "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullProjectNotEquals() {
        SubmissionTuple tuple1 = buildTuple(null, "a", "c", "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullFileNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", null, FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullVersionNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", null, "x", FileType.BAM);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "x", FileType.BAM);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testTupleFromString() {
        SubmissionTuple submissionTuple = buildTuple("p", "s", "v", "y", FileType.BAM);
        SubmissionTuple submissionTupleFromString = new SubmissionTuple("{\"project\":\"p\",\"sampleName\":\"s\",\"fileType\":\"BAM\",\"version\":\"v\",\"processingLocation\":\"y\"}");

        assertThat(submissionTuple, equalTo(submissionTupleFromString));
    }

    public void testToJsonString() {
        SubmissionTuple submissionTuple = buildTuple("p", "s", "v", "y", FileType.BAM);
        assertThat(submissionTuple.toString(), equalTo("{\"fileType\":\"BAM\",\"processingLocation\":\"y\",\"project\":\"p\",\"sampleName\":\"s\",\"version\":\"v\"}"));
    }

    private SubmissionTuple buildTuple(String project, String sampleName, String version, String processingLocation,
                                       FileType fileType) {
        SubmissionTuple submissionTuple = new SubmissionTuple(project, sampleName, version, processingLocation);
        submissionTuple.setFileType(fileType);
        return submissionTuple;
    }

}
