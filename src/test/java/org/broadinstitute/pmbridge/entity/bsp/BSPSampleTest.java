package org.broadinstitute.pmbridge.entity.bsp;

import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.pmbridge.infrastructure.bsp.MockBSPSampleSearchService;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.pmbridge.TestGroups.UNIT;

public class BSPSampleTest {

    @Test(groups = {UNIT})
    public void test_patient_id_integration() {
        BSPSampleDataFetcher fetcher = new BSPSampleDataFetcher(new MockBSPSampleSearchService());
        String sampleName = "SM-12CO4";
        BSPSample bspSample = new BSPSample( new SampleId(sampleName),
                fetcher.fetchSingleSampleFromBSP(sampleName));
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);
    }

}
