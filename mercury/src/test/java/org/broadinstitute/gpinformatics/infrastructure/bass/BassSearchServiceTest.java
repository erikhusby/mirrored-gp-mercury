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

import javax.ws.rs.core.MultivaluedMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BassSearchServiceTest {
    protected static final String RP_12 = "RP-12";
    protected static final String COLLABORATOR_SAMPLE_ID = "BOT2365_T";
    public static final String TEST_BASS_ID = "BI7839509";
    public BassSearchServiceImpl bassSearchService;

    @BeforeMethod
    public void setUp() {
        bassSearchService = new BassSearchServiceImpl(new BassConfig(Deployment.DEV));
    }

    public void testSearchBass() {
        List<BassDTO> bassDTOs = bassSearchService.runSearch(RP_12, BassFileType.BAM);
        Assert.assertFalse(bassDTOs.isEmpty());
        for (BassDTO bassDTO : bassDTOs) {
            Assert.assertEquals(BassFileType.BAM, BassFileType.byBassValue(bassDTO.getFileType()));
            // RP Aggregated research projects should always have a data_type column.
            if (bassDTO.isAggregatedByResearchProject()) {
                Assert.assertEquals(bassDTO.getRpid(), bassDTO.getProject());
                Assert.assertTrue(StringUtils.isNotBlank(bassDTO.getDatatype()), getDTOInfo(bassDTO));
            } else {
                Assert.assertNotEquals(bassDTO.getRpid(), bassDTO.getProject());
            }
        }
    }

    public void testSearchBassAllFileTypes() {
        List<BassDTO> bassDTOs = bassSearchService.runSearch(RP_12, BassFileType.ALL);
        Assert.assertFalse(bassDTOs.isEmpty());
        Set<BassFileType> resultFileTypes = new HashSet<>();
        for (BassDTO bassDTO : bassDTOs) {
            resultFileTypes.add(BassFileType.byBassValue(bassDTO.getFileType()));
        }
        Assert.assertEquals(resultFileTypes.size(), 2, "Result set should contain BAM and PICARD files.");
        Assert.assertTrue(resultFileTypes.contains(BassFileType.BAM));
        Assert.assertTrue(resultFileTypes.contains(BassFileType.PICARD));
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
            bassSearchService.runSearch(parameters, BassFileType.BAM);
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains(BassSearchServiceImpl.ONLY_IDS_MAY_BE_SPECIFIED));
        }
    }

    public void testGetBamParam() {
        MultivaluedMap<String, String> fileTypeParam = bassSearchService.getFileTypeParam(BassFileType.BAM);
        validate(fileTypeParam, BassDTO.FILETYPE, BassFileType.BAM);
    }

    public void testGetRgBamParam() {
        MultivaluedMap<String, String> fileTypeParam =
                bassSearchService.getFileTypeParam(BassFileType.READ_GROUP_BAM);
        validate(fileTypeParam, BassDTO.FILETYPE, BassFileType.READ_GROUP_BAM);
    }

    public void testGetPicardParam() {
        MultivaluedMap<String, String> fileTypeParam = bassSearchService.getFileTypeParam(BassFileType.PICARD);
        validate(fileTypeParam, BassDTO.FILETYPE, BassFileType.PICARD);
    }

    public void testGetAllParam() {
        MultivaluedMap<String, String> fileTypeParam = bassSearchService.getFileTypeParam(BassFileType.ALL);
        Assert.assertTrue(fileTypeParam.isEmpty());
    }

    private void validate(MultivaluedMap<String, String> fileTypeParam, String expected, BassFileType fileType) {
        Assert.assertTrue(fileTypeParam.keySet().contains(expected), "Key should be " + BassDTO.FILETYPE);
        Assert.assertTrue(fileTypeParam.get(expected).contains(fileType.getBassValue()),
                "Value should contain " + fileType.getBassValue());

    }

    public void testFileTypeByValue() {
        boolean iDidLoop = false;
        for (BassFileType fileType : BassFileType.values()) {
            BassFileType type = BassFileType.byBassValue(fileType.getBassValue());
            Assert.assertNotNull(type);
            iDidLoop = true;
        }
        Assert.assertTrue(iDidLoop);
    }


    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testFileTypeEnumByValueNoEnumConstant() {
        BassFileType.byBassValue("no-such-enum");
        Assert.fail("Should have thrown an IllegalArgumentException!");
    }

    private String getDTOInfo(BassDTO bassDTO) {
        return String.format("Bass ID: %s Bass DataType: %s", bassDTO.getId(), bassDTO.getDatatype());
    }

}
