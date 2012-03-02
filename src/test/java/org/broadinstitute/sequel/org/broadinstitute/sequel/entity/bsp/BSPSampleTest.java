package org.broadinstitute.sequel.org.broadinstitute.sequel.entity.bsp;

import org.broadinstitute.sequel.control.bsp.BSPConnectionParametersImpl;
import org.broadinstitute.sequel.control.bsp.BSPSampleSearchServiceImpl;
import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.testng.Assert;
import org.testng.annotations.Test;

public class BSPSampleTest {

    @Test
    public void test_patient_id() {
        BSPSample bspSample = new BSPSample("SM-12CO4",
                null,
                new BSPSampleSearchServiceImpl(new BSPConnectionParametersImpl()));
        String patientId = bspSample.getPatientId();

        Assert.assertNotNull(patientId);
        Assert.assertEquals("PT-2LK3",patientId);
    }
}
