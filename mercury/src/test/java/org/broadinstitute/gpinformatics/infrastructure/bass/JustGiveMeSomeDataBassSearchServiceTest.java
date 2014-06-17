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

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class JustGiveMeSomeDataBassSearchServiceTest {

    @Test(enabled = true)
    public void testRunSearch() throws Exception {
        JustGiveMeSomeDataBassSearchService searchService = new JustGiveMeSomeDataBassSearchService();
        List<BassDTO> results = searchService.runSearch(null);
        List<BassDTO.BassResultColumn> testColumns =
                Arrays.asList(BassDTO.BassResultColumn.id, BassDTO.BassResultColumn.rpid,
                        BassDTO.BassResultColumn.project, BassDTO.BassResultColumn.path);
        for (BassDTO bassDTO : results) {
            for (BassDTO.BassResultColumn column : testColumns) {
                Assert.assertFalse(bassDTO.getValue(column).isEmpty(), column.name() + " should not be null.");
            }
        }
    }
}
