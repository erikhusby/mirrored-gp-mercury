package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.*;


@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSampleDataFetcherTest extends ContainerTest {

    BSPSampleSearchService sampleSearchService = BSPSampleSearchServiceProducer.testInstance();

    /**
     * BSP sometimes sends shorter result arrays when there are null fields.
     */
    public void testNPEOnSampleWithMultipleMatchesAndSomeNullData() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        BSPSampleDTO bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-2NQU6");

        Assert.assertEquals(bspSampleDTO.getCollaboratorsSampleName(), "");
        Assert.assertEquals(bspSampleDTO.getSampleLsid(), "Unassigned");

        bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-1T7HE");

        assertEquals(bspSampleDTO.getCollaboratorName(), "Herman Taylor");
        assertEquals(bspSampleDTO.getOrganism(), "Homo : Homo sapiens");
        assertEquals(bspSampleDTO.getPrimaryDisease(), "Control");
        assertEquals(bspSampleDTO.getMaterialType(), "DNA:DNA Genomic");
        assertTrue(StringUtils.isBlank(bspSampleDTO.getReceiptDate()));
        assertTrue(bspSampleDTO.isSampleReceived());

        bspSampleDTO = fetcher.fetchSingleSampleFromBSP(bspSampleDTO.getRootSample());
        assertNotNull(bspSampleDTO.getReceiptDate());
        assertTrue(bspSampleDTO.isSampleReceived());
    }

    public void testGetStockIdForAliquotId() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);

        Assert.assertEquals(fetcher.getStockIdForAliquotId("SM-1T7HE"), "SM-1KXW2");
    }
}
