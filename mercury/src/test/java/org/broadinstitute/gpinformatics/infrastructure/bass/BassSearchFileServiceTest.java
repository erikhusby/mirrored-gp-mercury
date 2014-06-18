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
    Map<BassDTO.BassResultColumn, List<String>> parameters=new HashMap<>();

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
}
