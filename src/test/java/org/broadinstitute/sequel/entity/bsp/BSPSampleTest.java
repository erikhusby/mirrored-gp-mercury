package org.broadinstitute.sequel.entity.bsp;

import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.test.ContainerTest;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleTest extends ContainerTest {

    @Inject
    BSPSampleDataFetcher fetcher;

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void test_patient_id_integration() {
        String sampleName = "SM-12CO4";
        BSPSample bspSample = new BSPSample(sampleName,
                null,
                fetcher.fetchSingleSampleFromBSP(sampleName));
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);


    }
}
