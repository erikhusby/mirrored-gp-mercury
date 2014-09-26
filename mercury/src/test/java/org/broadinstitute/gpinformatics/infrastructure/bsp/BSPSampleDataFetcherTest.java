package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSampleDataFetcherTest {

    BSPSampleSearchService sampleSearchService = BSPSampleSearchServiceProducer.testInstance();

    public void testBSPSampleDataFetcher() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        SampleData bspSampleData = fetcher.fetchSingleSampleFromBSP("SM-1T7HE");

        assertEquals(bspSampleData.getCollaboratorName(), "Herman Taylor");
        assertEquals(bspSampleData.getOrganism(), "Homo : Homo sapiens");
        assertEquals(bspSampleData.getPrimaryDisease(), "Control");
        assertEquals(bspSampleData.getMaterialType(), "DNA:DNA Genomic");
        assertTrue(bspSampleData.isSampleReceived());

        bspSampleData = fetcher.fetchSingleSampleFromBSP(bspSampleData.getRootSample());
        assertTrue(bspSampleData.isSampleReceived());
    }

    public void testGetStockIdForAliquotId() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);

        Assert.assertEquals(fetcher.getStockIdForAliquotId("SM-1T7HE"), "SM-1KXW2");
    }

    public void testGetStockIdForAliquotIdNoPrefix() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        Assert.assertEquals(fetcher.getStockIdForAliquotId("1T7HE"), "SM-1KXW2");
    }

    public void testPooledSampleWithMultipleRoots() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        SampleData bspSampleData = fetcher.fetchSingleSampleFromBSP("SM-41YNK");

        assertTrue(bspSampleData.isSampleReceived());

        //Now this checks all of the roots
        String[] sampleIds = bspSampleData.getRootSample().split(" ");
        Map<String, BspSampleData> roots = fetcher.fetchSampleData(Arrays.asList(sampleIds));
        for (SampleData sampleDTO : roots.values()) {
            assertTrue(sampleDTO.isSampleReceived());
        }
    }

    public void testFetchSingleColumn()  {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        String sampleName = "SM-1T7HE";
        Map<String, ? extends SampleData> bspSampleData = fetcher.fetchSampleData(Arrays.asList(sampleName),
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
        Assert.assertEquals(bspSampleData.keySet().size(), 1);
        BspSampleData bspSample = (BspSampleData) bspSampleData.get(sampleName);
        Assert.assertEquals(bspSample.getColumnToValue().size(), 2);
        Assert.assertNotNull(bspSample.getCollaboratorsSampleName());
        Assert.assertNotNull(bspSample.getSampleId());
    }
}
