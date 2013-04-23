package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


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
        assertTrue(bspSampleDTO.isSampleReceived());

        bspSampleDTO = fetcher.fetchSingleSampleFromBSP(bspSampleDTO.getRootSample());
        assertTrue(bspSampleDTO.isSampleReceived());
    }

    public void testGetStockIdForAliquotId() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);

        Assert.assertEquals(fetcher.getStockIdForAliquotId("SM-1T7HE"), "SM-1KXW2");
    }

    public void testPooledSampleWithMultipleRoots() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        BSPSampleDTO bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-41YNK");

        assertTrue(bspSampleDTO.isSampleReceived());

        //Now this checks all of the roots
        String[] sampleIds = bspSampleDTO.getRootSample().split(" ");
        Map<String, BSPSampleDTO> roots = fetcher.fetchSamplesFromBSP(Arrays.asList(sampleIds));
        for (BSPSampleDTO sampleDTO : roots.values()) {
            assertTrue(sampleDTO.isSampleReceived());
        }
    }
}
