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

import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationAlignment;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationHybridSelection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.AggregationReadGroup;
import org.broadinstitute.gpinformatics.infrastructure.submission.FileType;
import org.broadinstitute.gpinformatics.infrastructure.submission.ISubmissionTuple;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionDto;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionStatusDetailBean;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionTupleTest {
    private static final String EXOME = "Exome";
    public void testNullSample() {
        SubmissionTuple tuple1 = null;
        SubmissionTuple tuple2 = buildTuple("P123", "b", "c", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testSampleNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "b", "c", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "y", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "y", FileType.BAM, EXOME);
        assertThat(tuple1, equalTo(tuple2));
        assertThat(tuple2, equalTo(tuple1));
    }

    public void testFileNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "x", FileType.PICARD, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testVersionNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "y", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "y", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testProcessingLocationNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "x", FileType.PICARD, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }
    
    public void testProjectNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "x", "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P456", "a", "c", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullSampleNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", null, "c", "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullProjectNotEquals() {
        SubmissionTuple tuple1 = buildTuple(null, "a", "c", "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "c", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullFileNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", "c", null, FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testNullVersionNotEquals() {
        SubmissionTuple tuple1 = buildTuple("P123", "a", null, "x", FileType.BAM, EXOME);
        SubmissionTuple tuple2 = buildTuple("P123", "a", "d", "x", FileType.BAM, EXOME);
        assertThat(tuple1, not(equalTo(tuple2)));
        assertThat(tuple2, not(equalTo(tuple1)));
    }

    public void testTupleFromString() {
        SubmissionTuple submissionTuple = buildTuple("p", "s", "v", "y", FileType.BAM, EXOME);
        SubmissionTuple submissionTupleFromString = new SubmissionTuple("{\"dataType\":\"Exome\",\"project\":\"p\",\"sampleName\":\"s\",\"fileType\":\"BAM\",\"version\":\"v\",\"processingLocation\":\"y\"}");

        assertThat(submissionTuple, equalTo(submissionTupleFromString));
    }

    public void testToJsonString() {
        SubmissionTuple submissionTuple = buildTuple("p", "s", "v", "y", FileType.BAM, EXOME);
        assertThat(submissionTuple.toString(), equalTo("{\"dataType\":\"Exome\",\"fileType\":\"BAM\",\"processingLocation\":\"y\",\"project\":\"p\",\"sampleName\":\"s\",\"version\":\"v\"}"));
    }

    private SubmissionTuple buildTuple(String project, String sampleName, String version, String processingLocation,
                                       FileType fileType, String dataType) {
        SubmissionTuple submissionTuple = new SubmissionTuple(project, sampleName, version, processingLocation, dataType);
        submissionTuple.setFileType(fileType);
        return submissionTuple;
    }

    public void testTupleEqual(){
        SubmissionTracker tracker1 =
            new SubmissionTracker(null, "p1", "s1", "1", FileType.BAM, SubmissionBioSampleBean.ON_PREM,
                Aggregation.DATA_TYPE_RNA);
        Set<AggregationAlignment> alignments = Collections.singleton(new AggregationAlignment(1l, "foo"));

        SubmissionDto submissionDto = new SubmissionDto(
            new Aggregation("p1", "s1", null, 1, 2, Aggregation.DATA_TYPE_RNA, alignments, null, null,
                Collections.<AggregationReadGroup>emptySet(), null, null, SubmissionBioSampleBean.ON_PREM), new SubmissionStatusDetailBean());
        assertThat(SubmissionTuple.hasTuple(Arrays.<ISubmissionTuple>asList(tracker1, submissionDto), tracker1), is(true));
    }

    public void testTupleNotEqual(){
        SubmissionTracker tracker1 = new SubmissionTracker(null, "p2", "s1", "1", FileType.BAM, SubmissionBioSampleBean.ON_PREM, EXOME);
        SubmissionTracker tracker2 = new SubmissionTracker(null, "p3", "s1", "1", FileType.BAM, SubmissionBioSampleBean.ON_PREM, EXOME);
        Set<AggregationAlignment> alignments = Collections.singleton(new AggregationAlignment(1l, "foo"));

        SubmissionDto submissionDto = new SubmissionDto(
            new Aggregation("p1", "s1", null, 1, 2, EXOME, alignments, null, new AggregationHybridSelection(1d),
                Collections.<AggregationReadGroup>emptySet(), null, null, SubmissionBioSampleBean.ON_PREM), new SubmissionStatusDetailBean());

        List<? extends ISubmissionTuple> tuples = Arrays.asList(tracker1, submissionDto);
        assertThat(SubmissionTuple.hasTuple(tuples, tracker2), not(true));
    }
}
