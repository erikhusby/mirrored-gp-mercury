package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSampleDataFetcherTest {

    BSPSampleSearchService sampleSearchService = BSPSampleSearchServiceProducer.testInstance();

    public void testBSPSampleDataFetcher() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcherImpl(sampleSearchService);
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
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcherImpl(sampleSearchService);

        Assert.assertEquals(fetcher.getStockIdForAliquotId("SM-1T7HE"), "SM-1KXW2");
    }

    public void testGetStockIdForAliquotIdNoPrefix() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcherImpl(sampleSearchService);
        Assert.assertEquals(fetcher.getStockIdForAliquotId("1T7HE"), "SM-1KXW2");
    }

    public void testPooledSampleWithMultipleRoots() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcherImpl(sampleSearchService);
        SampleData bspSampleData = fetcher.fetchSingleSampleFromBSP("SM-41YNK");

        assertTrue(bspSampleData.isSampleReceived());

        //Now this checks all of the roots
        String[] sampleIds = bspSampleData.getRootSample().split(" ");
        Map<String, BspSampleData> roots = fetcher.fetchSampleData(Arrays.asList(sampleIds));
        for (SampleData sampleData : roots.values()) {
            assertTrue(sampleData.isSampleReceived());
        }
    }

    public void testFetchSingleColumn()  {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcherImpl(sampleSearchService);
        String sampleName = "SM-1T7HE";
        Map<String, ? extends SampleData> bspSampleData = fetcher.fetchSampleData(Arrays.asList(sampleName),
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
        Assert.assertEquals(bspSampleData.keySet().size(), 1);
        BspSampleData bspSample = (BspSampleData) bspSampleData.get(sampleName);
        Assert.assertEquals(bspSample.getColumnToValue().size(), 2);
        Assert.assertNotNull(bspSample.getCollaboratorsSampleName());
        Assert.assertNotNull(bspSample.getSampleId());
    }

    public void test_fetchSampleData_allows_bare_sample_IDs() {
        /*
         * Use of a PROD BSPConfig here is highly undesirable but currently unavoidable. There are checks in the code
         * that loosen certain validations when not in PROD to allow for test data that doesn't follow the same
         * conventions as production data. However, these checks can, and have, masked actual production issues and make
         * it nearly impossible to write some tests, such as this one. In this case, the offending check is in
         * BSPSampleDataFetcher.fetchSampleData(Collection<String>, BSPSampleSearchColumn...). In order to remove the
         * PROD check, however, some tests need to be updated to produce test sample IDs that will pass the
         * BSPUtil.isInBspFormat() check. As of 10/21/2014, and based on Bamboo build MER-CURY-2557 from 9/30/2014
         * (http://prodinfobuild.broadinstitute.org:8085/browse/MER-CURY-2557), the known failures due to this are in
         * BettaLimsMessageResourceTest, ReworkEjbTest, and SolexaRunResourceNonRestTest. Those test classes will
         * demonstrate failures if the PROD check is removed and must be fixed to use more realistic sample IDs.
         *
         * In the mean time, the only way that BSPSampleDataFetcher uses BSPConfig is to perform this check and as an
         * argument to AbstractConfig.isSupported() which I don't understand the purpose and only checks that its
         * argument is not null. Therefore, I believe it is safe for now to have this very targeted test that
         * references PROD. Once the tests are fixed to use realistic sample IDs, the PROD check in BSPSampleDataFetcher
         * can be removed and this test can use a non-PROD BSPConfig.
         *
         * -breilly, 10-21-2014
         */
        BSPConfig bspConfig = new BSPConfig(Deployment.PROD);
        BSPSampleDataFetcher fetcher =
                new BSPSampleDataFetcherImpl(sampleSearchService, bspConfig);
        String bareSampleId = "1T7HE";
        String sampleId = "SM-" + bareSampleId;
        Map<String, BspSampleData> sampleDataBySampleId =
                fetcher.fetchSampleData(Collections.singletonList(bareSampleId));
        Assert.assertEquals(sampleDataBySampleId.size(), 1);
        BspSampleData sampleData = sampleDataBySampleId.get(sampleId);
        Assert.assertEquals(sampleData.getSampleId(), sampleId);
    }
}
