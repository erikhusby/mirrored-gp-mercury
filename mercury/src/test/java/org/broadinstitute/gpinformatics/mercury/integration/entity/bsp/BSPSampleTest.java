package org.broadinstitute.gpinformatics.mercury.integration.entity.bsp;

import org.broadinstitute.gpinformatics.infrastructure.bsp.*;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;

@Test(groups = TestGroups.DATABASE_FREE)
public class BSPSampleTest {

    @Test(groups = {DATABASE_FREE})
    public void test_patient_id_integration() {
        String sampleName = BSPSampleSearchServiceStub.SM_12CO4;
        MercurySample bspSample = new MercurySample(sampleName,
                new BSPSampleDTO(BSPSampleSearchServiceStub.getSamples().get(sampleName)));
        String patientId = bspSample.getBspSampleDTO().getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals(BSPSampleSearchServiceStub.SM_12CO4_PATIENT_ID, patientId);
    }

}
