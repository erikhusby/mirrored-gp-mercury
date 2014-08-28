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

package org.broadinstitute.gpinformatics.infrastructure.submission;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDtoTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.hamcrest.Matchers;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionDtoTest {
    public static final String TEST_SAMPLE = "BOT2365_T";
    public static final String RESEARCH_PROJECT = "RP-12";
    public static final String AGGREGATION_PROJECT = "RP-12";
    public static final int VERSION = 1;
    public static final double CONTAMINATION = 0.47;
    private static final String CONTAMINATION_STRING = "47%";
    public static final LevelOfDetection FINGERPRINT_LOD = new LevelOfDetection(-4.3d, -3.2d);
    public static final String BAM_FILE = BassDTO.FileType.BAM.getValue();
    private static final String DATA_TYPE = "Exome";
    private static final Double QUALITY_METRIC = 1.2;
    public static final String SUBMISSION_UUID = "1234";

    private Aggregation aggregation;
    private BassDTO bassDto;
    private List<ProductOrder> productOrders = new ArrayList<>();
    private SubmissionStatusDetailBean submissionStatus;

    @BeforeMethod
    public void setUp() throws Exception {
        bassDto = BassDtoTestFactory.buildBassResults(RESEARCH_PROJECT, TEST_SAMPLE);
        aggregation = AggregationTestFactory.buildAggregation(RESEARCH_PROJECT, TEST_SAMPLE, CONTAMINATION,
                FINGERPRINT_LOD, DATA_TYPE, QUALITY_METRIC, null, null);
        productOrders.add(ProductOrderTestFactory.createDummyProductOrder("PDO-1234"));
        productOrders.add(ProductOrderTestFactory.createDummyProductOrder("PDO-5678"));
        SubmissionsService submissionsService = new SubmissionsServiceStub();
        submissionStatus = submissionsService.getSubmissionStatus(SUBMISSION_UUID).iterator().next();
    }

    public void testDtoForSampleWithConstructor() {
        SubmissionDto submissionDTO = new SubmissionDto(bassDto, aggregation, productOrders, submissionStatus);
        assertThat(submissionDTO.getAggregation(), is(Matchers.notNullValue()));
        assertThat(submissionDTO.getBassDTO(), is(Matchers.notNullValue()));

        assertThat(submissionDTO.getSampleName(), equalTo(TEST_SAMPLE));
//        assertThat(submissionDTO.getBioSample());
        assertThat(submissionDTO.getDataType(), equalTo(BassDTO.DATA_TYPE_EXOME));
        assertThat(submissionDTO.getProductOrders(), containsInAnyOrder(productOrders.toArray()));
        assertThat(submissionDTO.getAggregationProject(), equalTo(AGGREGATION_PROJECT));
        assertThat(submissionDTO.getFileType(), equalTo(BAM_FILE));
        assertThat(submissionDTO.getVersion(), equalTo(VERSION));
        assertThat(submissionDTO.getQualityMetric(), equalTo(QUALITY_METRIC));
        assertThat(submissionDTO.getContamination(), equalTo(CONTAMINATION));
        assertThat(submissionDTO.getContaminationString(), equalTo(CONTAMINATION_STRING));
        assertThat(submissionDTO.getFingerprintLOD(), equalTo(FINGERPRINT_LOD));
        assertThat(submissionDTO.getLanesInAggregation(), Matchers.equalTo(2));
//        assertThat(submissionDTO.getBlacklistedLanes(), blacklistedLanes);
//        assertThat(submissionDTO.getSubmittedVersion(), submittedVersion);
        assertThat(submissionDTO.getResearchProject(), Matchers.equalTo(RESEARCH_PROJECT));
        assertThat(submissionDTO.getSubmittedStatus(), Matchers.equalTo(String.format(
                SubmissionStatusDetailBean.Status.SUBMITTED.getKey(),
                SUBMISSION_UUID, SUBMISSION_UUID)));
        assertThat(submissionDTO.getStatusDate(), Matchers.equalTo(SubmissionsServiceStub.STUB_UPDATE_DATE));

    }


}
