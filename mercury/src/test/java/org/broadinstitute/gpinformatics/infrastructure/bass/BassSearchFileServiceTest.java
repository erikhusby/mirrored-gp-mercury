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

package org.broadinstitute.gpinformatics.infrastructure.bass;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BassSearchFileServiceTest {
    private Map<BassDTO.BassResultColumn, List<String>> parameters;
    private BassSearchFileService service;

    @BeforeMethod
    public void setUp() throws Exception {
        service = new BassSearchFileService();
        parameters = new HashMap<>();
    }

    @Test(enabled = true)
    public void testReadFile() throws Exception {
        BassSearchFileService service = new BassSearchFileService();
        String fileData = service.readFile();
        Assert.assertTrue(fileData.startsWith("##FILE_TYPE=bam##"));
    }

    @Test(enabled = true)
    public void testRunSearch() throws Exception {
        List<BassDTO> results = service.runSearch(BassSearchServiceTest.RP_12);
        for (BassDTO result : results) {
            Assert.assertEquals(result.getValue(BassDTO.BassResultColumn.rpid), BassSearchServiceTest.RP_12);
        }
    }

    @Test(enabled = true)
    public void testRunSearchNoResults() throws Exception {
        String testRpId = "NO-SUCH-RP";
        List<BassDTO> results = service.runSearch(testRpId);
        Assert.assertTrue(results.isEmpty());
    }

    /**
     * Test that there is more than one sample in the result set. This test goes hand-in-hand with testFilterResults.
     */
    @Test(enabled = true)
    public void testResearchProjectHasMoreThanOneSampleInIt() throws Exception {
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(BassSearchServiceTest.RP_12));
        List<BassDTO> results = service.runSearch(parameters);
        boolean hasTestSample = false;
        boolean hasOtherSample = false;
        for (BassDTO result : results) {
            if (result.getSample().equals(BassSearchServiceTest.COLLABORATOR_SAMPLE_ID)) {
                hasTestSample = true;
            } else {
                hasOtherSample = true;
            }
        }
        Assert.assertTrue(hasTestSample);
        Assert.assertTrue(hasOtherSample);
    }

    @Test(enabled = true)
    public void testMultipleCriteria() throws Exception {
        parameters =
                service.buildParameterMap(BassSearchServiceTest.RP_12, BassSearchServiceTest.COLLABORATOR_SAMPLE_ID);
        List<BassDTO> results = service.runSearch(parameters);
        for (BassDTO result : results) {
            Assert.assertEquals(result.getSample(), BassSearchServiceTest.COLLABORATOR_SAMPLE_ID);
        }
    }
}
