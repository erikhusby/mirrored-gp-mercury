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

import org.broadinstitute.gpinformatics.infrastructure.bass.BassDTO;
import org.broadinstitute.gpinformatics.infrastructure.bass.BassDtoTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.AggregationTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.metrics.LevelOfDetection;
import org.broadinstitute.gpinformatics.infrastructure.metrics.entity.Aggregation;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class SubmissionSampleDtoTest {
    public static final String TEST_SAMPLE = "BOT2365_T";
    public static final String RESEARCH_PROJECT = "RP-12";
    public static final String AGGREGATION_PROJECT = "RP-12";
    public static final int VERSION = 1;
    public static final double CONTAMINATION = 0.47;
    public static final List<String> LANES = Arrays.asList("1", "2");
    public static final LevelOfDetection FINGERPRINT_LOD = new LevelOfDetection(-4.3d, -3.2d);
    public static final List<String> PRODUCT_ORDER_IDS = Arrays.asList("PDO-123", "PDO-456");
    public static final String BAM_FILE = BassDTO.FileType.BAM.getValue();

    private Aggregation aggregation;
    private BassDTO bassDto;


    @BeforeMethod
    public void setUp() throws Exception {
        bassDto = BassDtoTestFactory.buildBassResults(RESEARCH_PROJECT, TEST_SAMPLE);
        aggregation = AggregationTestFactory.buildAggregation(RESEARCH_PROJECT, TEST_SAMPLE, CONTAMINATION, FINGERPRINT_LOD);

    }

    public void testDtoForSampleWithConstructor() {
        SubmissionDTO submissionDTO = new SubmissionDTO(bassDto, aggregation, PRODUCT_ORDER_IDS);
        Assert.assertNotNull(submissionDTO.getAggregation());
        Assert.assertNotNull(submissionDTO.getBassDTO());

        Assert.assertEquals(submissionDTO.getSample(), TEST_SAMPLE);
//        Assert.assertEquals(submissionDTO.getBioSample());
        Assert.assertEquals(submissionDTO.getDataType(), Aggregation.DATA_TYPE_EXOME);
        Assert.assertEquals(submissionDTO.getProductOrderIds(), PRODUCT_ORDER_IDS);
        Assert.assertEquals(submissionDTO.getAggregationProject(), AGGREGATION_PROJECT);
        Assert.assertEquals(submissionDTO.getFileType(), BAM_FILE);
        Assert.assertEquals(submissionDTO.getVersion(), VERSION);
//        Assert.assertEquals(submissionDTO.getQualityMetric(), qualityMetric);
        Assert.assertEquals(submissionDTO.getContamination(), CONTAMINATION);
        Assert.assertEquals(submissionDTO.getFingerprintLOD(), FINGERPRINT_LOD);
        Assert.assertEquals(submissionDTO.getLanes(), LANES);
//        Assert.assertEquals(submissionDTO.getBlacklistedLanes(), blacklistedLanes);
//        Assert.assertEquals(submissionDTO.getSubmittedVersion(), submittedVersion);
//        Assert.assertEquals(submissionDTO.getCurrentStatus(), currentStatus);
        Assert.assertEquals(submissionDTO.getResearchProject(), RESEARCH_PROJECT);

    }

}
