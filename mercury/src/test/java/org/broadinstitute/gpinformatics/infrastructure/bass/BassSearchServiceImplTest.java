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

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BassSearchServiceImplTest {
    public BassSearchService bassSearchService;

    @BeforeMethod
    public void setUp() {
        bassSearchService = new BassSearchServiceImpl(new BassConfig(Deployment.DEV));
    }

    public void testSearchBass() {
        List<BassDTO> bassDTOs = bassSearchService.searchByResearchProject("RP-12");
        Assert.assertFalse(bassDTOs.isEmpty());
        for (BassDTO bassDTO : bassDTOs) {
            // RP Aggregated research projects should always have a data_type column.
            if (bassDTO.isAggregatedByResearchProject()) {
                Assert.assertTrue(StringUtils.isNotBlank(bassDTO.getDatatype()), getDTOInfo(bassDTO));
            }
        }
    }

    private String getDTOInfo(BassDTO bassDTO) {
        return String.format("Bass ID: %s Bass DataType: %s", bassDTO.getId(), bassDTO.getDatatype());
    }
}
