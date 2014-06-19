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
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BassSearchFileServiceTest {
    Map<BassDTO.BassResultColumn, List<String>> parameters = new HashMap<>();
    public static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";

    @Test(enabled = true)
    public void testReadFile() throws Exception {
        BassSearchFileService service = new BassSearchFileService();
        String fileData = service.readFile();
        Assert.assertTrue(fileData.startsWith("##FILE_TYPE=bam##"));
    }

    @Test(enabled = true)
    public void testRunSearch() throws Exception {
        BassSearchFileService service = new BassSearchFileService();
        String testRpId = "RP-12";
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(testRpId));
        List<BassDTO> results = service.runSearch(parameters);
        for (BassDTO result : results) {
            Assert.assertEquals(result.getValue(BassDTO.BassResultColumn.rpid), testRpId);
        }
    }

    @Test(enabled = true)
    public void testRunSearchNoResults() throws Exception {
        BassSearchFileService service = new BassSearchFileService();
        String testRpId = "NO-SUCH-RP";
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(testRpId));
        List<BassDTO> results = service.runSearch(parameters);
        Assert.assertTrue(results.isEmpty());
    }

    /**
     * Test that there is more than one sample in the result set. This test goes hand-in-hand with testFilterResults.
     */
    @Test(enabled = true)
    public void testResearchProjectHasMoreThanOneSampleInIt() throws Exception {
        BassSearchFileService service = new BassSearchFileService();
        String testRpId = "RP-12";
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(testRpId));
        List<BassDTO> results = service.runSearch(parameters);
        boolean hasTestSample = false;
        boolean hasOtherSample = false;
        for (BassDTO result : results) {
            if (result.getSample().equals(COLLABORATOR_SAMPLE_ID)) {
                hasTestSample = true;
            } else {
                hasOtherSample = true;
            }
        }
        Assert.assertTrue(hasTestSample);
        Assert.assertTrue(hasOtherSample);
    }

    @Test(enabled = true)
    public void testFilterResults() throws Exception {
        BassSearchFileService service = new BassSearchFileService();
        String researchProjectId = "RP-12";
        String testRpId = researchProjectId;
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(testRpId));
        List<BassDTO> results = service.runSearch(researchProjectId, COLLABORATOR_SAMPLE_ID);
        for (BassDTO result : results) {
            Assert.assertEquals(result.getValue(BassDTO.BassResultColumn.sample), COLLABORATOR_SAMPLE_ID);
        }
    }
}
