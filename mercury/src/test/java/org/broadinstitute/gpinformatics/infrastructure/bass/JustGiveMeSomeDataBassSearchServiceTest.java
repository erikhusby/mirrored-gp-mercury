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

import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class JustGiveMeSomeDataBassSearchServiceTest {

    @Test(enabled = true)
    public void testRunSearch() throws Exception {
        JustGiveMeSomeDataBassSearchService searchService = new JustGiveMeSomeDataBassSearchService();
        List<Map<BassDTO.BassResultColumns, String>> maps = searchService.runSearch(null);
        for (Map<BassDTO.BassResultColumns, String> map : maps) {
            switch (map.get(BassDTO.BassResultColumns.file_type)) {
            case BassDTO.FILE_TYPE_BAM:
                for (BassDTO.BassResultColumns column : BassDTO.BAM_COLUMNS) {
                    Assert.assertFalse(map.get(column).isEmpty());
                }
                break;
            case BassDTO.FILE_TYPE_READ_GROUP_BAM:
                for (BassDTO.BassResultColumns column : BassDTO.READ_GROUP_BAM_COLUMNS) {
                    Assert.assertFalse(map.get(column).isEmpty());
                }
                break;
            case BassDTO.FILE_TYPE_PICARD:
                for (BassDTO.BassResultColumns column : BassDTO.PICARD_COLUMNS) {
                    Assert.assertFalse(map.get(column).isEmpty());
                }
                break;
            }

        }
    }
}
