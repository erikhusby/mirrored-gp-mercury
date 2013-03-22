package org.broadinstitute.gpinformatics.mercury.integration.entity.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.*;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

public class BSPSampleTest {

    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_integration() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new BSPSampleSearchServiceStub());
        String sampleName = "SM-12CO4";
        MercurySample bspSample = new MercurySample(null, sampleName, fetcher.fetchSingleSampleFromBSP(sampleName));
        String patientId = bspSample.getBspSampleDTO().getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);
    }

}
