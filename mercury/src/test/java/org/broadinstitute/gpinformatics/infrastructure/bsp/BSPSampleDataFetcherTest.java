package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertEquals;

@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSampleDataFetcherTest extends ContainerTest {

    BSPSampleSearchService sampleSearchService = BSPSampleSearchServiceProducer.qaInstance();

    /**
     * BSP sometimes sends shorter result arrays when there are null fields
     */
    public void testNPEOnSampleWithMultipleMatchesAndSomeNullData() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        BSPSampleDTO bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-2NQU6");

        Assert.assertNull(bspSampleDTO.getCollaboratorsSampleName());
        Assert.assertEquals(bspSampleDTO.getSampleLsid(), "Unassigned");

        bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-1T7HE");

        assertEquals(bspSampleDTO.getCollaboratorName(), "Herman Taylor");
        assertEquals(bspSampleDTO.getOrganism(), "Homo : Homo sapiens");
        assertEquals(bspSampleDTO.getPrimaryDisease(), "Control");
        assertEquals(bspSampleDTO.getMaterialType(), "DNA:DNA Genomic");
    }

    public void testGetStockIdForAliquotId() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);

        Assert.assertEquals(fetcher.getStockIdForAliquotId("SM-1T7HE"), "SM-1KXW2");
    }
}
