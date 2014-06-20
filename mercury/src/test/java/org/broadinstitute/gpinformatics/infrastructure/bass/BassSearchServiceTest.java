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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BassSearchServiceTest {
    protected static final String RP_12 = "RP-12";
    protected static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    public static final String TEST_BASS_ID = "BI7839509";
    public BassSearchService bassSearchService;

    @BeforeMethod
    public void setUp() {
        bassSearchService = new BassSearchService(new BassConfig(Deployment.DEV));
    }

    public void testSearchBass() {
        List<BassDTO> bassDTOs = bassSearchService.runSearch(RP_12);
        Assert.assertFalse(bassDTOs.isEmpty());
        for (BassDTO bassDTO : bassDTOs) {
            // RP Aggregated research projects should always have a data_type column.
            if (bassDTO.isAggregatedByResearchProject()) {
                Assert.assertTrue(StringUtils.isNotBlank(bassDTO.getDatatype()), getDTOInfo(bassDTO));
            }
        }
    }

    public void testSearchBassMultipleParams() {
        List<BassDTO> bassDTOs = bassSearchService.runSearch(RP_12);
        int numResults = bassDTOs.size();

        bassDTOs = bassSearchService.runSearch(RP_12, COLLABORATOR_SAMPLE_ID);
        int numResultsMultipleParams = bassDTOs.size();
        Assert.assertTrue(numResults > numResultsMultipleParams,
                "Search using more parameters should have returned fewer results");

    }

    public void testSearchBassMultipleParamsOneIsId() {
        Map<BassDTO.BassResultColumn, List<String>> parameters = new HashMap<>();
        parameters.put(BassDTO.BassResultColumn.id, Arrays.asList(TEST_BASS_ID));
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(RP_12));

        try {
            bassSearchService.runSearch(parameters);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(BassSearchService.ONLY_IDS_MAY_BE_SPECIFIED));
        }

    }

    private String getDTOInfo(BassDTO bassDTO) {
        return String.format("Bass ID: %s Bass DataType: %s", bassDTO.getId(), bassDTO.getDatatype());
    }
}
