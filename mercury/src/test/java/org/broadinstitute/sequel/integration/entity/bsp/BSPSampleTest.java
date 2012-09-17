package org.broadinstitute.sequel.integration.entity.bsp;

import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.infrastructure.bsp.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class BSPSampleTest {

    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_integration() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new BSPSampleSearchServiceStub());
        String sampleName = "SM-12CO4";
        BSPStartingSample bspSample = new BSPStartingSample(sampleName,
                null,
                fetcher.fetchSingleSampleFromBSP(sampleName));
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);
    }

}
