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
        SampleData bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-1T7HE");

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

    public void testGetStockIdForAliquotIdNoPrefix() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        Assert.assertEquals(fetcher.getStockIdForAliquotId("1T7HE"), "SM-1KXW2");
    }

    public void testPooledSampleWithMultipleRoots() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        SampleData bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-41YNK");

        assertTrue(bspSampleDTO.isSampleReceived());

        //Now this checks all of the roots
        String[] sampleIds = bspSampleDTO.getRootSample().split(" ");
        Map<String, BSPSampleDTO> roots = fetcher.fetchSampleData(Arrays.asList(sampleIds));
        for (SampleData sampleDTO : roots.values()) {
            assertTrue(sampleDTO.isSampleReceived());
        }
    }

    public void testFetchSingleColumn()  {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(sampleSearchService);
        String sampleName = "SM-1T7HE";
        Map<String, ? extends SampleData> bspSampleDTO = fetcher.fetchSampleData(Arrays.asList(sampleName),
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
        Assert.assertEquals(bspSampleDTO.keySet().size(), 1);
        BSPSampleDTO bspSample = (BSPSampleDTO) bspSampleDTO.get(sampleName);
        Assert.assertEquals(bspSample.getColumnToValue().size(), 2);
        Assert.assertNotNull(bspSample.getCollaboratorsSampleName());
        Assert.assertNotNull(bspSample.getSampleId());
    }
}
