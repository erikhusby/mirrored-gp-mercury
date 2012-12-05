package org.broadinstitute.gpinformatics.infrastructure.bsp;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.testng.annotations.Test;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;

import static org.testng.Assert.*;

public class BSPSampleDataFetcherTest {


    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = true)
    public void test_sanity() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new BSPSampleSearchServiceImpl(new BSPConfigProducer().produce(Deployment.QA)));
        BSPSampleDTO bspSampleDTO = fetcher.fetchSingleSampleFromBSP("SM-1T7HE");


        assertEquals(bspSampleDTO.getCollaboratorName(),"Herman Taylor");
        assertEquals(bspSampleDTO.getOrganism(),"Homo : Homo sapiens");
        assertEquals(bspSampleDTO.getPrimaryDisease(),"Control");
        assertEquals(bspSampleDTO.getMaterialType(),"DNA:DNA Genomic");

    }
}
