package org.broadinstitute.sequel.integration.entity.bsp;

import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;

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
